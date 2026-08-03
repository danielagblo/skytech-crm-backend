package com.skytech.crm.entity;

import com.skytech.crm.enums.*;
import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "deals")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class Deal extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lead_id")
  private Lead lead;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_to")
  private User assignedTo;

  @Column(nullable = false, length = 255)
  private String title;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private DealStage stage = DealStage.PROSPECTING;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private Priority priority;

  @Column(precision = 15, scale = 2)
  private BigDecimal contractValue;

  @Column(precision = 15, scale = 2)
  private BigDecimal totalPaid = BigDecimal.ZERO;

  @Column(precision = 15, scale = 2)
  private BigDecimal arrears = BigDecimal.ZERO;

  private boolean isPaidInFull;
  private LocalDate hostingExpiry, domainExpiry, maintenanceExpiry;

  @Column(precision = 15, scale = 2)
  private BigDecimal hostingCost;

  @Column(precision = 15, scale = 2)
  private BigDecimal domainCost;

  @Column(precision = 15, scale = 2)
  private BigDecimal maintenanceCost;

  @Column(columnDefinition = "text")
  private String notes;

  @Version private Long version;
  private OffsetDateTime deletedAt;
}
