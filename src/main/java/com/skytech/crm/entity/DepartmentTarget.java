package com.skytech.crm.entity;

import com.skytech.crm.enums.TargetMetric;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(
    name = "department_targets",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"company_id", "period", "metric_type"}))
@Getter
@Setter
@NoArgsConstructor
public class DepartmentTarget extends BaseEntity {
  @Column(nullable = false, length = 7)
  private String period;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TargetMetric metricType;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal targetValue = BigDecimal.ZERO;

  @Column(nullable = false)
  private boolean enabled;
}