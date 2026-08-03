package com.skytech.crm.dto.response;

import java.time.*;
import java.util.*;

public record CalendarEventResponse(
    UUID id,
    String title,
    String description,
    UUID ownerId,
    UUID linkedLeadId,
    UUID linkedDealId,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    String eventType,
    UUID[] assignees,
    OffsetDateTime createdAt) {}
