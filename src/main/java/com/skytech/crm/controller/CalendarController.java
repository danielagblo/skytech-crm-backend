package com.skytech.crm.controller;

import com.skytech.crm.dto.request.*;
import com.skytech.crm.dto.response.ApiResponse;
import com.skytech.crm.service.CalendarService;
import jakarta.validation.Valid;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/calendar/events")
@RequiredArgsConstructor
public class CalendarController extends BaseController {
  private final CalendarService calendar;

  @GetMapping
  ApiResponse<?> list(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime to,
      @org.springframework.data.web.PageableDefault(size = 20, sort = "startTime")
          org.springframework.data.domain.Pageable p) {
    return ok(calendar.list(from, to, p));
  }

  @PostMapping
  ApiResponse<?> create(@Valid @RequestBody CreateCalendarEventRequest r) {
    return ok(calendar.create(r));
  }

  @GetMapping("/{id}")
  ApiResponse<?> get(@PathVariable UUID id) {
    return ok(calendar.get(id));
  }

  @PutMapping("/{id}")
  ApiResponse<?> update(@PathVariable UUID id, @Valid @RequestBody UpdateCalendarEventRequest r) {
    return ok(calendar.update(id, r));
  }

  @DeleteMapping("/{id}")
  ApiResponse<Void> delete(@PathVariable UUID id) {
    calendar.delete(id);
    return done("Calendar event deleted");
  }
}
