package com.skytech.crm.dto.response;

import java.math.*;
import java.time.*;
import java.util.*;

public record DealLogResponse(
    UUID id,
    UUID dealId,
    UUID createdById,
    String logType,
    String contactMode,
    String responseType,
    String callDirection,
    Integer callDurationSeconds,
    String callOutcome,
    OffsetDateTime followUpAt,
    BigDecimal settlementValue,
    OffsetDateTime settlementFollowUp,
    String specialConditions,
    BigDecimal amountPaid,
    String paymentMode,
    String invoiceNumber,
    String receiptNumber,
    Boolean invoiceIssued,
    String serviceType,
    LocalDate expiryDate,
    BigDecimal retentionAmount,
    String retentionInvoice,
    String retentionReceipt,
    Integer autoReviewScore,
    String body,
    OffsetDateTime createdAt) {}
