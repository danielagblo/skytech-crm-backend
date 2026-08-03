package com.skytech.crm.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "calendar_events")
@Getter
@Setter
@NoArgsConstructor
public class CalendarEvent extends BaseEntity {
  @Column(length = 255)
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id")
  private User owner;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "linked_lead_id")
  private Lead linkedLead;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "linked_deal_id")
  private Deal linkedDeal;

  private OffsetDateTime startTime, endTime;

  @Column(length = 50)
  private String eventType;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(columnDefinition = "uuid[]")
  private UUID[] assignees;
}
