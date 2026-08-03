package com.skytech.crm.dto.response;

import com.skytech.crm.enums.*;
import java.time.*;
import java.util.*;

public record BroadcastResponse(
    UUID id,
    String name,
    String messageContent,
    String channel,
    BroadcastStatus status,
    int recipientCount,
    Map<String, Object> segmentFilter,
    UUID createdById,
    OffsetDateTime scheduledAt,
    OffsetDateTime sentAt,
    OffsetDateTime createdAt) {}
