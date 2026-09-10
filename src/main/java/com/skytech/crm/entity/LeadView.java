package com.skytech.crm.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.*;

@Entity
@Table(name = "lead_views", uniqueConstraints = {@UniqueConstraint(columnNames = {"lead_id", "user_id"})})
@Getter
@Setter
@NoArgsConstructor
public class LeadView extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lead_id", nullable = false)
  private Lead lead;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  private OffsetDateTime seenAt;
}
