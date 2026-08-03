package com.skytech.crm.controller;

import com.skytech.crm.dto.request.*;
import com.skytech.crm.dto.response.ApiResponse;
import com.skytech.crm.enums.AutomationType;
import com.skytech.crm.service.AutomationService;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/automations")
@RequiredArgsConstructor
public class AutomationController extends BaseController {
  private final AutomationService automation;

  @GetMapping
  ApiResponse<?> list(@PageableDefault(size = 20) Pageable p) {
    return ok(automation.list(p));
  }

  @PostMapping
  ApiResponse<?> create(@Valid @RequestBody CreateAutomationRequest r) {
    return ok(automation.create(r));
  }

  @GetMapping("/options")
  ApiResponse<?> options() {
    return ok(automation.options());
  }

  @GetMapping("/birthday-configs")
  ApiResponse<?> birthday(@PageableDefault(size = 20) Pageable p) {
    return ok(automation.type(AutomationType.BIRTHDAY, p));
  }

  @GetMapping("/holiday-configs")
  ApiResponse<?> holidays(@PageableDefault(size = 20) Pageable p) {
    return ok(automation.type(AutomationType.PUBLIC_HOLIDAY, p));
  }

  @GetMapping("/payment-workflows")
  ApiResponse<?> payments(@PageableDefault(size = 20) Pageable p) {
    return ok(automation.type(AutomationType.PAYMENT, p));
  }

  @GetMapping("/{id}")
  ApiResponse<?> get(@PathVariable UUID id) {
    return ok(automation.get(id));
  }

  @PutMapping("/{id}")
  ApiResponse<?> update(@PathVariable UUID id, @Valid @RequestBody UpdateAutomationRequest r) {
    return ok(automation.update(id, r));
  }

  @DeleteMapping("/{id}")
  ApiResponse<Void> delete(@PathVariable UUID id) {
    automation.delete(id);
    return done("Automation deleted");
  }

  @PutMapping("/{id}/toggle")
  ApiResponse<?> toggle(@PathVariable UUID id) {
    return ok(automation.toggle(id));
  }
}
