package com.skytech.crm.controller;

import com.skytech.crm.dto.response.ApiResponse;
import com.skytech.crm.service.DashboardService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController extends BaseController {
  private final DashboardService dashboard;

  @GetMapping("/overview")
  ApiResponse<?> overview(@RequestParam(defaultValue = "today") String period) {
    return ok(dashboard.overview(period));
  }

  @GetMapping("/top-deals")
  ApiResponse<?> top(
      @RequestParam(defaultValue = "last_6_months") String period,
      @PageableDefault(size = 20) Pageable pageable) {
    return ok(dashboard.topDeals(period, pageable));
  }

  @GetMapping("/agent-stats/{userId}")
  ApiResponse<?> agent(@PathVariable UUID userId) {
    return ok(dashboard.agent(userId));
  }
}
