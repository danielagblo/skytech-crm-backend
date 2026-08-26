package com.skytech.crm.service;

import com.skytech.crm.config.AuthenticationConfig;
import com.skytech.crm.dto.request.AuthRequests;
import com.skytech.crm.dto.response.*;
import com.skytech.crm.entity.User;
import com.skytech.crm.enums.ActivityType;
import com.skytech.crm.exception.ForbiddenException;
import com.skytech.crm.mapper.CrmMapper;
import com.skytech.crm.repository.UserRepository;
import com.skytech.crm.security.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final UserRepository users;
  private final PasswordEncoder passwords;
  private final JwtTokenProvider tokens;
  private final NotificationService notifications;
  private final ActivityService activity;
  private final CrmMapper mapper;
  private final CurrentUserService current;
  private final AuthenticationConfig authConfig;
  private final UserSessionService sessions;

  @Transactional
  public AuthResponse.Login login(AuthRequests.Login req) {
    User u = users.findByEmailIgnoreCase(req.email()).orElse(null);
    if (u == null || !u.isActive() || !passwords.matches(req.password(), u.getPasswordHash())) {
      activity.logRejectedLogin(u == null ? null : u.getId(), req.email());
      throw new ForbiddenException("Invalid credentials");
    }
    if (!authConfig.otpEnabled()) return completePasswordLogin(u);

    String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
    u.setOtpCode(otp);
    u.setOtpExpiresAt(OffsetDateTime.now().plusMinutes(10));
    users.save(u);
    activity.log(
        u.getId(), ActivityType.LEAD_STAGE_CHANGED, "SYSTEM", u.getId(), "Issued login OTP");
    if (u.getPhone() != null && !u.getPhone().isBlank())
      notifications.sendSms(u.getPhone(), "Your Skytech CRM verification code is " + otp);
    else
      notifications.sendEmail(
          u.getEmail(),
          "Skytech CRM verification code",
          "Your verification code is " + otp + ". It expires in 10 minutes.");
    return new AuthResponse.Login(true, u.getId());
  }

  private AuthResponse.Login completePasswordLogin(User u) {
    String access = tokens.access(u);
    String refresh = tokens.refresh(u);
    u.setOtpCode(null);
    u.setOtpExpiresAt(null);
    u.setRefreshTokenHash(hash(refresh));
    u.setLastLogin(OffsetDateTime.now());
    u.setLastSeenAt(OffsetDateTime.now());
    users.save(u);
    sessions.start(u);
    activity.log(
        u.getId(), ActivityType.LEAD_STAGE_CHANGED, "SYSTEM", u.getId(), "Completed password login");
    return new AuthResponse.Login(false, u.getId(), access, refresh, mapper.user(u));
  }

  @Transactional
  public AuthResponse.Tokens verify(AuthRequests.VerifyOtp req) {
    User u = users.findById(req.userId()).orElseThrow(() -> new ForbiddenException("Invalid OTP"));
    if (u.getOtpCode() == null
        || u.getOtpExpiresAt() == null
        || u.getOtpExpiresAt().isBefore(OffsetDateTime.now())
        || !MessageDigest.isEqual(
            u.getOtpCode().getBytes(StandardCharsets.UTF_8),
            req.otp().getBytes(StandardCharsets.UTF_8))) {
      activity.logRejectedOtp(u.getId());
      throw new ForbiddenException("Invalid or expired OTP");
    }
    String access = tokens.access(u), refresh = tokens.refresh(u);
    u.setOtpCode(null);
    u.setOtpExpiresAt(null);
    u.setRefreshTokenHash(hash(refresh));
    u.setLastLogin(OffsetDateTime.now());
    u.setLastSeenAt(OffsetDateTime.now());
    users.save(u);
    sessions.start(u);
    activity.log(
        u.getId(), ActivityType.LEAD_STAGE_CHANGED, "SYSTEM", u.getId(), "Completed OTP login");
    return new AuthResponse.Tokens(access, refresh, mapper.user(u));
  }

  @Transactional
  public AuthResponse.AccessToken refresh(AuthRequests.Refresh req) {
    if (!tokens.valid(req.refreshToken(), "refresh"))
      throw new ForbiddenException("Invalid refresh token");
    User u =
        users
            .findById(tokens.userId(req.refreshToken()))
            .orElseThrow(() -> new ForbiddenException("Invalid refresh token"));
    if (!u.isActive()
        || u.getRefreshTokenHash() == null
        || !MessageDigest.isEqual(
            u.getRefreshTokenHash().getBytes(StandardCharsets.UTF_8),
            hash(req.refreshToken()).getBytes(StandardCharsets.UTF_8)))
      throw new ForbiddenException("Refresh token revoked");
    return new AuthResponse.AccessToken(tokens.access(u));
  }

  @Transactional
  public void logout() {
    User u = current.get();
    sessions.end(u);
    u.setRefreshTokenHash(null);
    users.save(u);
    activity.log(u.getId(), ActivityType.LEAD_STAGE_CHANGED, "SYSTEM", u.getId(), "Logged out");
  }

  @Transactional(readOnly = true)
  public UserResponse me() {
    return mapper.user(current.get());
  }

  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
