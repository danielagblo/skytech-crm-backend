package com.skytech.crm.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.skytech.crm.entity.User;
import com.skytech.crm.enums.Role;
import com.skytech.crm.exception.ForbiddenException;
import com.skytech.crm.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {
  @Test
  void agentCannotRequestAnotherUsersDashboard() {
    CurrentUserService current = mock(CurrentUserService.class);
    User agent = user(Role.AGENT, UUID.randomUUID());
    when(current.get()).thenReturn(agent);
    DashboardService service = service(current, mock(UserRepository.class));

    assertThatThrownBy(() -> service.overview("today", UUID.randomUUID()))
        .isInstanceOf(ForbiddenException.class)
        .hasMessageContaining("own dashboard");
  }

  @Test
  void managerCanScopeDashboardToSpecificAgent() {
    CurrentUserService current = mock(CurrentUserService.class);
    UserRepository users = mock(UserRepository.class);
    UUID companyId = UUID.randomUUID();
    User manager = user(Role.MANAGER, companyId);
    User agent = user(Role.AGENT, companyId);
    when(current.get()).thenReturn(manager);
    when(users.findById(agent.getId())).thenReturn(Optional.of(agent));
    when(users.findAll()).thenReturn(List.of(manager, agent));
    DashboardService service = service(current, users);

    var result = service.overview("this_month", agent.getId());

    assertThat(result.topRevenuePerAgent()).singleElement().satisfies(item ->
        assertThat(item.userId()).isEqualTo(agent.getId()));
    assertThat(result.executivePerformance()).singleElement().satisfies(item ->
        assertThat(item.userId()).isEqualTo(agent.getId()));
  }

  private DashboardService service(CurrentUserService current, UserRepository users) {
    return new DashboardService(
        mock(DealRepository.class),
        mock(DealLogRepository.class),
        mock(TaskRepository.class),
        users,
        mock(RatingRepository.class),
        current,
        mock(FeatureGateService.class));
  }

  private User user(Role role, UUID companyId) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setRole(role);
    user.setCompanyId(companyId);
    user.setActive(true);
    user.setFirstName(role.name());
    user.setLastName("User");
    return user;
  }
}
