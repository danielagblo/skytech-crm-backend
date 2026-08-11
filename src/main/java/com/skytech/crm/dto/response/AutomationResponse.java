package com.skytech.crm.dto.response;

import com.skytech.crm.enums.*;
import java.time.*;
import java.util.*;

public record AutomationResponse(
    UUID id,
    AutomationType automationType,
    String name,
    boolean active,
    UUID[] contactIds,
    Map<String, Object> triggerConfig,
    List<Map<String, Object>> steps,
    String executionState,
    OffsetDateTime nextRunAt,
    OffsetDateTime lastExecutedAt,
    String failureReason,
    int recipientCount,
    UUID createdById,
    OffsetDateTime createdAt) {}
