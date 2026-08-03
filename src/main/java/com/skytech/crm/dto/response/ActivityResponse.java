package com.skytech.crm.dto.response;

import com.skytech.crm.enums.ActivityType;
import java.time.*;
import java.util.*;

public record ActivityResponse(
    UUID id,
    UUID actorId,
    ActivityType eventType,
    String entityType,
    UUID entityId,
    String description,
    Map<String, Object> metadata,
    OffsetDateTime createdAt) {}
