package com.skytech.crm.dto.request;

import java.util.Map;

public record LeadAssignmentConfigRequest(boolean enabled, Map<String, Object> config) {}
