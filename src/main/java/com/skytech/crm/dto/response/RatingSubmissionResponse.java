package com.skytech.crm.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RatingSubmissionResponse(
    UUID id,
    UUID agentId,
    UUID dealId,
    Integer rating,
    String feedback,
    String clientName,
    OffsetDateTime ratedAt) {}