package com.skytech.crm.dto.response;

import java.util.*;

public final class AuthResponse {
  private AuthResponse() {}

  public record Login(boolean requiresOtp, UUID userId) {}

  public record Tokens(String accessToken, String refreshToken, UserResponse user) {}

  public record AccessToken(String accessToken) {}
}
