package com.skytech.crm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "arkesel.sms")
public record ArkeselSmsConfig(
    String apiUrl, String apiKey, String senderId, boolean sandbox, String defaultCountryCode) {}
