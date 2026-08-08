package com.skytech.crm.dto.response;

import java.util.UUID;

public record RatingInfoResponse(
    UUID id,
    UUID agentId,
    String agentName,
    String dealTitle,
    Boolean rated,
    Integer rating,
    String feedback) {}