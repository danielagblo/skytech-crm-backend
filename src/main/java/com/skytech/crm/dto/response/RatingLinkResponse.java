package com.skytech.crm.dto.response;

import java.util.UUID;

public record RatingLinkResponse(
    UUID id, UUID agentId, UUID dealId, String clientEmail, String status, String message) {}