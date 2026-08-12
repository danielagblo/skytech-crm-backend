package com.skytech.crm.dto.response;

import com.skytech.crm.enums.*;
import java.time.*;
import java.util.*;

public record UserResponse(
    UUID id,
    UUID companyId,
    String firstName,
    String lastName,
    String email,
    String phone,
    String username,
    Role role,
    PlanTier planTier,
    String profilePhotoUrl,
    boolean active,
    OffsetDateTime lastLogin,
    OffsetDateTime lastSeenAt,
    PresenceStatus presenceStatus,
    OffsetDateTime createdAt) {}
