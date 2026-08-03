package com.skytech.crm.controller;

import com.skytech.crm.dto.request.ActivityRequest;
import com.skytech.crm.dto.response.ApiResponse;
import com.skytech.crm.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/activities")
@RequiredArgsConstructor
public class ActivityController extends BaseController {
  private final ActivityService activities;

  @GetMapping
  ApiResponse<?> list(
      @RequestParam(defaultValue = "ALL") String filter,
      @RequestParam(defaultValue = "7") int days,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable p) {
    return ok(activities.list(filter, days, p));
  }

  @PostMapping
  ApiResponse<?> create(@Valid @RequestBody ActivityRequest r) {
    return ok(activities.create(r));
  }
}
