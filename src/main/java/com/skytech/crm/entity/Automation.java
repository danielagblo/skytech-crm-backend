package com.skytech.crm.entity;

import com.skytech.crm.enums.AutomationType;
import jakarta.persistence.*;
import java.util.*;
import java.time.OffsetDateTime;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "automations")
@Getter
@Setter
@NoArgsConstructor
public class Automation extends BaseEntity {
  @Enumerated(EnumType.STRING)
  @Column(length = 50)
  private AutomationType automationType;

  @Column(length = 255)
  private String name;

  private boolean isActive = true;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(columnDefinition = "uuid[]")
  private UUID[] contactIds;

  @Column(length = 30)
  private String executionState = "WAITING";

  private OffsetDateTime nextRunAt;
  private OffsetDateTime lastExecutedAt;

  @Column(columnDefinition = "text")
  private String failureReason;

  private int recipientCount;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> triggerConfig;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private List<Map<String, Object>> steps;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;
}
