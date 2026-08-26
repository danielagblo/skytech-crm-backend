package com.skytech.crm.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.skytech.crm.dto.request.CreateAutomationRequest;
import com.skytech.crm.entity.User;
import com.skytech.crm.entity.Lead;
import com.skytech.crm.entity.Automation;
import com.skytech.crm.enums.*;
import com.skytech.crm.mapper.CrmMapper;
import com.skytech.crm.repository.AutomationRepository;
import com.skytech.crm.repository.LeadRepository;
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
    CalendarSyncService calendar = mock(CalendarSyncService.class);
    LeadRepository leads = mock(LeadRepository.class);
    User admin = new User();
    admin.setRole(Role.ADMIN);
    when(current.get()).thenReturn(admin);
    AutomationService service =
        new AutomationService(automations, current, gates, activity, mapper, calendar, leads);
    CreateAutomationRequest request = new CreateAutomationRequest();
    request.setAutomationType(AutomationType.PUBLIC_HOLIDAY);
    request.setName("Holiday greeting");
    request.setTriggerConfig(Map.of("date", "03/06/2026"));

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("YYYY-MM-DD");
    verifyNoInteractions(automations);
  }

  @Test
  void acceptsTopLevelPersonalContactsAndReturnsWaitingExecutionState() {
    AutomationRepository automations = mock(AutomationRepository.class);
    CurrentUserService current = mock(CurrentUserService.class);
    FeatureGateService gates = mock(FeatureGateService.class);
    ActivityService activity = mock(ActivityService.class);
    CrmMapper mapper = mock(CrmMapper.class);
    CalendarSyncService calendar = mock(CalendarSyncService.class);
    LeadRepository leads = mock(LeadRepository.class);
    UUID companyId = UUID.randomUUID(), contactId = UUID.randomUUID();
    User admin = new User();
    admin.setId(UUID.randomUUID());
    admin.setCompanyId(companyId);
    admin.setRole(Role.ADMIN);
    Lead contact = new Lead();
    contact.setId(contactId);
    contact.setCompanyId(companyId);
    when(current.get()).thenReturn(admin);
    when(current.id()).thenReturn(admin.getId());
    when(leads.findAllById(anyCollection())).thenReturn(List.of(contact));
    when(automations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    AutomationService service =
        new AutomationService(automations, current, gates, activity, mapper, calendar, leads);
    CreateAutomationRequest request = new CreateAutomationRequest();
    request.setAutomationType(AutomationType.PERSONAL);
    request.setName("Personal reminder");
    request.setContactIds(new UUID[] {contactId});
    request.setTriggerConfig(Map.of("date", "2026-08-20"));
    request.setSteps(List.of(Map.of("channel", "SMS", "message", "Hello")));

    service.create(request);

    var saved = org.mockito.ArgumentCaptor.forClass(Automation.class);
    verify(automations).save(saved.capture());
    assertThat(saved.getValue().getContactIds()).containsExactly(contactId);
    assertThat(saved.getValue().getExecutionState()).isEqualTo("WAITING");
    assertThat(saved.getValue().getNextRunAt()).isNotNull();
    assertThat(saved.getValue().getRecipientCount()).isZero();
  }
}
