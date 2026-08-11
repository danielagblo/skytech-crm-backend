package com.skytech.crm.dto.response;

import com.skytech.crm.enums.InvoiceStatus;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record InvoiceResponse(
    UUID id,
    UUID companyId,
    UUID dealId,
    UUID createdById,
    String invoiceNumber,
    InvoiceStatus status,
    LocalDate issueDate,
    LocalDate dueDate,
    String currency,
    String recipientName,
    String recipientCompany,
    String recipientEmail,
    String recipientAddress,
    String issuerName,
    String issuerEmail,
    String issuerPhone,
    String issuerAddress,
    String issuerTaxId,
    String paymentInstructions,
    BigDecimal subtotal,
    BigDecimal taxRate,
    BigDecimal taxAmount,
    BigDecimal discountAmount,
    BigDecimal total,
    BigDecimal amountPaid,
    BigDecimal balanceDue,
    String notes,
    String terms,
    String lastSendError,
    OffsetDateTime issuedAt,
    OffsetDateTime sendRequestedAt,
    OffsetDateTime sentAt,
    OffsetDateTime paidAt,
    OffsetDateTime voidedAt,
    boolean receptionConfirmed,
    OffsetDateTime receptionConfirmedAt,
    UUID receptionConfirmedById,
    List<Item> items,
    List<Payment> payments,
    Long version,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {
  public record Item(
      UUID id,
      String description,
      BigDecimal quantity,
      BigDecimal unitPrice,
      BigDecimal amount,
      int position,
      List<String> subLines) {}

  public record Payment(
      UUID id,
      UUID dealLogId,
      UUID recordedById,
      BigDecimal amount,
      String paymentMode,
      String reference,
      OffsetDateTime paidAt) {}
}
