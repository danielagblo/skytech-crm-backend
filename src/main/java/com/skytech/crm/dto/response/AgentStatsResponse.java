package com.skytech.crm.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record AgentStatsResponse(
    UUID userId, String name, long deals, BigDecimal revenue, long tasksDone) {}
