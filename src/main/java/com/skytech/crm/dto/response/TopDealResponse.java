package com.skytech.crm.dto.response;

import com.skytech.crm.enums.DealStage;
import java.math.BigDecimal;
import java.util.UUID;

public record TopDealResponse(UUID id, String title, BigDecimal value, DealStage stage) {}
