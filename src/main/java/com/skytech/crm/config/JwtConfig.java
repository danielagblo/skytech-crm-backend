package com.skytech.crm.config;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtConfig(
    @NotBlank @Size(min = 32) String secret,
    @Positive long accessTokenExpiryMs,
    @Min(604800000) @Max(604800000) long refreshTokenExpiryMs) {}
