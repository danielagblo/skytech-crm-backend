package com.skytech.crm.dto.request;

import com.skytech.crm.enums.Priority;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record LeadConvertRequest(
    @Size(max = 255) String title,
    UUID assignedToId,
    Priority priority,
    @PositiveOrZero @Digits(integer = 13, fraction = 2) BigDecimal contractValue) {}
