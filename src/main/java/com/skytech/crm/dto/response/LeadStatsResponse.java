package com.skytech.crm.dto.response;

import com.skytech.crm.enums.LeadStatus;
import java.util.Map;

public record LeadStatsResponse(
    long total,
    Map<LeadStatus, Long> countsByStatus,
    Map<String, Long> sourceBreakdown,
    double averageConversionScore) {}
