package com.skytech.crm.dto.response;

import com.skytech.crm.enums.TargetMetric;
import java.math.BigDecimal;
import java.util.List;

public record DepartmentTargetsResponse(String period, List<TargetSetting> targets) {
  public record TargetSetting(TargetMetric metric, BigDecimal target, boolean enabled) {}
}