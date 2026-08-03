package com.skytech.crm.dto.response;

import com.skytech.crm.enums.*;
import java.time.*;
import java.util.*;

public record AutomationResponse(
    UUID id,
    AutomationType automationType,
    String name,
    boolean active,
    Map<String, Object> triggerConfig,
    List<Map<String, Object>> steps,
    UUID createdById,
    OffsetDateTime createdAt) {}
