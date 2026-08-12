package com.skytech.crm.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "user_sessions")
@Getter
@Setter
@NoArgsConstructor
public class UserSession extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private OffsetDateTime startedAt;

  @Column(nullable = false)
  private OffsetDateTime lastActivityAt;

  private OffsetDateTime endedAt;
}
