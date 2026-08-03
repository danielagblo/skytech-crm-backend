package com.skytech.crm.controller;

import com.skytech.crm.dto.request.*;
import com.skytech.crm.dto.response.*;
import com.skytech.crm.enums.*;
import com.skytech.crm.service.LeadService;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class LeadController extends BaseController {
  private final LeadService leads;

  @GetMapping
  ApiResponse<?> list(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Priority priority,
      @RequestParam(required = false) LeadStatus status,
      @RequestParam(required = false) String source,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) UUID assignee,
      @PageableDefault(size = 20) Pageable p) {
    return ok(leads.list(search, priority, status, source, category, assignee, p));
  }

  @PostMapping
  ApiResponse<?> create(@Valid @RequestBody CreateLeadRequest r) {
    return ok(leads.create(r));
  }

  @GetMapping("/stats")
  ApiResponse<?> stats() {
    return ok(leads.stats());
  }

  @GetMapping("/auto-assign/config")
  ApiResponse<?> autoGet() {
    return ok(leads.autoConfig());
  }

  @PutMapping("/auto-assign/config")
  ApiResponse<?> autoPut(@RequestBody LeadAssignmentConfigRequest r) {
    return ok(leads.autoConfig(r));
  }

  @GetMapping("/{id}")
  ApiResponse<?> get(@PathVariable UUID id) {
    return ok(leads.get(id));
  }

  @PutMapping("/{id}")
  ApiResponse<?> update(@PathVariable UUID id, @Valid @RequestBody UpdateLeadRequest r) {
    return ok(leads.update(id, r));
  }

  @DeleteMapping("/{id}")
  ApiResponse<Void> delete(@PathVariable UUID id) {
    leads.delete(id);
    return done("Lead deleted");
  }

  @PostMapping("/{id}/convert")
  ApiResponse<?> convert(
      @PathVariable UUID id, @Valid @RequestBody(required = false) LeadConvertRequest r) {
    return ok(leads.convert(id, r));
  }

  @PutMapping("/{id}/assign")
  ApiResponse<?> assign(@PathVariable UUID id, @RequestBody LeadAssignmentRequest r) {
    return ok(leads.assign(id, r.assignees(), r.autoAssign()));
  }
}
