package com.skytech.crm.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.skytech.crm.entity.*;
import com.skytech.crm.enums.*;
import com.skytech.crm.mapper.CrmMapper;
import com.skytech.crm.repository.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DealServiceTest {
  @Mock DealRepository deals;
  @Mock DealLogRepository logs;
  @Mock LeadRepository leads;
  @Mock UserRepository users;
  @Mock CurrentUserService current;
  @Mock ActivityService activity;
  @Mock CrmMapper mapper;
  @InjectMocks DealService service;

  @Test
  void stageChangePersistsTransitionLog() {
    UUID dealId = UUID.randomUUID();
    User admin = new User();
    admin.setId(UUID.randomUUID());
    admin.setRole(Role.ADMIN);
    Deal deal = new Deal();
    deal.setId(dealId);
    deal.setTitle("Skytech website");
    deal.setStage(DealStage.NEGOTIATION);
    when(current.get()).thenReturn(admin);
    when(current.id()).thenReturn(admin.getId());
    when(deals.findById(dealId)).thenReturn(Optional.of(deal));
    when(deals.save(deal)).thenReturn(deal);

    service.stage(dealId, DealStage.SETTLEMENT);

    assertThat(deal.getStage()).isEqualTo(DealStage.SETTLEMENT);
    ArgumentCaptor<DealLog> captor = ArgumentCaptor.forClass(DealLog.class);
    verify(logs).save(captor.capture());
    assertThat(captor.getValue().getLogType()).isEqualTo("SETTLEMENT");
    assertThat(captor.getValue().getBody()).contains("NEGOTIATION", "SETTLEMENT");
    verify(activity)
        .log(
            admin.getId(),
            ActivityType.LEAD_STAGE_CHANGED,
            "DEAL",
            dealId,
            "Moved deal from NEGOTIATION to SETTLEMENT");
  }
}
