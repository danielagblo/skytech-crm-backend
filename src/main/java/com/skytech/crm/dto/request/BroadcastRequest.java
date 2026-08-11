package com.skytech.crm.dto.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BroadcastRequest {
  @NotBlank
  @Size(max = 255)
  private String name;

  @NotBlank private String messageContent;

  @NotBlank
  @Pattern(regexp = "SMS|EMAIL")
  private String channel;

  private Map<String, Object> segmentFilter;
  private UUID[] contactIds;
  @Future private OffsetDateTime scheduledAt;
}
