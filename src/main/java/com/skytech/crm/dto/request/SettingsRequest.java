package com.skytech.crm.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record SettingsRequest(@NotNull Map<String, Object> general) {}
