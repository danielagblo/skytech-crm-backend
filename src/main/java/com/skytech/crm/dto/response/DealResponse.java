package com.skytech.crm.dto.response;

import com.skytech.crm.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public record DealResponse(
    UUID id,
    UUID companyId,
    UUID leadId,
    UUID createdById,
    UUID assignedToId,
    String title,
    DealStage stage,
    Priority priority,
    BigDecimal contractValue,
    BigDecimal totalPaid,
    BigDecimal arrears,
    boolean paidInFull,
    LocalDate hostingExpiry,
    LocalDate domainExpiry,
    LocalDate maintenanceExpiry,
    BigDecimal hostingCost,
    BigDecimal domainCost,
    BigDecimal maintenanceCost,
    String notes,
    String customerFirstName,
    String customerLastName,
    String customerEmail,
    String customerPhone,
    String customerCompany,
    String customerAddress,
    String customerCategory,
    Long version,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
