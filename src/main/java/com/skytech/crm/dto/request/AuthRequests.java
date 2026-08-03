package com.skytech.crm.dto.request;

import jakarta.validation.constraints.*;
import java.util.*;

public final class AuthRequests {
  private AuthRequests() {}

  public record Login(@Email @NotBlank String email, @NotBlank String password) {}

  public record VerifyOtp(@NotNull UUID userId, @NotBlank @Pattern(regexp = "\\d{6}") String otp) {}

  public record Refresh(@NotBlank String refreshToken) {}
}
