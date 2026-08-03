package com.skytech.crm.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "task_comments")
@Getter
@Setter
@NoArgsConstructor
public class TaskComment extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "task_id")
  private Task task;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_comment_id")
  private TaskComment parentComment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "author_id")
  private User author;

  @Column(nullable = false, columnDefinition = "text")
  private String body;
}
