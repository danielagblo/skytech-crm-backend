package com.skytech.crm.entity;

import com.skytech.crm.enums.BroadcastStatus;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "broadcast_messages")
@Getter
@Setter
@NoArgsConstructor
public class BroadcastMessage extends BaseEntity {
  @Column(length = 255)
  private String name;

  @Column(nullable = false, columnDefinition = "text")
  private String messageContent;

  @Column(length = 20)
  private String channel;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private BroadcastStatus status = BroadcastStatus.DRAFT;

  private int recipientCount;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(columnDefinition = "uuid[]")
  private UUID[] contactIds;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> segmentFilter;

  @Column(columnDefinition = "text")
  private String failureDetails;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  private OffsetDateTime scheduledAt, sentAt;
}
