package com.skytech.crm.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public record UserPerformanceResponse(
    int rank,
    long closedDeals,
    BigDecimal revenue,
    long loggedCallSeconds,
    long activeSessionSeconds,
    Map<String, BigDecimal> byMonth) {}
