package com.skytech.crm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "invoice")
public record InvoiceConfig(
    String issuerName,
    String issuerEmail,
    String issuerPhone,
    String issuerAddress,
    String issuerTaxId,
    String paymentInstructions) {}
