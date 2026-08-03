package com.skytech.crm.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "invoice_payments")
@Getter
@Setter
@NoArgsConstructor
public class InvoicePayment extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "invoice_id", nullable = false)
  private Invoice invoice;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "deal_log_id")
  private DealLog dealLog;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recorded_by")
  private User recordedBy;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 50)
  private String paymentMode;

  @Column(length = 100)
  private String reference;

  @Column(nullable = false)
  private OffsetDateTime paidAt;
}
