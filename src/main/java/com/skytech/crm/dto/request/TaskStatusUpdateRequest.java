package com.skytech.crm.dto.request;

import com.skytech.crm.enums.TaskStatus;

public record TaskStatusUpdateRequest(TaskStatus status, String reason) {}
