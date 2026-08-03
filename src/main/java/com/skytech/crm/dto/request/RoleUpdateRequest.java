package com.skytech.crm.dto.request;

import com.skytech.crm.enums.Role;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(@NotNull Role role) {}
