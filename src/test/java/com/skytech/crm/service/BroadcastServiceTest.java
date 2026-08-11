package com.skytech.crm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.skytech.crm.dto.request.BroadcastRequest;
import com.skytech.crm.entity.*;
import com.skytech.crm.enums.*;
import com.skytech.crm.mapper.CrmMapper;
import com.skytech.crm.repository.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BroadcastServiceTest {
  private final BroadcastRepository broadcasts = mock(BroadcastRepository.class);
  private final LeadRepository leads = mock(LeadRepository.class);
  private final DealRepository deals = mock(DealRepository.class);
  private final CurrentUserService current = mock(CurrentUserService.class);
  private final SmsService sms = mock(SmsService.class);
  private final EmailService email = mock(EmailService.class);
  private final User manager = new User();
  private final UUID companyId = UUID.randomUUID();
  private BroadcastService service;

  @BeforeEach
  void setUp() {
    manager.setId(UUID.randomUUID());
    manager.setCompanyId(companyId);
    manager.setRole(Role.MANAGER);
    when(current.get()).thenReturn(manager);
    when(current.id()).thenReturn(manager.getId());
    service =
        new BroadcastService(
            broadcasts,
            leads,
            deals,
            current,
            mock(FeatureGateService.class),
            sms,
            email,
            mock(ActivityService.class),
            mock(CrmMapper.class),
            mock(CalendarSyncService.class));
  }

  @Test
  void persistsFutureBroadcastAsWaitingWithExplicitContacts() {
    Lead selected = lead("+233500000001", companyId);
    when(leads.findAllById(anyCollection())).thenReturn(List.of(selected));
    when(broadcasts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    BroadcastRequest request = request();
    request.setContactIds(new UUID[] {selected.getId()});
    request.setScheduledAt(OffsetDateTime.now().plusHours(1));

    service.create(request);

    var saved = org.mockito.ArgumentCaptor.forClass(BroadcastMessage.class);
    verify(broadcasts).save(saved.capture());
    assertThat(saved.getValue().getStatus()).isEqualTo(BroadcastStatus.WAITING);
    assertThat(saved.getValue().getContactIds()).containsExactly(selected.getId());
    assertThat(saved.getValue().getCompanyId()).isEqualTo(companyId);
  }

  @Test
  void unionsStagesLeadIdsAndExplicitContactsWithoutDuplicates() {
    Lead explicit = lead("+233500000001", companyId);
    Lead staged = lead("+233500000002", companyId);
    Lead otherTenant = lead("+233500000003", UUID.randomUUID());
    Deal stagedDeal = new Deal();
    stagedDeal.setCompanyId(companyId);
    stagedDeal.setLead(staged);
    BroadcastMessage broadcast = broadcast(explicit);
    broadcast.setSegmentFilter(
        Map.of(
            "stages", List.of("PROSPECTING"),
            "lead_ids", List.of(explicit.getId().toString())));
    when(broadcasts.findById(broadcast.getId())).thenReturn(Optional.of(broadcast));
    when(leads.findAll()).thenReturn(List.of(explicit, staged, otherTenant));
    when(deals.findByStage(DealStage.PROSPECTING)).thenReturn(List.of(stagedDeal));
    when(broadcasts.save(broadcast)).thenReturn(broadcast);

    service.send(broadcast.getId());

    verify(sms, times(1)).send(explicit.getPhone1(), broadcast.getMessageContent());
    verify(sms, times(1)).send(staged.getPhone1(), broadcast.getMessageContent());
    verify(sms, never()).send(otherTenant.getPhone1(), broadcast.getMessageContent());
    assertThat(broadcast.getRecipientCount()).isEqualTo(2);
    assertThat(broadcast.getStatus()).isEqualTo(BroadcastStatus.SENT);
  }

  @Test
  void persistsProviderFailureDetails() {
    Lead selected = lead("+233500000001", companyId);
    BroadcastMessage broadcast = broadcast(selected);
    when(broadcasts.findById(broadcast.getId())).thenReturn(Optional.of(broadcast));
    when(leads.findAll()).thenReturn(List.of(selected));
    doThrow(new IllegalStateException("provider unavailable"))
        .when(sms)
        .send(selected.getPhone1(), broadcast.getMessageContent());

    service.send(broadcast.getId());

    assertThat(broadcast.getStatus()).isEqualTo(BroadcastStatus.FAILED);
    assertThat(broadcast.getFailureDetails()).contains("provider unavailable");
    assertThat(broadcast.getRecipientCount()).isZero();
  }

  private BroadcastRequest request() {
    return new BroadcastRequest()
        .setName("Reminder")
        .setChannel("SMS")
        .setMessageContent("Hello")
        .setSegmentFilter(Map.of());
  }

  private Lead lead(String phone, UUID tenant) {
    Lead lead = new Lead();
    lead.setId(UUID.randomUUID());
    lead.setCompanyId(tenant);
    lead.setPhone1(phone);
    lead.setSmsOptIn(true);
    return lead;
  }

  private BroadcastMessage broadcast(Lead selected) {
    BroadcastMessage broadcast = new BroadcastMessage();
    broadcast.setId(UUID.randomUUID());
    broadcast.setCompanyId(companyId);
    broadcast.setCreatedBy(manager);
    broadcast.setName("Reminder");
    broadcast.setChannel("SMS");
    broadcast.setMessageContent("Hello");
    broadcast.setStatus(BroadcastStatus.DRAFT);
    broadcast.setContactIds(new UUID[] {selected.getId()});
    broadcast.setSegmentFilter(Map.of());
    return broadcast;
  }
}
