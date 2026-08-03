package com.skytech.crm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public record AuthenticationConfig(boolean otpEnabled) {}
