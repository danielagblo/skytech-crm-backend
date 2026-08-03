package com.skytech.crm.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record InvoicePaymentRequest(
    @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal amount,
    @NotBlank @Pattern(regexp = "MOMO|BANK_TRANSFER|CASH|CHEQUE") String paymentMode,
    @Size(max = 100) String reference,
    @PastOrPresent OffsetDateTime paidAt) {}
