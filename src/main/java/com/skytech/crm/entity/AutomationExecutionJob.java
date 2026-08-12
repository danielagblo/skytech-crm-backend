package com.skytech.crm.entity;

import com.skytech.crm.enums.AutomationJobStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "automation_execution_jobs")
@Getter
@Setter
@NoArgsConstructor
public class AutomationExecutionJob extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "automation_id", nullable = false)
  private Automation automation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lead_id")
  private Lead lead;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "deal_id")
  private Deal deal;

  @Column(nullable = false)
  private int stepIndex;

  @Column(nullable = false)
  private OffsetDateTime scheduledAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private AutomationJobStatus status = AutomationJobStatus.PENDING;

  @Column(nullable = false)
  private int attemptCount;

  @Column(columnDefinition = "text")
  private String lastError;
}
