package com.skytech.crm.controller;

import com.skytech.crm.dto.response.ApiResponse;
import com.skytech.crm.service.InAppNotificationService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class InAppNotificationController extends BaseController {
  private final InAppNotificationService notifications;

  @GetMapping
  ApiResponse<?> list(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ok(notifications.list(pageable));
  }

  @GetMapping("/unread-count")
  ApiResponse<?> unreadCount() {
    return ok(Map.of("count", notifications.unreadCount()));
  }

  @PostMapping("/{id}/read")
  ApiResponse<?> markRead(@PathVariable UUID id) {
    return ok(notifications.markRead(id));
  }

  @PostMapping("/read-all")
  ApiResponse<Void> markAllRead() {
    notifications.markAllRead();
    return done("Notifications marked as read");
  }
}
