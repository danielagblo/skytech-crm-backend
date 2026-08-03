package com.skytech.crm.entity;

import com.skytech.crm.enums.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.*;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
public class Task extends BaseEntity {
  @Column(nullable = false, length = 255)
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(length = 50)
  private TaskStatus status = TaskStatus.TODO;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private Priority priority;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  private boolean allowReminder = true;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "linked_lead_id")
  private Lead linkedLead;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "linked_deal_id")
  private Deal linkedDeal;

  private OffsetDateTime dueDate;

  @ManyToMany
  @JoinTable(
      name = "task_assignees",
      joinColumns = @JoinColumn(name = "task_id"),
      inverseJoinColumns = @JoinColumn(name = "user_id"))
  private Set<User> assignees = new HashSet<>();

  @Version private Long version;
}
