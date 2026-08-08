package com.skytech.crm.service;

import java.util.UUID;

public record RatingEmailRequested(
    UUID ratingId, String toEmail, String agentName, String dealTitle, String link) {}