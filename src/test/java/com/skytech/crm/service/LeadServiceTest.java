package com.skytech.crm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    when(users.findById(availableAgent.getId())).thenReturn(Optional.of(availableAgent));
    when(leads.save(any(Lead.class)))
        .thenAnswer(
            invocation -> {
              Lead lead = invocation.getArgument(0);
              lead.setId(UUID.randomUUID());
              return lead;
            });
    when(deals.save(any(Deal.class)))
        .thenAnswer(
            invocation -> {
              Deal deal = invocation.getArgument(0);
              deal.setId(UUID.randomUUID());
              return deal;
            });

    service.create(emptyLead());

    ArgumentCaptor<Lead> saved = ArgumentCaptor.forClass(Lead.class);
    Mockito.verify(leads).save(saved.capture());
    assertThat(saved.getValue().getAssignedTo()).containsExactly(availableAgent.getId());
    ArgumentCaptor<Deal> createdDeal = ArgumentCaptor.forClass(Deal.class);
    Mockito.verify(deals).save(createdDeal.capture());
    assertThat(createdDeal.getValue().getLead()).isSameAs(saved.getValue());
    assertThat(createdDeal.getValue().getStage()).isEqualTo(DealStage.PROSPECTING);
    assertThat(createdDeal.getValue().getAssignedTo()).isSameAs(availableAgent);
  }

  @Test
  void acceptsMultipleAssigneesAndLegacyIndustryWhileCreatingOneDeal() {
    User manager = user(Role.MANAGER, OffsetDateTime.now().minusYears(1));
    User first = user(Role.AGENT, OffsetDateTime.now().minusMonths(2));
    User second = user(Role.AGENT, OffsetDateTime.now().minusMonths(1));
    when(current.get()).thenReturn(manager);
    when(settings.findAll()).thenReturn(List.of());
    when(users.existsById(first.getId())).thenReturn(true);
    when(users.existsById(second.getId())).thenReturn(true);
    when(users.findById(first.getId())).thenReturn(Optional.of(first));
    when(leads.save(any(Lead.class))).thenAnswer(invocation -> {
      Lead lead = invocation.getArgument(0);
      lead.setId(UUID.randomUUID());
      return lead;
    });
    when(deals.save(any(Deal.class))).thenAnswer(invocation -> {
      Deal deal = invocation.getArgument(0);
      deal.setId(UUID.randomUUID());
      return deal;
    });
    LeadRequest request =
        emptyLead()
            .setAssignedTo(new UUID[] {first.getId(), second.getId()})
            .setIndustry("Tech");

    service.create(request);

    ArgumentCaptor<Lead> lead = ArgumentCaptor.forClass(Lead.class);
    verify(leads).save(lead.capture());
    assertThat(lead.getValue().getAssignedTo()).containsExactly(first.getId(), second.getId());
    assertThat(lead.getValue().getCategory()).isEqualTo("Tech");
    assertThat(lead.getValue().getIndustry()).isEqualTo("Tech");
    verify(deals, times(1)).save(any(Deal.class));
  }

  @Test
  void rejectsUnknownLeadCategory() {
    User manager = user(Role.MANAGER, OffsetDateTime.now());
    when(current.get()).thenReturn(manager);

    assertThatThrownBy(() -> service.create(emptyLead().setCategory("Manufacturing")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("category must be one of");
    Mockito.verifyNoInteractions(deals);
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
