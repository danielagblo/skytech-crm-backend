package com.skytech.crm.service;

import com.skytech.crm.dto.response.*;
import com.skytech.crm.entity.*;
import com.skytech.crm.enums.*;
import com.skytech.crm.exception.*;
import com.skytech.crm.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {
  private final DealRepository deals;
  private final DealLogRepository logs;
  private final TaskRepository tasks;
  private final UserRepository users;
  private final CurrentUserService current;
  private final FeatureGateService gates;

  @Transactional(readOnly = true)
    public DashboardOverviewResponse overview(String period) {
    User me = current.get();
        OffsetDateTime start = periodStart(period);
        List<Deal> visibleDeals = visibleDeals(me).stream().filter(deal -> dealRecent(deal, start)).toList();
    Set<UUID> visibleDealIds =
        visibleDeals.stream().map(Deal::getId).collect(java.util.stream.Collectors.toSet());
    List<DealLog> visibleLogs =
                visibleDealIds.isEmpty()
                        ? List.of()
                        : logs.findByDealIdIn(visibleDealIds).stream()
                                .filter(log -> logRecent(log, start))
                                .toList();

    List<User> visibleUsers = me.getRole() == Role.AGENT ? List.of(me) : users.findAll();
    List<DashboardOverviewResponse.AgentRevenue> revenue =
        visibleUsers.stream()
            .filter(user -> user.isActive() && user.getRole() == Role.AGENT)
            .map(
                user ->
                    new DashboardOverviewResponse.AgentRevenue(
                        user.getId(), user.fullName(), revenue(user.getId())))
            .sorted(
                Comparator.comparing(DashboardOverviewResponse.AgentRevenue::revenue).reversed())
            .limit(10)
            .toList();

    List<DashboardOverviewResponse.ExecutivePerformance> performance =
        visibleUsers.stream()
            .filter(user -> user.isActive() && user.getRole() == Role.AGENT)
            .map(
                user -> {
                  List<Deal> assigned = deals.findByAssignedToId(user.getId());
                  long closed =
                      assigned.stream()
                          .filter(d -> d.getStage() == DealStage.CLIENT_RETENTION)
                          .count();
                  double conversion =
                      assigned.isEmpty()
                          ? 0
                          : Math.round(closed * 10000.0 / assigned.size()) / 100.0;
                  Set<UUID> ids =
                      assigned.stream()
                          .map(Deal::getId)
                          .collect(java.util.stream.Collectors.toSet());
                  double rating =
                      ids.isEmpty()
                          ? 0
                          : logs.findByDealIdIn(ids).stream()
                              .filter(log -> log.getAutoReviewScore() != null)
                              .mapToInt(DealLog::getAutoReviewScore)
                              .average()
                              .orElse(0);
                  return new DashboardOverviewResponse.ExecutivePerformance(
                      user.getId(),
                      user.fullName(),
                      closed,
                      revenue(user.getId()),
                      conversion,
                      Math.round(rating * 100.0) / 100.0);
                })
            .sorted(
                Comparator.comparing(DashboardOverviewResponse.ExecutivePerformance::revenue)
                    .reversed())
            .toList();

    OffsetDateTime now = OffsetDateTime.now();
    List<DashboardOverviewResponse.FollowUpReminder> reminders =
        visibleLogs.stream()
            .flatMap(
                log ->
                    java.util.stream.Stream.of(
                        reminder(log, log.getFollowUpAt(), "NEGOTIATION"),
                        reminder(log, log.getSettlementFollowUp(), "SETTLEMENT")))
            .filter(Objects::nonNull)
            .filter(reminder -> !reminder.followUpAt().isBefore(now))
            .sorted(Comparator.comparing(DashboardOverviewResponse.FollowUpReminder::followUpAt))
            .limit(10)
            .toList();

    List<DashboardOverviewResponse.RecentPayment> payments =
        visibleLogs.stream()
            .filter(log -> log.getAmountPaid() != null && log.getAmountPaid().signum() > 0)
            .sorted(
                Comparator.comparing(
                    DealLog::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(10)
            .map(
                log ->
                    new DashboardOverviewResponse.RecentPayment(
                        log.getDeal().getId(),
                        log.getDeal().getTitle(),
                        log.getAmountPaid(),
                        log.getCreatedAt()))
            .toList();

    DashboardOverviewResponse.CallStats outgoing =
        callStats(
            visibleLogs.stream()
                .filter(
                    log ->
                        "PHONE_CALL".equals(log.getContactMode())
                            && "OUTGOING".equals(log.getCallDirection()))
                .toList());
    DashboardOverviewResponse.CallStats incoming =
        callStats(
            visibleLogs.stream()
                .filter(
                    log ->
                        "PHONE_CALL".equals(log.getContactMode())
                            && "INCOMING".equals(log.getCallDirection()))
                .toList());
    BigDecimal ownRevenue =
        visibleDeals.stream().map(this::paid).reduce(BigDecimal.ZERO, BigDecimal::add);
    long screenTimeHours =
        visibleLogs.stream()
                .map(DealLog::getCallDurationSeconds)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum()
            / 3600;
    BigDecimal contractTotal =
        visibleDeals.stream()
            .map(deal -> Optional.ofNullable(deal.getContractValue()).orElse(BigDecimal.ZERO))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    double targetAchievement =
        contractTotal.signum() == 0
            ? 0
            : ownRevenue
                .multiply(BigDecimal.valueOf(100))
                .divide(contractTotal, 2, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    DashboardOverviewResponse.AgentRank rank =
        new DashboardOverviewResponse.AgentRank(
            rankFor(me),
            users.findAll().stream().filter(u -> u.getRole() == Role.AGENT).count(),
            screenTimeHours,
            targetAchievement,
            ownRevenue);

    return new DashboardOverviewResponse(
        outgoing, incoming, revenue, performance, reminders, payments, rank);
  }

    private OffsetDateTime periodStart(String period) {
        OffsetDateTime now = OffsetDateTime.now();
        return switch (period == null ? "today" : period) {
            case "today" -> LocalDateTime.of(now.toLocalDate(), java.time.LocalTime.MIDNIGHT).atOffset(now.getOffset());
            case "this_week" ->
                    LocalDateTime.of(
                                    now.toLocalDate().minusDays(Math.max(now.getDayOfWeek().getValue() - 1, 0)),
                                    java.time.LocalTime.MIDNIGHT)
                            .atOffset(now.getOffset());
            case "this_month" ->
                    LocalDateTime.of(now.toLocalDate().withDayOfMonth(1), java.time.LocalTime.MIDNIGHT)
                            .atOffset(now.getOffset());
            case "three_months" -> now.minusMonths(3);
            default -> throw new IllegalArgumentException("period must be today, this_week, this_month, or three_months");
        };
    }

    private boolean dealRecent(Deal deal, OffsetDateTime start) {
        return deal.getCreatedAt() == null || !deal.getCreatedAt().isBefore(start);
    }

    private boolean logRecent(DealLog log, OffsetDateTime start) {
        return log.getCreatedAt() == null || !log.getCreatedAt().isBefore(start);
    }

  @Transactional(readOnly = true)
  public Page<TopDealResponse> topDeals(String period, Pageable pageable) {
    User me = current.get();
    gates.require(me, Feature.ADVANCED_REPORTS);
    OffsetDateTime cutoff =
        switch (period == null ? "last_6_months" : period) {
          case "last_6_months" -> OffsetDateTime.now().minusMonths(6);
          case "last_year" -> OffsetDateTime.now().minusYears(1);
          default ->
              throw new IllegalArgumentException("period must be last_6_months or last_year");
        };
    List<TopDealResponse> sorted =
        visibleDeals(me).stream()
            .filter(deal -> deal.getCreatedAt() == null || !deal.getCreatedAt().isBefore(cutoff))
            .sorted(
                Comparator.comparing(
                        (Deal deal) ->
                            Optional.ofNullable(deal.getContractValue()).orElse(BigDecimal.ZERO))
                    .reversed())
            .map(
                deal ->
                    new TopDealResponse(
                        deal.getId(),
                        deal.getTitle(),
                        Optional.ofNullable(deal.getContractValue()).orElse(BigDecimal.ZERO),
                        deal.getStage()))
            .toList();
    int from = Math.min((int) pageable.getOffset(), sorted.size());
    int to = Math.min(from + pageable.getPageSize(), sorted.size());
    return new PageImpl<>(sorted.subList(from, to), pageable, sorted.size());
  }

  @Transactional(readOnly = true)
  public AgentStatsResponse agent(UUID id) {
    User me = current.get();
    if (me.getRole() == Role.AGENT && !me.getId().equals(id)) {
      throw new ForbiddenException("Agents may only view their own dashboard stats");
    }
    User user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User"));
    List<Deal> assigned = deals.findByAssignedToId(id);
    return new AgentStatsResponse(
        id,
        user.fullName(),
        assigned.size(),
        revenue(id),
        tasks.countByStatusAndAssigneesId(TaskStatus.DONE, id));
  }

  private DashboardOverviewResponse.CallStats callStats(List<DealLog> callLogs) {
    long total = callLogs.size();
    long noResponses =
        callLogs.stream()
            .filter(
                log ->
                    "NO_RESPONSE".equals(log.getResponseType())
                        || "NO_RESPONSE".equals(log.getCallOutcome()))
            .count();
    long networkInterruptions =
        callLogs.stream()
            .filter(log -> "NETWORK_INTERRUPTION".equals(log.getCallOutcome()))
            .count();
    long customerHungUp =
        callLogs.stream().filter(log -> "CUSTOMER_HUNG_UP".equals(log.getCallOutcome())).count();
    long successful =
        callLogs.stream().filter(log -> "POSITIVE".equals(log.getResponseType())).count();
    double successRate = total == 0 ? 0 : Math.round((successful * 10000.0) / total) / 100.0;
    double averageDuration =
        callLogs.stream()
            .filter(log -> log.getCallDurationSeconds() != null)
            .mapToInt(DealLog::getCallDurationSeconds)
            .average()
            .orElse(0);
    return new DashboardOverviewResponse.CallStats(
        total,
        noResponses,
        networkInterruptions,
        customerHungUp,
        Math.round(averageDuration * 100.0) / 100.0,
        successRate);
  }

  private DashboardOverviewResponse.FollowUpReminder reminder(
      DealLog log, OffsetDateTime time, String type) {
    return time == null
        ? null
        : new DashboardOverviewResponse.FollowUpReminder(
            log.getDeal().getId(), log.getDeal().getTitle(), time, type);
  }

  private List<Deal> visibleDeals(User user) {
    return user.getRole() == Role.AGENT ? deals.findByAssignedToId(user.getId()) : deals.findAll();
  }

  private BigDecimal revenue(UUID userId) {
    return deals.findByAssignedToId(userId).stream()
        .map(this::paid)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal paid(Deal deal) {
    return Optional.ofNullable(deal.getTotalPaid()).orElse(BigDecimal.ZERO);
  }

  private int rankFor(User user) {
    if (user.getRole() != Role.AGENT) return 1;
    List<UUID> ranked =
        users.findAll().stream()
            .filter(candidate -> candidate.getRole() == Role.AGENT && candidate.isActive())
            .sorted(Comparator.comparing((User candidate) -> revenue(candidate.getId())).reversed())
            .map(User::getId)
            .toList();
    int index = ranked.indexOf(user.getId());
    return index < 0 ? ranked.size() + 1 : index + 1;
  }
}
