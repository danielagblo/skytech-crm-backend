package com.skytech.crm.dto.response;

import com.skytech.crm.enums.DealStage;
import java.util.Map;

public record ContactSegmentsResponse(long all, Map<DealStage, Long> byStage) {}
