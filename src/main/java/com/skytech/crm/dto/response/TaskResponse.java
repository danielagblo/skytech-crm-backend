package com.skytech.crm.dto.response;

import com.skytech.crm.enums.*;
import java.time.*;
import java.util.*;

public record TaskResponse(
    UUID id,
    UUID companyId,
    String title,
    String description,
    TaskStatus status,
    Priority priority,
    UUID createdById,
    boolean allowReminder,
    UUID linkedLeadId,
    UUID linkedDealId,
    OffsetDateTime dueDate,
    Set<UUID> assigneeIds,
    String completionReason,
    Long version,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
