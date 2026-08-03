package com.skytech.crm.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.*;

public final class AuthResponse {
  private AuthResponse() {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Login(
      boolean requiresOtp,
      UUID userId,
      String accessToken,
      String refreshToken,
      UserResponse user) {
    public Login(boolean requiresOtp, UUID userId) {
      this(requiresOtp, userId, null, null, null);
    }
  }

  public record Tokens(String accessToken, String refreshToken, UserResponse user) {}

  public record AccessToken(String accessToken) {}
}
