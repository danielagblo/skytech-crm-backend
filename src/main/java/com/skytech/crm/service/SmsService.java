package com.skytech.crm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.skytech.crm.config.ArkeselSmsConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class SmsService {
  private final ArkeselSmsConfig config;
  private final String provider;
  private final RestClient client;

  @Autowired
  public SmsService(
      ArkeselSmsConfig config,
      @Value("${communication.sms-provider:arkesel}") String provider) {
    this(config, provider, client());
  }

  SmsService(ArkeselSmsConfig config, String provider, RestClient client) {
    this.config = config;
    this.provider = provider;
    this.client = client;
  }

  public void send(String phone, String body) {
    validateConfiguration();
    if (body == null || body.isBlank()) throw new IllegalArgumentException("SMS message is required");
    String recipient = normalize(phone);
    JsonNode response =
        client
            .post()
            .uri(URI.create(config.apiUrl()))
            .header("api-key", config.apiKey())
            .body(new SendRequest(config.senderId(), body, List.of(recipient), config.sandbox()))
            .retrieve()
            .body(JsonNode.class);
    if (response == null || !"success".equalsIgnoreCase(response.path("status").asText())) {
      String providerMessage = response == null ? "empty response" : response.path("message").asText();
      throw new IllegalStateException(
          "Arkesel rejected the SMS request"
              + (providerMessage.isBlank() ? "" : ": " + providerMessage));
    }
    log.info("Arkesel accepted SMS for recipient ending {}", suffix(recipient));
  }

  private void validateConfiguration() {
    if (!"arkesel".equalsIgnoreCase(provider))
      throw new IllegalStateException("Unsupported SMS provider: " + provider);
    if (config.apiUrl() == null || config.apiUrl().isBlank())
      throw new IllegalStateException("Arkesel SMS API URL is not configured");
    if (config.apiKey() == null || config.apiKey().isBlank())
      throw new IllegalStateException("Arkesel SMS API key is not configured");
    if (config.senderId() == null || config.senderId().isBlank())
      throw new IllegalStateException("Arkesel SMS sender ID is not configured");
    if (config.senderId().length() > 11)
      throw new IllegalStateException("Arkesel SMS sender ID must be 11 characters or fewer");
  }

  private String normalize(String phone) {
    if (phone == null || phone.isBlank()) throw new IllegalArgumentException("Phone number is required");
    String number = phone.trim().replaceAll("[^0-9+]", "");
    if (number.startsWith("+")) number = number.substring(1);
    if (number.startsWith("00")) number = number.substring(2);
    if (number.startsWith("0")) {
      String country = config.defaultCountryCode();
      if (country == null || !country.matches("[1-9][0-9]{0,3}"))
        throw new IllegalStateException("Arkesel default country code is invalid");
      number = country + number.substring(1);
    }
    if (!number.matches("[1-9][0-9]{7,14}"))
      throw new IllegalArgumentException("Phone number must use international format");
    return number;
  }

  private String suffix(String recipient) {
    return recipient.substring(Math.max(0, recipient.length() - 4));
  }

  private static RestClient client() {
    HttpClient httpClient =
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofSeconds(10));
    return RestClient.builder().requestFactory(requestFactory).build();
  }

  private record SendRequest(
      String sender, String message, List<String> recipients, boolean sandbox) {}
}
