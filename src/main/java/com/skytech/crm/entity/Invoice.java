package com.skytech.crm.entity;

import com.skytech.crm.enums.InvoiceStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.*;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
public class Invoice extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "deal_id", nullable = false)
  private Deal deal;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @Column(length = 50, unique = true)
  private String invoiceNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private InvoiceStatus status = InvoiceStatus.DRAFT;

  private LocalDate issueDate;
  private LocalDate dueDate;

  @Column(nullable = false, length = 3)
  private String currency = "GHS";

  @Column(nullable = false, length = 255)
  private String recipientName;

  @Column(length = 255)
  private String recipientCompany;

  @Column(length = 255)
  private String recipientEmail;

  @Column(columnDefinition = "text")
  private String recipientAddress;

  @Column(nullable = false, length = 255)
  private String issuerName = "Skytech";

  @Column(length = 255)
  private String issuerEmail;

  @Column(length = 30)
  private String issuerPhone;

  @Column(columnDefinition = "text")
  private String issuerAddress;

  @Column(length = 100)
  private String issuerTaxId;

  @Column(columnDefinition = "text")
  private String paymentInstructions;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal subtotal = BigDecimal.ZERO;

  @Column(nullable = false, precision = 7, scale = 4)
  private BigDecimal taxRate = BigDecimal.ZERO;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal taxAmount = BigDecimal.ZERO;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal discountAmount = BigDecimal.ZERO;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal total = BigDecimal.ZERO;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal amountPaid = BigDecimal.ZERO;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal balanceDue = BigDecimal.ZERO;

  @Column(columnDefinition = "text")
  private String notes;

  @Column(columnDefinition = "text")
  private String terms;

  @Column(columnDefinition = "text")
  private String lastSendError;

  private OffsetDateTime issuedAt;
  private OffsetDateTime sendRequestedAt;
  private OffsetDateTime sentAt;
  private OffsetDateTime paidAt;
  private OffsetDateTime voidedAt;

  @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("position ASC")
  private List<InvoiceItem> items = new ArrayList<>();

  @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
  @OrderBy("paidAt ASC")
  private List<InvoicePayment> payments = new ArrayList<>();

  @Version private Long version;
}
