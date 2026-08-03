package com.skytech.crm.controller;

import com.skytech.crm.dto.request.*;
import com.skytech.crm.dto.response.ApiResponse;
import com.skytech.crm.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController extends BaseController {
  private final SettingsService settings;

  @GetMapping
  ApiResponse<?> get() {
    return ok(settings.get());
  }

  @PutMapping
  ApiResponse<?> put(@Valid @RequestBody SettingsRequest r) {
    return ok(settings.update(r));
  }

  @GetMapping("/lead-assignment")
  ApiResponse<?> assignment() {
    return ok(settings.assignment());
  }

  @PutMapping("/lead-assignment")
  ApiResponse<?> assignment(@RequestBody LeadAssignmentConfigRequest r) {
    return ok(settings.assignment(r));
  }
}
