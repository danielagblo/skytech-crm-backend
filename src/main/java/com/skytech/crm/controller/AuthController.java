package com.skytech.crm.controller;

import com.skytech.crm.dto.request.AuthRequests;
import com.skytech.crm.dto.response.*;
import com.skytech.crm.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {
  private final AuthService auth;

  @PostMapping("/login")
  ApiResponse<?> login(@Valid @RequestBody AuthRequests.Login r) {
    return ok(auth.login(r));
  }

  @PostMapping("/verify-otp")
  ApiResponse<?> verify(@Valid @RequestBody AuthRequests.VerifyOtp r) {
    return ok(auth.verify(r));
  }

  @PostMapping("/refresh")
  ApiResponse<?> refresh(@Valid @RequestBody AuthRequests.Refresh r) {
    return ok(auth.refresh(r));
  }

  @PostMapping("/logout")
  ApiResponse<Void> logout() {
    auth.logout();
    return done("Logged out");
  }

  @GetMapping("/me")
  ApiResponse<?> me() {
    return ok(auth.me());
  }
}
