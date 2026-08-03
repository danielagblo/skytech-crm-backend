package com.skytech.crm.dto.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.skytech.crm.enums.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class DealRequest {
  private UUID leadId;
  private UUID assignedToId;

  @NotBlank
  @Size(max = 255)
  private String title;

  private DealStage stage;
  private Priority priority;

  @PositiveOrZero
  @Digits(integer = 13, fraction = 2)
  private BigDecimal contractValue;

  @PositiveOrZero
  @Digits(integer = 13, fraction = 2)
  private BigDecimal totalPaid;

  private LocalDate hostingExpiry;
  private LocalDate domainExpiry;
  private LocalDate maintenanceExpiry;

  @PositiveOrZero
  @Digits(integer = 13, fraction = 2)
  private BigDecimal hostingCost;

  @PositiveOrZero
  @Digits(integer = 13, fraction = 2)
  private BigDecimal domainCost;

  @PositiveOrZero
  @Digits(integer = 13, fraction = 2)
  private BigDecimal maintenanceCost;

  private String notes;
  private Long version;
}
