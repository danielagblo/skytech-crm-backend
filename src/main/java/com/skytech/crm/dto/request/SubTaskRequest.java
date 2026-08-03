package com.skytech.crm.dto.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.skytech.crm.enums.Priority;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SubTaskRequest {
  @NotBlank
  @Size(max = 255)
  private String title;

  private String description;
  private Priority priority;
  private Boolean complete;
}
