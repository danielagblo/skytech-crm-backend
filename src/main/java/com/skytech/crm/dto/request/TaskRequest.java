package com.skytech.crm.dto.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.skytech.crm.enums.*;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class TaskRequest {
  @NotBlank
  @Size(max = 255)
  private String title;

  private String description;
  private TaskStatus status;
  private Priority priority;
  private Boolean allowReminder;
  private UUID linkedLeadId;
  private UUID linkedDealId;
  private OffsetDateTime dueDate;
  private Set<UUID> assigneeIds;
  private Long version;
}
