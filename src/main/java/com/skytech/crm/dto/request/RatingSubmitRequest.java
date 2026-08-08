package com.skytech.crm.dto.request;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record RatingSubmitRequest(
    @NotNull UUID ratingId,
    @NotNull @Min(1) @Max(5) Integer rating,
    @Size(max = 2000) String feedback,
    @Size(max = 120) String clientName) {}