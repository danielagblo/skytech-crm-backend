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
  private final RatingRepository ratings;
  private final CurrentUserService current;
  private final FeatureGateService gates;

  @Transactional(readOnly = true)
  public DashboardOverviewResponse overview(String period) {
    return overview(period, null);
  }

  @Transactional(readOnly = true)
  public DashboardOverviewResponse overview(String period, UUID requestedUserId) {
    User me = current.get();
        UUID scopedUserId = dashboardScope(me, requestedUserId);
        OffsetDateTime start = periodStart(period);
        Set<UUID> visibleDealIds =
            visibleDeals(me, scopedUserId).stream()
                .map(Deal::getId)
                .collect(java.util.stream.Collectors.toSet());
    List<DealLog> visibleLogs =
                visibleDealIds.isEmpty()
                        ? List.of()
                        : logs.findByDealIdIn(visibleDealIds).stream()
                                .filter(log -> logRecent(log, start))
                                .toList();
    List<Deal> visibleDeals =
        visibleDeals(me, scopedUserId).stream().filter(deal -> dealRecent(deal, start)).toList();

    List<User> visibleUsers =
        scopedUserId == null
            ? users.findAll().stream()
                .filter(user -> Objects.equals(user.getCompanyId(), me.getCompanyId()))
                .toList()
            : List.of(
                users
                    .findById(scopedUserId)
                    .filter(user -> Objects.equals(user.getCompanyId(), me.getCompanyId()))
                    .orElseThrow(() -> new ResourceNotFoundException("User")));
    List<DashboardOverviewResponse.AgentRevenue> revenue =
        visibleUsers.stream()
            .filter(user -> user.isActive() && user.getRole() == Role.AGENT)
            .map(
                user ->
                    new DashboardOverviewResponse.AgentRevenue(
                        user.getId(), user.fullName(), revenue(user.getId(), start)))
            .sorted(
                Comparator.comparing(DashboardOverviewResponse.AgentRevenue::revenue).reversed())
            .limit(10)
            .toList();

    List<DashboardOverviewResponse.ExecutivePerformance> board = rankingBoard(agents());
    List<DashboardOverviewResponse.ExecutivePerformance> performance =
        board.stream()
            .filter(
                p ->
                    visibleUsers.stream().anyMatch(v -> v.getId().equals(p.userId())))
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
            rankIn(board, me),
            board.size(),
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
    return topDeals(period, null, pageable);
  }

  @Transactional(readOnly = true)
  public Page<TopDealResponse> topDeals(String period, UUID requestedUserId, Pageable pageable) {
    User me = current.get();
    UUID scopedUserId = dashboardScope(me, requestedUserId);
    gates.require(me, Feature.ADVANCED_REPORTS);
    OffsetDateTime cutoff =
        switch (period == null ? "last_6_months" : period) {
          case "today", "this_week", "this_month", "three_months" -> periodStart(period);
          case "last_6_months" -> OffsetDateTime.now().minusMonths(6);
          case "last_year" -> OffsetDateTime.now().minusYears(1);
          default ->
              throw new IllegalArgumentException(
                  "period must be today, this_week, this_month, three_months, last_6_months, or last_year");
        };
    List<TopDealResponse> sorted =
            visibleDeals(me, scopedUserId).stream()
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

  private List<Deal> visibleDeals(User user, UUID scopedUserId) {
    if (scopedUserId != null) return deals.findByAssignedToId(scopedUserId);
    return deals.findAll().stream()
        .filter(deal -> Objects.equals(deal.getCompanyId(), user.getCompanyId()))
        .toList();
  }

  private BigDecimal revenue(UUID userId) {
    return deals.findByAssignedToId(userId).stream()
        .map(this::paid)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal revenue(UUID userId, OffsetDateTime start) {
    return deals.findByAssignedToId(userId).stream()
        .filter(deal -> dealRecent(deal, start))
        .map(this::paid)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private UUID dashboardScope(User actor, UUID requestedUserId) {
    if (actor.getRole() == Role.AGENT) {
      if (requestedUserId != null && !actor.getId().equals(requestedUserId))
        throw new ForbiddenException("Agents may only view their own dashboard data");
      return actor.getId();
    }
    if (requestedUserId != null) {
      User requested =
          users.findById(requestedUserId).orElseThrow(() -> new ResourceNotFoundException("User"));
      if (!Objects.equals(requested.getCompanyId(), actor.getCompanyId()))
        throw new ForbiddenException("User belongs to another tenant");
    }
    return requestedUserId;
  }

  private BigDecimal paid(Deal deal) {
    return Optional.ofNullable(deal.getTotalPaid()).orElse(BigDecimal.ZERO);
  }

  private int rankIn(List<DashboardOverviewResponse.ExecutivePerformance> board, User user) {
    if (user.getRole() != Role.AGENT) return 1;
    for (DashboardOverviewResponse.ExecutivePerformance p : board)
      if (p.userId().equals(user.getId())) return p.rank();
    return board.isEmpty() ? 1 : board.size() + 1;
  }

  private List<User> agents() {
    UUID companyId = current.get().getCompanyId();
    return users.findAll().stream()
        .filter(user -> Objects.equals(user.getCompanyId(), companyId))
        .filter(user -> user.isActive() && user.getRole() == Role.AGENT)
        .toList();
  }

  private record AgentMetric(
      UUID userId,
      String name,
      long deals,
      long closed,
      BigDecimal revenue,
      double ratingSum,
      long ratingCount,
      double perfSum,
      long perfCount) {}

  private List<DashboardOverviewResponse.ExecutivePerformance> rankingBoard(List<User> agentUsers) {
    if (agentUsers.isEmpty()) return List.of();

    List<AgentMetric> metrics = new ArrayList<>();
    long teamDeals = 0, teamClosed = 0;
    for (User u : agentUsers) {
      List<Deal> assigned = deals.findByAssignedToId(u.getId());
      long closed =
          assigned.stream().filter(d -> d.getStage() == DealStage.CLIENT_RETENTION).count();
      teamDeals += assigned.size();
      teamClosed += closed;

      double ratingSum = 0;
      long ratingCount = 0;
      for (Rating r : ratings.findByAgentIdOrderByCreatedAtDesc(u.getId()))
        if (Boolean.TRUE.equals(r.getRated()) && r.getRating() != null) {
          ratingSum += r.getRating();
          ratingCount++;
        }

      Set<UUID> ids = assigned.stream().map(Deal::getId).collect(java.util.stream.Collectors.toSet());
      double perfSum = 0;
      long perfCount = 0;
      if (!ids.isEmpty())
        for (DealLog l : logs.findByDealIdIn(ids))
          if (l.getAutoReviewScore() != null) {
            perfSum += l.getAutoReviewScore();
            perfCount++;
          }

      metrics.add(
          new AgentMetric(
              u.getId(),
              u.fullName(),
              assigned.size(),
              closed,
              revenue(u.getId()),
              ratingSum,
              ratingCount,
              perfSum,
              perfCount));
    }

    double teamConversion = teamDeals == 0 ? 0 : (double) teamClosed / teamDeals;
    long totalRatingCount = metrics.stream().mapToLong(AgentMetric::ratingCount).sum();
    double globalRating =
        totalRatingCount == 0
            ? 4.0
            : metrics.stream().mapToDouble(AgentMetric::ratingSum).sum() / totalRatingCount;
    long totalPerfCount = metrics.stream().mapToLong(AgentMetric::perfCount).sum();
    double globalPerf =
        totalPerfCount == 0
            ? 3.0
            : metrics.stream().mapToDouble(AgentMetric::perfSum).sum() / totalPerfCount;

    double maxRevenueLog =
        metrics.stream()
            .map(AgentMetric::revenue)
            .mapToDouble(r -> Math.log1p(Math.max(0, r.doubleValue())))
            .max()
            .orElse(0);
    double maxClosedLog =
        metrics.stream().mapToLong(AgentMetric::closed).mapToDouble(Math::log1p).max().orElse(0);

    List<DashboardOverviewResponse.ExecutivePerformance> board = new ArrayList<>();
    for (AgentMetric m : metrics) {
      // Bayesian shrinkage : pull small samples toward the team baselines so a 1/1
      // 100% conversion or a single 5.0 review cannot dominate the ranking.
      double ratingBayes =
          (m.ratingSum() + RATING_PRIOR * globalRating) / (m.ratingCount() + RATING_PRIOR);
      double perfBayes =
          (m.perfSum() + RATING_PRIOR * globalPerf) / (m.perfCount() + RATING_PRIOR);
      double convBayes =
          (m.closed() + CONV_PRIOR * teamConversion) / (m.deals() + CONV_PRIOR);

      double revNorm =
          maxRevenueLog <= 0
              ? 0
              : Math.log1p(Math.max(0, m.revenue().doubleValue())) / maxRevenueLog;
      double closedNorm = maxClosedLog <= 0 ? 0 : Math.log1p(m.closed()) / maxClosedLog;

      double composite =
          100
              * (WEIGHT_RATING * (ratingBayes / 5.0)
                  + WEIGHT_REVENUE * revNorm
                  + WEIGHT_PERF * (perfBayes / 5.0)
                  + WEIGHT_CLOSED * closedNorm
                  + WEIGHT_CONVERSION * convBayes);

      double rawRating = m.ratingCount() == 0 ? 0 : m.ratingSum() / m.ratingCount();
      double rawConversion = m.deals() == 0 ? 0 : m.closed() * 100.0 / m.deals();
      board.add(
          new DashboardOverviewResponse.ExecutivePerformance(
              m.userId(),
              m.name(),
              m.closed(),
              m.revenue(),
              Math.round(rawConversion * 100.0) / 100.0,
              Math.round(rawRating * 100.0) / 100.0,
              0,
              Math.round(composite * 100.0) / 100.0));
    }

    board.sort(
        Comparator.comparing(DashboardOverviewResponse.ExecutivePerformance::score).reversed());
    for (int i = 0; i < board.size(); i++) {
      DashboardOverviewResponse.ExecutivePerformance p = board.get(i);
      board.set(
          i,
          new DashboardOverviewResponse.ExecutivePerformance(
              p.userId(),
              p.name(),
              p.closedDeals(),
              p.revenue(),
              p.conversionRate(),
              p.rating(),
              i + 1,
              p.score()));
    }
    return board;
  }

  private static final int RATING_PRIOR = 3;
  private static final int CONV_PRIOR = 5;
  private static final double WEIGHT_RATING = 0.25;
  private static final double WEIGHT_REVENUE = 0.25;
  private static final double WEIGHT_PERF = 0.20;
  private static final double WEIGHT_CLOSED = 0.15;
  private static final double WEIGHT_CONVERSION = 0.15;
}
