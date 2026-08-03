package com.skytech.crm.dto.response;

import java.time.OffsetDateTime;

public record ApiResponse<T>(boolean success, T data, String message, OffsetDateTime timestamp) {
  public static <T> ApiResponse<T> ok(T data, String message) {
    return new ApiResponse<>(true, data, message, OffsetDateTime.now());
  }
}
