package com.skytech.crm.entity;

import com.skytech.crm.enums.ActivityType;
import jakarta.persistence.*;
import java.util.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "activities")
@Getter
@Setter
@NoArgsConstructor
public class Activity extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actor_id")
  private User actor;

  @Enumerated(EnumType.STRING)
  @Column(length = 100)
  private ActivityType eventType;

  @Column(length = 50)
  private String entityType;

  private UUID entityId;

  @Column(columnDefinition = "text")
  private String description;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> metadata;
}
