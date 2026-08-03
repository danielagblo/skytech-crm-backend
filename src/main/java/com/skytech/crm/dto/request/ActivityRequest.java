package com.skytech.crm.dto.request;

import com.skytech.crm.enums.ActivityType;
import jakarta.validation.constraints.*;
import java.util.*;

public record ActivityRequest(
    @NotNull ActivityType eventType,
    @NotBlank @Pattern(regexp = "LEAD|DEAL|TASK|SYSTEM|AUTOMATION") String entityType,
    UUID entityId,
    @NotBlank String description,
    Map<String, Object> metadata) {}
