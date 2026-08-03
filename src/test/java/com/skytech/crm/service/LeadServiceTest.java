package com.skytech.crm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.skytech.crm.dto.request.*;
import com.skytech.crm.entity.*;
import com.skytech.crm.enums.*;
import com.skytech.crm.mapper.CrmMapper;
import com.skytech.crm.repository.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {
  @Mock LeadRepository leads;
  @Mock UserRepository users;
  @Mock DealRepository deals;
  @Mock SettingRepository settings;
  @Mock CurrentUserService current;
  @Mock FeatureGateService gates;
  @Mock ActivityService activity;
  @Mock CrmMapper mapper;
  @InjectMocks LeadService service;

  @Test
  void configuredAutoAssignmentSelectsTheLeastLoadedActiveAgent() {
    User manager = user(Role.MANAGER, OffsetDateTime.now().minusYears(1));
    User busyAgent = user(Role.AGENT, OffsetDateTime.now().minusMonths(2));
    User availableAgent = user(Role.AGENT, OffsetDateTime.now().minusMonths(1));
    Setting setting = new Setting();
    setting.setAutoAssignEnabled(true);

    when(current.get()).thenReturn(manager);
    when(settings.findAll()).thenReturn(List.of(setting));
    when(users.findAll()).thenReturn(List.of(manager, busyAgent, availableAgent));
    when(leads.findAssigned(busyAgent.getId())).thenReturn(List.of(new Lead(), new Lead()));
    when(leads.findAssigned(availableAgent.getId())).thenReturn(List.of());
    when(leads.save(any(Lead.class)))
        .thenAnswer(
            invocation -> {
              Lead lead = invocation.getArgument(0);
              lead.setId(UUID.randomUUID());
              return lead;
            });

    service.create(emptyLead());

    ArgumentCaptor<Lead> saved = ArgumentCaptor.forClass(Lead.class);
    Mockito.verify(leads).save(saved.capture());
    assertThat(saved.getValue().getAssignedTo()).containsExactly(availableAgent.getId());
  }

  private User user(Role role, OffsetDateTime createdAt) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setRole(role);
    user.setActive(true);
    user.setCreatedAt(createdAt);
    return user;
  }

  private LeadRequest emptyLead() {
    return new CreateLeadRequest()
        .setFirstName("Jane")
        .setLastName("Doe")
        .setEmail("jane@example.com")
        .setCompanyName("Example Ltd")
        .setPriority(Priority.MEDIUM)
        .setStatus(LeadStatus.NEW)
        .setConversionScore(50);
  }
}
