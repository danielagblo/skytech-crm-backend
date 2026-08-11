package com.skytech.crm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.skytech.crm.config.AuthenticationConfig;
import com.skytech.crm.dto.request.AuthRequests;
import com.skytech.crm.dto.response.AuthResponse;
import com.skytech.crm.dto.response.UserResponse;
import com.skytech.crm.entity.User;
import com.skytech.crm.mapper.CrmMapper;
import com.skytech.crm.repository.UserRepository;
import com.skytech.crm.security.JwtTokenProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

  @Test
  void bypassModeReturnsTokensImmediatelyWithoutSendingOtp() {
    Fixture fixture = new Fixture(false);

    AuthResponse.Login response =
        fixture.service.login(new AuthRequests.Login(fixture.user.getEmail(), "password"));

    assertThat(response.requiresOtp()).isFalse();
    assertThat(response.userId()).isEqualTo(fixture.user.getId());
    assertThat(response.accessToken()).isEqualTo("access-token");
    assertThat(response.refreshToken()).isEqualTo("refresh-token");
    assertThat(response.user()).isSameAs(fixture.responseUser);
    assertThat(fixture.user.getRefreshTokenHash()).isNotBlank();
    assertThat(fixture.user.getLastLogin()).isNotNull();
    assertThat(fixture.user.getOtpCode()).isNull();
    verifyNoInteractions(fixture.notifications);
    verify(fixture.users).save(fixture.user);
  }

  @Test
  void enabledModeKeepsTheExistingOtpChallenge() {
    Fixture fixture = new Fixture(true);

    AuthResponse.Login response =
        fixture.service.login(new AuthRequests.Login(fixture.user.getEmail(), "password"));

    assertThat(response.requiresOtp()).isTrue();
    assertThat(response.userId()).isEqualTo(fixture.user.getId());
    assertThat(response.accessToken()).isNull();
    assertThat(fixture.user.getOtpCode()).matches("\\d{6}");
    assertThat(fixture.user.getOtpExpiresAt()).isNotNull();
    verify(fixture.notifications)
        .sendEmail(
            eq(fixture.user.getEmail()),
            eq("Skytech CRM verification code"),
            contains(fixture.user.getOtpCode()));
    verify(fixture.tokens, never()).access(any());
  }

  @Test
  void refreshReturnsANewAccessTokenWithoutInvalidatingTheRefreshSession() {
    Fixture fixture = new Fixture(false);
    fixture.service.login(new AuthRequests.Login(fixture.user.getEmail(), "password"));
    when(fixture.tokens.valid("refresh-token", "refresh")).thenReturn(true);
    when(fixture.tokens.userId("refresh-token")).thenReturn(fixture.user.getId());
    when(fixture.users.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
    when(fixture.tokens.access(fixture.user)).thenReturn("refreshed-access-token");

    AuthResponse.AccessToken response =
        fixture.service.refresh(new AuthRequests.Refresh("refresh-token"));

    assertThat(response.accessToken()).isEqualTo("refreshed-access-token");
    assertThat(fixture.user.getRefreshTokenHash()).isNotBlank();
    verify(fixture.users, times(1)).save(fixture.user);
  }

  private static final class Fixture {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final JwtTokenProvider tokens = mock(JwtTokenProvider.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final ActivityService activity = mock(ActivityService.class);
    private final CrmMapper mapper = mock(CrmMapper.class);
    private final CurrentUserService current = mock(CurrentUserService.class);
    private final UserResponse responseUser = mock(UserResponse.class);
    private final User user = new User();
    private final AuthService service;

    private Fixture(boolean otpEnabled) {
      user.setId(UUID.randomUUID());
      user.setEmail("admin@skytechghana.com");
      user.setPasswordHash("encoded-password");
      user.setActive(true);
      when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
      when(passwords.matches("password", user.getPasswordHash())).thenReturn(true);
      when(tokens.access(user)).thenReturn("access-token");
      when(tokens.refresh(user)).thenReturn("refresh-token");
      when(mapper.user(user)).thenReturn(responseUser);
      service =
          new AuthService(
              users,
              passwords,
              tokens,
              notifications,
              activity,
              mapper,
              current,
              new AuthenticationConfig(otpEnabled));
    }
  }
}
