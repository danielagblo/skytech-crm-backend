package com.skytech.crm.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.skytech.crm.config.ArkeselSmsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SmsServiceTest {
  private static final String URL = "https://sms.arkesel.com/api/v2/sms/send";

  @Test
  void sendsArkeselRequestAndNormalizesGhanaianLocalNumber() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    SmsService service =
        new SmsService(
            new ArkeselSmsConfig(URL, "secret-key", "Skytech", true, "233"),
            "arkesel",
            builder.build());

    server
        .expect(requestTo(URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("api-key", "secret-key"))
        .andExpect(content().string(containsString("\"sender\":\"Skytech\"")))
        .andExpect(content().string(containsString("\"message\":\"Your OTP is 123456\"")))
        .andExpect(content().string(containsString("\"recipients\":[\"233241234567\"]")))
        .andExpect(content().string(containsString("\"sandbox\":true")))
        .andRespond(withSuccess("{\"status\":\"success\"}", MediaType.APPLICATION_JSON));

    service.send("024 123 4567", "Your OTP is 123456");

    server.verify();
  }

  @Test
  void rejectsSenderIdsLongerThanArkeselLimit() {
    SmsService service =
        new SmsService(
            new ArkeselSmsConfig(URL, "secret-key", "Skytech Ghana", false, "233"),
            "arkesel",
            RestClient.create());

    assertThatThrownBy(() -> service.send("+233241234567", "Hello"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("11 characters or fewer");
  }

  @Test
  void rejectsMissingCredentialsBeforeCallingArkesel() {
    SmsService service =
        new SmsService(
            new ArkeselSmsConfig(URL, "", "Skytech", false, "233"),
            "arkesel",
            RestClient.create());

    assertThatThrownBy(() -> service.send("+233241234567", "Hello"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("API key is not configured");
  }

  @Test
  void rejectsProviderFailureResponses() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    SmsService service =
        new SmsService(
            new ArkeselSmsConfig(URL, "secret-key", "Skytech", false, "233"),
            "arkesel",
            builder.build());

    server
        .expect(requestTo(URL))
        .andRespond(
            withSuccess(
                "{\"status\":\"error\",\"message\":\"Invalid sender\"}",
                MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> service.send("+233241234567", "Hello"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Invalid sender");
    server.verify();
  }
}
