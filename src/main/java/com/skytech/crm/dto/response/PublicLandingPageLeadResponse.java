package com.skytech.crm.dto.response;

import com.skytech.crm.enums.DealStage;
import java.util.UUID;

public record PublicLandingPageLeadResponse(UUID leadId, UUID dealId, DealStage stage) {}
