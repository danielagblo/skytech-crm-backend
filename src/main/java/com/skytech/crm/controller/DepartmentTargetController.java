package com.skytech.crm.controller;

import com.skytech.crm.dto.request.DepartmentTargetRequest;
import com.skytech.crm.dto.response.ApiResponse;
import com.skytech.crm.service.DepartmentTargetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class DepartmentTargetController extends BaseController {
  private final DepartmentTargetService targets;

  @GetMapping("/api/v1/department-targets")
  ApiResponse<?> config(@RequestParam String period) {
    return ok(targets.getConfig(period));
  }

  @PutMapping("/api/v1/department-targets")
  ApiResponse<?> save(@RequestParam String period, @Valid @RequestBody DepartmentTargetRequest r) {
    return ok(targets.save(period, r));
  }

  @GetMapping("/api/v1/department-targets/achievement")
  ApiResponse<?> achievement(@RequestParam String period) {
    return ok(targets.achievement(period));
  }
}