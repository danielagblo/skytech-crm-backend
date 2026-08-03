package com.skytech.crm.dto.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.skytech.crm.enums.AutomationType;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AutomationRequest {
  @NotNull private AutomationType automationType;

  @NotBlank
  @Size(max = 255)
  private String name;

  private Boolean active;
  private Map<String, Object> triggerConfig;
  private List<Map<String, Object>> steps;
}
