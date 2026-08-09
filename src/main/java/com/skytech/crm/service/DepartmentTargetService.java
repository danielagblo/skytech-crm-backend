package com.skytech.crm.service;

import com.skytech.crm.dto.request.DepartmentTargetRequest;
import static com.skytech.crm.dto.response.DepartmentAchievementResponse.*;
import com.skytech.crm.dto.response.*;
import com.skytech.crm.entity.*;
import com.skytech.crm.enums.*;
import com.skytech.crm.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class DepartmentTargetService {
  private final DepartmentTargetRepository targets;
  private final UserRepository users;
  private final DealRepository deals;
  private final DealLogRepository dealLogs;
  private final CurrentUserService current;

  @Value("${app.time-zone:Africa/Accra}")
  private String timeZone;

  @Transactional(readOnly = true)
  public DepartmentTargetsResponse getConfig(String period) {
    String normalized = normalize(period);
    List<DepartmentTargetsResponse.TargetSetting> settings = new ArrayList<>();
    for (TargetMetric metric : TargetMetric.values()) {
      DepartmentTarget row = find(metric, normalized);
      settings.add(
          new DepartmentTargetsResponse.TargetSetting(
              metric,
              row == null ? BigDecimal.ZERO : row.getTargetValue(),
              row == null ? metric == TargetMetric.CALLS : row.isEnabled()));
    }
    return new DepartmentTargetsResponse(normalized, settings);
  }

  @Transactional
  public DepartmentTargetsResponse save(String period, DepartmentTargetRequest request) {
    String normalized = normalize(period);
    UUID companyId = current.get().getCompanyId();
    for (DepartmentTargetRequest.TargetInput input : request.targets()) {
      if (input.metric() == null) throw new IllegalArgumentException("Metric is required");
      DepartmentTarget row =
          targets
              .findByCompanyIdAndPeriodAndMetricType(companyId, normalized, input.metric())
              .orElseGet(DepartmentTarget::new);
      row.setCompanyId(companyId);
      row.setPeriod(normalized);
      row.setMetricType(input.metric());
      row.setTargetValue(input.target());
      row.setEnabled(input.enabled());
      targets.save(row);
    }
    return getConfig(normalized);
  }

  @Transactional(readOnly = true)
  public DepartmentAchievementResponse achievement(String period) {
    String normalized = normalize(period);
    ZoneId zone = ZoneId.of(timeZone);
    YearMonth month = YearMonth.parse(normalized);
    OffsetDateTime start = month.atDay(1).atStartOfDay().atZone(zone).toOffsetDateTime();
    OffsetDateTime end = month.plusMonths(1).atDay(1).atStartOfDay().atZone(zone).toOffsetDateTime();

    UUID companyId = current.get().getCompanyId();
    Map<TargetMetric, DepartmentTarget> configured = new EnumMap<>(TargetMetric.class);
    for (DepartmentTarget row : targets.findByCompanyIdAndPeriod(companyId, normalized))
      configured.put(row.getMetricType(), row);

    List<User> agents =
        users.findAll().stream()
            .filter(u -> u.getRole() == Role.AGENT && u.isActive())
            .filter(u -> companyId.equals(u.getCompanyId()))
            .toList();

    List<AgentAchievement> rows = new ArrayList<>();
    for (User agent : agents) {
      List<Deal> owned = deals.findByAssignedToId(agent.getId());
      List<Deal> closed =
          owned.stream()
              .filter(d -> d.getStage() == DealStage.CLIENT_RETENTION)
              .filter(d -> d.getCreatedAt() != null)
              .filter(d -> !d.getCreatedAt().isBefore(start) && d.getCreatedAt().isBefore(end))
              .toList();
      List<UUID> dealIds = owned.stream().map(Deal::getId).toList();
      long calls =
          dealIds.isEmpty()
              ? 0L
              : dealLogs.findByDealIdIn(dealIds).stream()
                  .filter(l -> "PHONE_CALL".equals(l.getContactMode()))
                  .filter(l -> l.getCreatedAt() != null)
                  .filter(l -> !l.getCreatedAt().isBefore(start) && l.getCreatedAt().isBefore(end))
                  .count();
      long closedCount = closed.size();
      BigDecimal revenue =
          closed.stream()
              .map(Deal::getTotalPaid)
              .filter(Objects::nonNull)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      Map<TargetMetric, BigDecimal> actuals = new EnumMap<>(TargetMetric.class);
      actuals.put(TargetMetric.CALLS, BigDecimal.valueOf(calls));
      actuals.put(TargetMetric.DEALS_CLOSED, BigDecimal.valueOf(closedCount));
      actuals.put(TargetMetric.REVENUE, revenue);

      List<MetricAchievement> agentMetrics = scored(configured, actuals);
      double agentOverall =
          agentMetrics.stream()
              .mapToDouble(DepartmentAchievementResponse.MetricAchievement::achievementPct)
              .average()
              .orElse(0);
      rows.add(new AgentAchievement(agent.getId(), agent.fullName(), agentOverall, agentMetrics));
    }

    Map<TargetMetric, BigDecimal> totals = new EnumMap<>(TargetMetric.class);
    for (TargetMetric metric : TargetMetric.values()) {
      BigDecimal sum =
          rows.stream()
              .flatMap(r -> r.metrics().stream())
              .filter(m -> m.metric() == metric)
              .map(DepartmentAchievementResponse.MetricAchievement::actual)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      totals.put(metric, sum);
    }

    List<MetricAchievement> departmentMetrics = scored(configured, totals);
    double overallPct =
        departmentMetrics.stream()
            .mapToDouble(DepartmentAchievementResponse.MetricAchievement::achievementPct)
            .average()
            .orElse(0);

    return new DepartmentAchievementResponse(normalized, overallPct, departmentMetrics, rows);
  }

  private List<MetricAchievement> scored(
      Map<TargetMetric, DepartmentTarget> configured, Map<TargetMetric, BigDecimal> actuals) {
    List<MetricAchievement> result = new ArrayList<>();
    for (TargetMetric metric : TargetMetric.values()) {
      DepartmentTarget row = configured.get(metric);
      if (!applicable(row)) continue;
      BigDecimal actual = actuals.getOrDefault(metric, BigDecimal.ZERO);
      result.add(
          new MetricAchievement(metric, row.getTargetValue(), actual, pct(actual, row.getTargetValue())));
    }
    return result;
  }

  private boolean applicable(DepartmentTarget row) {
    return row != null && row.isEnabled() && row.getTargetValue().signum() > 0;
  }

  private double pct(BigDecimal actual, BigDecimal target) {
    if (target.signum() <= 0) return 0;
    return actual
        .multiply(BigDecimal.valueOf(100))
        .divide(target, 2, RoundingMode.HALF_UP)
        .doubleValue();
  }

  private DepartmentTarget find(TargetMetric metric, String period) {
    return targets
        .findByCompanyIdAndPeriodAndMetricType(current.get().getCompanyId(), period, metric)
        .orElse(null);
  }

  private String normalize(String period) {
    if (period == null || period.isBlank())
      throw new IllegalArgumentException("Period is required (YYYY-MM)");
    try {
      return YearMonth.parse(period).toString();
    } catch (DateTimeException e) {
      throw new IllegalArgumentException("Period must be in YYYY-MM format");
    }
  }
}