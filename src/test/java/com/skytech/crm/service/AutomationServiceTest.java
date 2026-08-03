package com.skytech.crm.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.skytech.crm.dto.request.CreateAutomationRequest;
import com.skytech.crm.entity.User;
import com.skytech.crm.enums.*;
import com.skytech.crm.mapper.CrmMapper;
import com.skytech.crm.repository.AutomationRepository;
import java.util.*;
import org.junit.jupiter.api.Test;

class AutomationServiceTest {
  @Test
  void rejectsHolidayWithoutAnExplicitIsoDate() {
    AutomationRepository automations = mock(AutomationRepository.class);
    CurrentUserService current = mock(CurrentUserService.class);
    FeatureGateService gates = mock(FeatureGateService.class);
    ActivityService activity = mock(ActivityService.class);
    CrmMapper mapper = mock(CrmMapper.class);
    User admin = new User();
    admin.setRole(Role.ADMIN);
    when(current.get()).thenReturn(admin);
    AutomationService service =
        new AutomationService(automations, current, gates, activity, mapper);
    CreateAutomationRequest request = new CreateAutomationRequest();
    request.setAutomationType(AutomationType.PUBLIC_HOLIDAY);
    request.setName("Holiday greeting");
    request.setTriggerConfig(Map.of("date", "03/06/2026"));

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("YYYY-MM-DD");
    verifyNoInteractions(automations);
  }
}
