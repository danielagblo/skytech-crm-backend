package com.skytech.crm.dto.request;

import com.skytech.crm.enums.TargetMetric;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record DepartmentTargetRequest(@Valid @NotEmpty List<TargetInput> targets) {
  public record TargetInput(
      @NotNull TargetMetric metric,
      @NotNull @DecimalMin("0") BigDecimal target,
      boolean enabled) {}
}