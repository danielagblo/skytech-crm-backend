package com.skytech.crm.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InAppNotificationResponse(
    UUID id,
    String type,
    String title,
    String body,
    String href,
    boolean read,
    OffsetDateTime createdAt) {}
