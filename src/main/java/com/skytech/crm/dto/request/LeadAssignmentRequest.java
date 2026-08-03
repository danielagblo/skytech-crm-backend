package com.skytech.crm.dto.request;

import java.util.UUID;

public record LeadAssignmentRequest(UUID[] assignees, boolean autoAssign) {}
