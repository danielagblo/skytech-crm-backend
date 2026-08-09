package com.skytech.crm.dto.response;

import com.skytech.crm.enums.TargetMetric;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DepartmentAchievementResponse(
    String period,
    double overallPct,
    List<MetricAchievement> metrics,
    List<AgentAchievement> agents) {

  public record MetricAchievement(
      TargetMetric metric, BigDecimal target, BigDecimal actual, double achievementPct) {}

  public record AgentAchievement(
      UUID userId, String name, double overallPct, List<MetricAchievement> metrics) {}
}