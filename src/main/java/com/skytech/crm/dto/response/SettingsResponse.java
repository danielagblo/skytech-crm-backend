package com.skytech.crm.dto.response;

import java.util.Map;

public record SettingsResponse(
    Map<String, Object> general, LeadAssignmentConfigResponse leadAssignment) {}
