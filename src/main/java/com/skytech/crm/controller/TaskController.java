package com.skytech.crm.controller;

import com.skytech.crm.dto.request.*;
import com.skytech.crm.dto.response.ApiResponse;
import com.skytech.crm.enums.*;
import com.skytech.crm.service.TaskService;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController extends BaseController {
  private final TaskService tasks;

  @GetMapping
  ApiResponse<?> list(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) TaskStatus status,
      @RequestParam(required = false) UUID assignee,
      @RequestParam(required = false) Priority priority,
      @RequestParam(required = false) Boolean overdue,
      @PageableDefault(size = 20) Pageable p) {
    return ok(tasks.list(search, status, assignee, priority, overdue, p));
  }

  @PostMapping
  ApiResponse<?> create(@Valid @RequestBody CreateTaskRequest r) {
    return ok(tasks.create(r));
  }

  @GetMapping("/stats")
  ApiResponse<?> stats() {
    return ok(tasks.stats());
  }

  @GetMapping("/{id}")
  ApiResponse<?> get(@PathVariable UUID id) {
    return ok(tasks.get(id));
  }

  @PutMapping("/{id}")
  ApiResponse<?> update(@PathVariable UUID id, @Valid @RequestBody UpdateTaskRequest r) {
    return ok(tasks.update(id, r));
  }

  @DeleteMapping("/{id}")
  ApiResponse<Void> delete(@PathVariable UUID id) {
    tasks.delete(id);
    return done("Task deleted");
  }

  @PutMapping("/{id}/status")
  ApiResponse<?> status(@PathVariable UUID id, @Valid @RequestBody TaskStatusUpdateRequest r) {
    return ok(tasks.status(id, r.status()));
  }

  @GetMapping("/{taskId}/subtasks")
  ApiResponse<?> subs(
      @PathVariable UUID taskId, @PageableDefault(size = 20, sort = "createdAt") Pageable p) {
    return ok(tasks.subtasks(taskId, p));
  }

  @PostMapping("/{taskId}/subtasks")
  ApiResponse<?> createSub(@PathVariable UUID taskId, @Valid @RequestBody CreateSubTaskRequest r) {
    return ok(tasks.createSub(taskId, r));
  }

  @PutMapping("/{taskId}/subtasks/{subTaskId}")
  ApiResponse<?> updateSub(
      @PathVariable UUID taskId,
      @PathVariable UUID subTaskId,
      @Valid @RequestBody UpdateSubTaskRequest r) {
    return ok(tasks.updateSub(taskId, subTaskId, r));
  }

  @DeleteMapping("/{taskId}/subtasks/{subTaskId}")
  ApiResponse<Void> deleteSub(@PathVariable UUID taskId, @PathVariable UUID subTaskId) {
    tasks.deleteSub(taskId, subTaskId);
    return done("Subtask deleted");
  }

  @GetMapping("/{taskId}/comments")
  ApiResponse<?> comments(
      @PathVariable UUID taskId, @PageableDefault(size = 20, sort = "createdAt") Pageable p) {
    return ok(tasks.comments(taskId, p));
  }

  @PostMapping("/{taskId}/comments")
  ApiResponse<?> comment(@PathVariable UUID taskId, @Valid @RequestBody CreateCommentRequest r) {
    return ok(tasks.comment(taskId, r, null));
  }

  @PutMapping("/{taskId}/comments/{commentId}")
  ApiResponse<?> updateComment(
      @PathVariable UUID taskId,
      @PathVariable UUID commentId,
      @Valid @RequestBody UpdateCommentRequest r) {
    return ok(tasks.updateComment(taskId, commentId, r));
  }

  @DeleteMapping("/{taskId}/comments/{commentId}")
  ApiResponse<Void> deleteComment(@PathVariable UUID taskId, @PathVariable UUID commentId) {
    tasks.deleteComment(taskId, commentId);
    return done("Comment deleted");
  }

  @PostMapping("/{taskId}/comments/{commentId}/reply")
  ApiResponse<?> reply(
      @PathVariable UUID taskId,
      @PathVariable UUID commentId,
      @Valid @RequestBody CreateCommentRequest r) {
    return ok(tasks.comment(taskId, r, commentId));
  }
}
