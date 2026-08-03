package com.skytech.crm.controller;

import com.skytech.crm.dto.request.*;
import com.skytech.crm.dto.response.*;
import com.skytech.crm.service.UserService;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController extends BaseController {
  private final UserService users;

  @GetMapping
  ApiResponse<?> list(
      @RequestParam(required = false) String search, @PageableDefault(size = 20) Pageable p) {
    return ok(users.list(search, p));
  }

  @PostMapping
  ApiResponse<?> create(@Valid @RequestBody CreateUserRequest r) {
    return ok(users.create(r));
  }

  @GetMapping("/{id}")
  ApiResponse<?> get(@PathVariable UUID id) {
    return ok(users.get(id));
  }

  @PutMapping("/{id}")
  ApiResponse<?> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest r) {
    return ok(users.update(id, r));
  }

  @DeleteMapping("/{id}")
  ApiResponse<Void> delete(@PathVariable UUID id) {
    users.delete(id);
    return done("User deleted");
  }

  @PutMapping("/{id}/photo")
  ApiResponse<?> photo(@PathVariable UUID id, @RequestPart("file") MultipartFile f) {
    return ok(users.photo(id, f));
  }

  @GetMapping("/{id}/performance")
  ApiResponse<?> performance(@PathVariable UUID id) {
    return ok(users.performance(id));
  }

  @PutMapping("/{id}/role")
  ApiResponse<?> role(@PathVariable UUID id, @Valid @RequestBody RoleUpdateRequest r) {
    return ok(users.role(id, r.role()));
  }
}
