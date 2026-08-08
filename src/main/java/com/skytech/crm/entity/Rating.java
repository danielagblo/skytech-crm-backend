package com.skytech.crm.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "ratings")
@Getter
@Setter
@NoArgsConstructor
public class Rating extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "agent_id")
  private User agent;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "deal_id")
  private Deal deal;

  @Column(length = 50)
  private String contactChannel;

  @Column(length = 255)
  private String clientEmail;

  @Column(length = 255)
  private String clientName;

  @Column(length = 100, unique = true)
  private String token;

  private Integer rating;

  @Column(columnDefinition = "text")
  private String feedback;

  private Boolean rated = false;

  private OffsetDateTime emailSentAt;

  private OffsetDateTime ratedAt;
}
