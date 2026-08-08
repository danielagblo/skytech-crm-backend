package com.skytech.crm.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RatingRequest(@NotNull UUID dealId) {}