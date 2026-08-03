package com.skytech.crm.controller;

import com.skytech.crm.dto.request.*;
import com.skytech.crm.dto.response.ApiResponse;
import com.skytech.crm.service.DealLogService;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/deals/{dealId}/logs")
@RequiredArgsConstructor
public class DealLogController extends BaseController {
  private final DealLogService logs;

  @GetMapping
  ApiResponse<?> list(
      @PathVariable UUID dealId,
      @org.springframework.data.web.PageableDefault(
              size = 20,
              sort = "createdAt",
              direction = org.springframework.data.domain.Sort.Direction.DESC)
          org.springframework.data.domain.Pageable p) {
    return ok(logs.list(dealId, p));
  }

  @PostMapping
  ApiResponse<?> create(@PathVariable UUID dealId, @Valid @RequestBody CreateDealLogRequest r) {
    return ok(logs.create(dealId, r));
  }

  @GetMapping("/{logId}")
  ApiResponse<?> get(@PathVariable UUID dealId, @PathVariable UUID logId) {
    return ok(logs.get(dealId, logId));
  }

  @PutMapping("/{logId}")
  ApiResponse<?> update(
      @PathVariable UUID dealId,
      @PathVariable UUID logId,
      @Valid @RequestBody UpdateDealLogRequest r) {
    return ok(logs.update(dealId, logId, r));
  }

  @DeleteMapping("/{logId}")
  ApiResponse<Void> delete(@PathVariable UUID dealId, @PathVariable UUID logId) {
    logs.delete(dealId, logId);
    return done("Deal log deleted");
  }

  @GetMapping("/{logId}/comments")
  ApiResponse<?> comments(
      @PathVariable UUID dealId,
      @PathVariable UUID logId,
      @org.springframework.data.web.PageableDefault(size = 20, sort = "createdAt")
          org.springframework.data.domain.Pageable p) {
    return ok(logs.comments(dealId, logId, p));
  }

  @PostMapping("/{logId}/comments")
  ApiResponse<?> comment(
      @PathVariable UUID dealId,
      @PathVariable UUID logId,
      @Valid @RequestBody CreateCommentRequest r) {
    return ok(logs.comment(dealId, logId, r, null));
  }

  @PutMapping("/{logId}/comments/{commentId}")
  ApiResponse<?> updateComment(
      @PathVariable UUID dealId,
      @PathVariable UUID logId,
      @PathVariable UUID commentId,
      @Valid @RequestBody UpdateCommentRequest r) {
    return ok(logs.updateComment(dealId, logId, commentId, r));
  }

  @DeleteMapping("/{logId}/comments/{commentId}")
  ApiResponse<Void> deleteComment(
      @PathVariable UUID dealId, @PathVariable UUID logId, @PathVariable UUID commentId) {
    logs.deleteComment(dealId, logId, commentId);
    return done("Comment deleted");
  }

  @PostMapping("/{logId}/comments/{commentId}/reply")
  ApiResponse<?> reply(
      @PathVariable UUID dealId,
      @PathVariable UUID logId,
      @PathVariable UUID commentId,
      @Valid @RequestBody CreateCommentRequest r) {
    return ok(logs.comment(dealId, logId, r, commentId));
  }
}
