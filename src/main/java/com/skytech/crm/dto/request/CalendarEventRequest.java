package com.skytech.crm.dto.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CalendarEventRequest {
  @NotBlank
  @Size(max = 255)
  private String title;

  private String description;
  private UUID linkedLeadId;
  private UUID linkedDealId;
  @NotNull private OffsetDateTime startTime;
  @NotNull private OffsetDateTime endTime;

  @NotBlank
  @Pattern(regexp = "CALL_LOG_FOLLOWUP|PAYMENT_DUE|MEETING|REMINDER")
  private String eventType;

  private UUID[] assignees;
}
