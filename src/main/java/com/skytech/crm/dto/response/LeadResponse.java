package com.skytech.crm.dto.response;

import com.skytech.crm.enums.*;
import java.time.*;
import java.util.*;

public record LeadResponse(
    UUID id,
    UUID companyId,
    UUID[] assignedTo,
    UUID createdById,
    String firstName,
    String lastName,
    String email,
    String phone1,
    String phone2,
    String whatsapp,
    String companyName,
    String role,
    String address,
    String industry,
    String category,
    String leadSource,
    Priority priority,
    LeadStatus status,
    String launchTimeline,
    Boolean hasPublicOffice,
    Boolean meetingArranged,
    LocalDate birthday,
    boolean smsOptIn,
    boolean emailOptIn,
    boolean newsletterOptIn,
    String description,
    int conversionScore,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
