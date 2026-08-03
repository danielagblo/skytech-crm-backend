package com.skytech.crm.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record BroadcastScheduleRequest(@NotNull @Future OffsetDateTime scheduledAt) {}
