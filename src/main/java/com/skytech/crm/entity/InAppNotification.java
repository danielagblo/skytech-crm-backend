package com.skytech.crm.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "in_app_notifications")
@Getter
@Setter
@NoArgsConstructor
public class InAppNotification extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, length = 50)
  private String type;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(nullable = false, columnDefinition = "text")
  private String body;

  @Column(columnDefinition = "text")
  private String href;

  @Column(length = 255)
  private String deduplicationKey;

  private OffsetDateTime readAt;
}
