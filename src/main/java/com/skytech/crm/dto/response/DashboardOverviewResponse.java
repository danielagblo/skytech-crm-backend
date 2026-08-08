package com.skytech.crm.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

public record DashboardOverviewResponse(
    CallStats outgoingCalls,
    CallStats incomingCalls,
    List<AgentRevenue> topRevenuePerAgent,
    List<ExecutivePerformance> executivePerformance,
    List<FollowUpReminder> followUpReminders,
    List<RecentPayment> recentPayments,
    AgentRank agentRank) {
  public record CallStats(
      long total,
      long nonResponses,
      long networkInterruptions,
      long customerHungUp,
      double avgDuration,
      double successRate) {}

  public record AgentRevenue(UUID userId, String name, BigDecimal revenue) {}

  public record ExecutivePerformance(
      UUID userId,
      String name,
      long closedDeals,
      BigDecimal revenue,
      double conversionRate,
      double rating,
      int rank,
      double score) {}

  public record FollowUpReminder(
      UUID dealId, String dealTitle, OffsetDateTime followUpAt, String type) {}

  public record RecentPayment(
      UUID dealId, String dealTitle, BigDecimal amount, OffsetDateTime paidAt) {}

  public record AgentRank(
      int rank,
      long totalAgents,
      long screenTime,
      double targetAchievement,
      BigDecimal salesRevenue) {}
}
