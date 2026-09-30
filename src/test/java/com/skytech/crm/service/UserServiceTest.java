package com.skytech.crm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.skytech.crm.dto.response.UserResponse;
import com.skytech.crm.entity.User;
import com.skytech.crm.enums.Role;
import com.skytech.crm.exception.ForbiddenException;
import com.skytech.crm.mapper.CrmMapper;
import com.skytech.crm.repository.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceTest {

  private UserRepository users;
  private DealRepository deals;
  private DealLogRepository dealLogs;
  private PasswordEncoder passwords;
  private CrmMapper mapper;
  private CurrentUserService current;
  private FeatureGateService gates;
  private ActivityService activity;
  private UserSessionService sessions;
  private RatingRepository ratings;
  private UserService service;

  private User admin;
  private UUID adminId;
  private UUID companyId;

  @BeforeEach
  void setUp() {
    users = mock(UserRepository.class);
    deals = mock(DealRepository.class);
    dealLogs = mock(DealLogRepository.class);
    passwords = mock(PasswordEncoder.class);
    mapper = mock(CrmMapper.class);
    current = mock(CurrentUserService.class);
    gates = mock(FeatureGateService.class);
    activity = mock(ActivityService.class);
    sessions = mock(UserSessionService.class);
    ratings = mock(RatingRepository.class);

    service =
        new UserService(
            users,
            deals,
            dealLogs,
            passwords,
            mapper,
            current,
            gates,
            activity,
            sessions,
            ratings);

    adminId = UUID.randomUUID();
    companyId = UUID.randomUUID();
    admin = new User();
    admin.setId(adminId);
    admin.setCompanyId(companyId);
    admin.setRole(Role.ADMIN);
    when(current.get()).thenReturn(admin);
  }

  @Test
  void photoUploadStoresBase64DataUriInDatabase() {
    when(users.findById(adminId)).thenReturn(Optional.of(admin));
    UserResponse mockResponse = mock(UserResponse.class);
    when(mapper.user(admin)).thenReturn(mockResponse);

    byte[] dummyPng = "fake-png-content".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile file =
        new MockMultipartFile("file", "avatar.png", "image/png", dummyPng);

    UserResponse result = service.photo(adminId, file);

    assertThat(result).isSameAs(mockResponse);
    assertThat(admin.getProfilePhotoUrl()).isNotNull();
    assertThat(admin.getProfilePhotoUrl()).startsWith("data:image/png;base64,");
    verify(users).save(admin);
    verify(activity).log(eq(adminId), any(), eq("SYSTEM"), eq(adminId), eq("Updated profile photo"));
  }

  @Test
  void agentCannotUpdateOtherUserPhoto() {
    User agent = new User();
    UUID agentId = UUID.randomUUID();
    agent.setId(agentId);
    agent.setCompanyId(companyId);
    agent.setRole(Role.AGENT);
    when(current.get()).thenReturn(agent);

    UUID otherUserId = UUID.randomUUID();
    MockMultipartFile file =
        new MockMultipartFile("file", "avatar.png", "image/png", new byte[] {1, 2, 3});

    assertThatThrownBy(() -> service.photo(otherUserId, file))
        .isInstanceOf(ForbiddenException.class);
    verify(users, never()).save(any());
  }

  @Test
  void rejectsUnsupportedFileType() {
    MockMultipartFile file =
        new MockMultipartFile("file", "test.txt", "text/plain", new byte[] {1, 2, 3});

    assertThatThrownBy(() -> service.photo(adminId, file))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Only JPEG, PNG, WebP, and GIF");
    verify(users, never()).save(any());
  }
}
