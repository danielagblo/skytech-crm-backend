package com.skytech.crm.dto.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class DealLogRequest {
  @NotBlank
  @Pattern(regexp = "NEGOTIATION|SETTLEMENT|PAYMENT|CLIENT_RETENTION")
  private String logType;

  @Pattern(regexp = "PHONE_CALL|EMAIL|IN_PERSON|WHATSAPP")
  private String contactMode;

  @Pattern(regexp = "POSITIVE|NEGATIVE|NEUTRAL|NO_RESPONSE")
  private String responseType;

  @Pattern(regexp = "OUTGOING|INCOMING")
  private String callDirection;

  @PositiveOrZero private Integer callDurationSeconds;

  @Pattern(regexp = "COMPLETED|NETWORK_INTERRUPTION|CUSTOMER_HUNG_UP|NO_RESPONSE")
  private String callOutcome;

  private OffsetDateTime followUpAt;

  @PositiveOrZero
  @Digits(integer = 13, fraction = 2)
  private BigDecimal settlementValue;

  private OffsetDateTime settlementFollowUp;
  private String specialConditions;

  @PositiveOrZero
  @Digits(integer = 13, fraction = 2)
  private BigDecimal amountPaid;

  @Pattern(regexp = "MOMO|BANK_TRANSFER|CASH|CHEQUE")
  private String paymentMode;

  @Size(max = 100)
  private String invoiceNumber;

  @Size(max = 100)
  private String receiptNumber;

  private Boolean invoiceIssued;

  @Pattern(regexp = "HOSTING|DOMAIN|MAINTENANCE")
  private String serviceType;

  private LocalDate expiryDate;

  @PositiveOrZero
  @Digits(integer = 13, fraction = 2)
  private BigDecimal retentionAmount;

  @Size(max = 100)
  private String retentionInvoice;

  @Size(max = 100)
  private String retentionReceipt;

  private String body;
}
