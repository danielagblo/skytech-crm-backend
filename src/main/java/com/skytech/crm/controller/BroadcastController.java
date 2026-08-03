package com.skytech.crm.controller;

import com.skytech.crm.dto.request.*;
import com.skytech.crm.dto.response.ApiResponse;
import com.skytech.crm.service.BroadcastService;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class BroadcastController extends BaseController {
  private final BroadcastService broadcasts;

  @GetMapping("/api/v1/broadcasts")
  ApiResponse<?> list(@PageableDefault(size = 20) Pageable p) {
    return ok(broadcasts.list(p));
  }

  @PostMapping("/api/v1/broadcasts")
  ApiResponse<?> create(@Valid @RequestBody CreateBroadcastRequest r) {
    return ok(broadcasts.create(r));
  }

  @GetMapping("/api/v1/broadcasts/recent-activity")
  ApiResponse<?> recent(
      @RequestParam(defaultValue = "7") int days,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable p) {
    return ok(broadcasts.recent(days, p));
  }

  @GetMapping("/api/v1/contacts/segments")
  ApiResponse<?> segments() {
    return ok(broadcasts.segments());
  }

  @GetMapping("/api/v1/broadcasts/{id}")
  ApiResponse<?> get(@PathVariable UUID id) {
    return ok(broadcasts.get(id));
  }

  @PutMapping("/api/v1/broadcasts/{id}")
  ApiResponse<?> update(@PathVariable UUID id, @Valid @RequestBody UpdateBroadcastRequest r) {
    return ok(broadcasts.update(id, r));
  }

  @DeleteMapping("/api/v1/broadcasts/{id}")
  ApiResponse<Void> delete(@PathVariable UUID id) {
    broadcasts.delete(id);
    return done("Broadcast deleted");
  }

  @PostMapping("/api/v1/broadcasts/{id}/send")
  ApiResponse<?> send(@PathVariable UUID id) {
    return ok(broadcasts.send(id));
  }

  @PostMapping("/api/v1/broadcasts/{id}/schedule")
  ApiResponse<?> schedule(@PathVariable UUID id, @Valid @RequestBody BroadcastScheduleRequest r) {
    return ok(broadcasts.schedule(id, r.scheduledAt()));
  }
}
