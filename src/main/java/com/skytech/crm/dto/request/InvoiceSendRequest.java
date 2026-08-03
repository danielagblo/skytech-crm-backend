package com.skytech.crm.dto.request;

import jakarta.validation.constraints.*;

public record InvoiceSendRequest(
    @Email @Size(max = 255) String email,
    @Size(max = 255) String subject,
    @Size(max = 5000) String message) {}
