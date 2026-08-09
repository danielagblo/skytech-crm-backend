package com.skytech.crm.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;
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

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "invoice_item_sublines",
      joinColumns = @JoinColumn(name = "invoice_item_id"))
  @OrderColumn(name = "position")
  @Column(name = "label", nullable = false, length = 500)
  private List<String> subLines = new ArrayList<>();
}
