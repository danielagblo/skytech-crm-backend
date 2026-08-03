package com.skytech.crm.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceItem extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "invoice_id", nullable = false)
  private Invoice invoice;

  @Column(nullable = false, length = 500)
  private String description;

  @Column(nullable = false, precision = 15, scale = 4)
  private BigDecimal quantity;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal unitPrice;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false)
  private int position;
}
