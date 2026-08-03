package com.skytech.crm.controller;

import com.skytech.crm.dto.response.*;
import org.springframework.data.domain.Page;

abstract class BaseController {
  protected <T> ApiResponse<T> ok(T data) {
    return ApiResponse.ok(data, "Success");
  }

  protected <T> ApiResponse<PageResponse<T>> ok(Page<T> data) {
    return ApiResponse.ok(PageResponse.from(data), "Success");
  }

  protected ApiResponse<Void> done(String message) {
    return ApiResponse.ok(null, message);
  }
}
