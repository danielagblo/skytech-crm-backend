package com.skytech.crm.entity;

import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "deal_logs")
@Getter
@Setter
@NoArgsConstructor
public class DealLog extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "deal_id", nullable = false)
  private Deal deal;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @Column(length = 50)
  private String logType;

  @Column(length = 50)
  private String contactMode;

  @Column(length = 50)
  private String responseType;

  @Column(length = 20)
  private String callDirection;

  @Column(length = 50)
  private String callOutcome;

  private Integer callDurationSeconds;
  private OffsetDateTime followUpAt;

  @Column(precision = 15, scale = 2)
  private BigDecimal settlementValue;

  private OffsetDateTime settlementFollowUp;

  @Column(columnDefinition = "text")
  private String specialConditions;

  @Column(precision = 15, scale = 2)
  private BigDecimal amountPaid;

  @Column(length = 50)
  private String paymentMode;

  @Column(length = 100)
  private String invoiceNumber;

  @Column(length = 100)
  private String receiptNumber;

  private Boolean invoiceIssued;

  @Column(length = 50)
  private String serviceType;

  private LocalDate expiryDate;

  @Column(precision = 15, scale = 2)
  private BigDecimal retentionAmount;

  @Column(length = 100)
  private String retentionInvoice;

  @Column(length = 100)
  private String retentionReceipt;

  private Integer autoReviewScore;

  @Column(columnDefinition = "text")
  private String body;
}
