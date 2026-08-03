package com.skytech.crm.dto.response;

import com.skytech.crm.enums.Priority;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SubTaskResponse(
    UUID id,
    UUID taskId,
    String title,
    String description,
    Priority priority,
    boolean complete,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
