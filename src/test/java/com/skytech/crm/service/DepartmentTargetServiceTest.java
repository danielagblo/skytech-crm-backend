package com.skytech.crm.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.skytech.crm.dto.response.DepartmentAchievementResponse;
import com.skytech.crm.entity.*;
import com.skytech.crm.enums.*;
import com.skytech.crm.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

class DepartmentTargetServiceTest {
  private final UUID companyId = UUID.randomUUID();

  private DepartmentTargetService service;
  private DepartmentTargetRepository targets;
  private UserRepository users;
  private DealRepository deals;
  private DealLogRepository dealLogs;

  @BeforeEach
  void setUp() {
    targets = mock(DepartmentTargetRepository.class);
    users = mock(UserRepository.class);
    deals = mock(DealRepository.class);
    dealLogs = mock(DealLogRepository.class);
    CurrentUserService current = mock(CurrentUserService.class);
    InAppNotificationService notifications = mock(InAppNotificationService.class);

    User manager = new User();
    manager.setCompanyId(companyId);
    when(current.get()).thenReturn(manager);

    service = new DepartmentTargetService(targets, users, deals, dealLogs, current, notifications);
    ReflectionTestUtils.setField(service, "timeZone", "UTC");
  }

  @Test
  void achievementAggregatesCallsDealsAndRevenuePerAgent() {
    User ada = agent("Ada");
    User ben = agent("Ben");
    when(users.findAll()).thenReturn(List.of(ada, ben));

    Deal dealA = closedDeal(ada, "2026-08-15", "1000");
    Deal dealB = closedDeal(ben, "2026-08-20", "5000");

    List<DealLog> logs =
        List.of(
            call(dealA, "2026-08-02"),
            call(dealA, "2026-08-21"),
            call(dealA, "2026-10-01"),
            call(dealB, "2026-08-05"));

    when(targets.findByCompanyIdAndPeriod(companyId, "2026-08"))
        .thenReturn(
            List.of(
                target(TargetMetric.CALLS, "100", true),
                target(TargetMetric.DEALS_CLOSED, "3", true),
                target(TargetMetric.REVENUE, "2000", true)));
    when(deals.findByAssignedToId(ada.getId())).thenReturn(List.of(dealA));
    when(deals.findByAssignedToId(ben.getId())).thenReturn(List.of(dealB));
    when(dealLogs.findByDealIdIn(anyCollection()))
        .thenAnswer(
            invocation -> {
              Collection<UUID> ids = invocation.getArgument(0);
              return logs.stream().filter(l -> ids.contains(l.getDeal().getId())).toList();
            });

    DepartmentAchievementResponse result = service.achievement("2026-08");

    assertEquals("2026-08", result.period());
    assertEquals(2, result.agents().size());
    assertEquals(3, result.metrics().size());

    // Department level: calls 3/100, deals 2/3, revenue 6000/2000 -> avg
    assertEquals(3.0, pct(result, "CALLS"), 0.01);
    assertEquals(66.67, pct(result, "DEALS_CLOSED"), 0.01);
    assertEquals(300.0, pct(result, "REVENUE"), 0.01);
    assertEquals(123.22, result.overallPct(), 0.01);

    DepartmentAchievementResponse.AgentAchievement adaRow =
        result.agents().stream()
            .filter(a -> a.name().equals(ada.fullName()))
            .findFirst()
            .orElseThrow();
    assertEquals(2.0, pct(adaRow, "CALLS"), 0.01);
    assertEquals(33.33, pct(adaRow, "DEALS_CLOSED"), 0.01);
    assertEquals(50.0, pct(adaRow, "REVENUE"), 0.01);
    assertEquals(28.44, adaRow.overallPct(), 0.01);

    DepartmentAchievementResponse.AgentAchievement benRow =
        result.agents().stream()
            .filter(a -> a.name().equals(ben.fullName()))
            .findFirst()
            .orElseThrow();
    assertEquals(1.0, pct(benRow, "CALLS"), 0.01);
    assertEquals(94.78, benRow.overallPct(), 0.01);
  }

  @Test
  void disabledMetricsAreExcludedFromAchievement() {
    User ada = agent("Ada");
    when(users.findAll()).thenReturn(List.of(ada));
    Deal dealA = closedDeal(ada, "2026-08-15", "1000");

    when(targets.findByCompanyIdAndPeriod(companyId, "2026-08"))
        .thenReturn(
            List.of(
                target(TargetMetric.CALLS, "100", false),
                target(TargetMetric.DEALS_CLOSED, "3", true)));
    when(deals.findByAssignedToId(ada.getId())).thenReturn(List.of(dealA));
    when(dealLogs.findByDealIdIn(anyCollection())).thenReturn(List.of());

    DepartmentAchievementResponse result = service.achievement("2026-08");

    assertEquals(1, result.metrics().size());
    assertEquals("DEALS_CLOSED", result.metrics().get(0).metric().name());
    assertEquals(33.33, result.overallPct(), 0.01);
  }

  private double pct(DepartmentAchievementResponse result, String metric) {
    return pct(result.metrics(), metric);
  }

  private double pct(DepartmentAchievementResponse.AgentAchievement agent, String metric) {
    return pct(agent.metrics(), metric);
  }

  private double pct(
      List<DepartmentAchievementResponse.MetricAchievement> metrics, String metric) {
    return metrics.stream()
        .filter(m -> m.metric().name().equals(metric))
        .findFirst()
        .orElseThrow()
        .achievementPct();
  }

  private User agent(String name) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setFirstName(name);
    user.setLastName("");
    user.setRole(Role.AGENT);
    user.setActive(true);
    user.setCompanyId(companyId);
    return user;
  }

  private Deal closedDeal(User agent, String created, String paid) {
    Deal deal = new Deal();
    deal.setId(UUID.randomUUID());
    deal.setAssignedTo(agent);
    deal.setStage(DealStage.CLIENT_RETENTION);
    deal.setCreatedAt(OffsetDateTime.parse(created + "T10:00:00Z"));
    deal.setTotalPaid(new BigDecimal(paid));
    return deal;
  }

  private DealLog call(Deal deal, String created) {
    DealLog log = new DealLog();
    log.setDeal(deal);
    log.setContactMode("PHONE_CALL");
    log.setCallDirection("OUTGOING");
    log.setCreatedAt(OffsetDateTime.parse(created + "T09:00:00Z"));
    return log;
  }

  private DepartmentTarget target(TargetMetric metric, String value, boolean enabled) {
    DepartmentTarget tg = new DepartmentTarget();
    tg.setCompanyId(companyId);
    tg.setPeriod("2026-08");
    tg.setMetricType(metric);
    tg.setTargetValue(new BigDecimal(value));
    tg.setEnabled(enabled);
    return tg;
  }
}
