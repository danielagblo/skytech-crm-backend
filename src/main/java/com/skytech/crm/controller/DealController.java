package com.skytech.crm.controller;

import com.skytech.crm.dto.request.*;
import com.skytech.crm.dto.response.ApiResponse;
import com.skytech.crm.enums.*;
import com.skytech.crm.service.DealService;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/deals")
@RequiredArgsConstructor
public class DealController extends BaseController {
  private final DealService deals;

  @GetMapping
  ApiResponse<?> list(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) DealStage stage,
      @RequestParam(required = false) UUID assignee,
      @RequestParam(required = false) Priority priority,
      @PageableDefault(size = 20) Pageable p) {
    return ok(deals.list(search, stage, assignee, priority, p));
  }

  @PostMapping
  ApiResponse<?> create(@Valid @RequestBody CreateDealRequest r) {
    return ok(deals.create(r));
  }

  @GetMapping("/{id}")
  ApiResponse<?> get(@PathVariable UUID id) {
    return ok(deals.get(id));
  }

  @PutMapping("/{id}")
  ApiResponse<?> update(@PathVariable UUID id, @Valid @RequestBody UpdateDealRequest r) {
    return ok(deals.update(id, r));
  }

  @DeleteMapping("/{id}")
  ApiResponse<Void> delete(@PathVariable UUID id) {
    deals.delete(id);
    return done("Deal deleted");
  }

  @PutMapping("/{id}/stage")
  ApiResponse<?> stage(@PathVariable UUID id, @Valid @RequestBody DealStageUpdateRequest r) {
    return ok(deals.stage(id, r.stage()));
  }
}
