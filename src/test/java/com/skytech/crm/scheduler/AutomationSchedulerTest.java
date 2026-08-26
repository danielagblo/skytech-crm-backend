package com.skytech.crm.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.skytech.crm.entity.*;
import com.skytech.crm.enums.AutomationType;
import com.skytech.crm.repository.*;
import com.skytech.crm.service.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AutomationSchedulerTest {
  @Test
  void executesDuePersonalAutomationAndPersistsExecutionState() {
    LeadRepository leads = mock(LeadRepository.class);
    AutomationRepository automations = mock(AutomationRepository.class);
    AutomationExecutionService execution = mock(AutomationExecutionService.class);
    UUID companyId = UUID.randomUUID(), contactId = UUID.randomUUID();
    Lead lead = new Lead();
    lead.setId(contactId);
    lead.setCompanyId(companyId);
    Automation automation = new Automation();
    automation.setId(UUID.randomUUID());
    automation.setCompanyId(companyId);
    automation.setAutomationType(AutomationType.PERSONAL);
    automation.setActive(true);
    automation.setExecutionState("WAITING");
    automation.setContactIds(new UUID[] {contactId});
    automation.setTriggerConfig(
        Map.of("date", LocalDate.now(ZoneId.of("Africa/Accra")).toString()));
    when(automations.findByAutomationTypeAndIsActiveTrue(AutomationType.PERSONAL))
        .thenReturn(List.of(automation));
    when(leads.findById(contactId)).thenReturn(Optional.of(lead));
    when(execution.execute(automation, lead, automation.getName())).thenReturn(1);
    AutomationScheduler scheduler =
        new AutomationScheduler(
            leads,
            mock(DealRepository.class),
            mock(DealLogRepository.class),
            mock(CalendarEventRepository.class),
            automations,
            mock(ActivityRepository.class),
            mock(ActivityService.class),
            mock(NotificationService.class),
            execution,
            mock(BroadcastService.class),
            mock(TaskService.class));
    ReflectionTestUtils.setField(scheduler, "timeZone", "Africa/Accra");

    scheduler.personalAutomations();

    assertThat(automation.getExecutionState()).isEqualTo("COMPLETED");
    assertThat(automation.getRecipientCount()).isEqualTo(1);
    assertThat(automation.getLastExecutedAt()).isNotNull();
    assertThat(automation.getNextRunAt()).isNull();
    verify(automations).save(automation);
  }
}
