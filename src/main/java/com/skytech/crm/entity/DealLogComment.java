package com.skytech.crm.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "deal_log_comments")
@Getter
@Setter
@NoArgsConstructor
public class DealLogComment extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "deal_log_id")
  private DealLog dealLog;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_comment_id")
  private DealLogComment parentComment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "author_id")
  private User author;

  @Column(nullable = false, columnDefinition = "text")
  private String body;
}
