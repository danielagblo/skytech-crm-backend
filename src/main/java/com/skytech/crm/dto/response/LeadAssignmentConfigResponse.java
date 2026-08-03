package com.skytech.crm.dto.response;

import java.util.Map;

public record LeadAssignmentConfigResponse(boolean enabled, Map<String, Object> config) {}
