package com.skytech.crm.dto.request;

import com.skytech.crm.enums.DealStage;
import jakarta.validation.constraints.NotNull;

public record DealStageUpdateRequest(@NotNull DealStage stage) {}
