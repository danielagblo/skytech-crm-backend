package com.skytech.crm.entity;

import com.skytech.crm.enums.Priority;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sub_tasks")
@Getter
@Setter
@NoArgsConstructor
public class SubTask extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "task_id")
  private Task task;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private Priority priority;

  private boolean isComplete;
}
