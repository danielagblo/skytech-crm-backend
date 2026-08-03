package com.skytech.crm.service;

import static org.mockito.Mockito.*;

import com.skytech.crm.entity.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AutomationExecutionServiceTest {
  @Test
  void executesOrderedSmsAndEmailStepsForOptedInLead() {
    NotificationService notifications = mock(NotificationService.class);
    AutomationExecutionService service = new AutomationExecutionService(notifications);
    Lead lead = new Lead();
    lead.setSmsOptIn(true);
    lead.setEmailOptIn(true);
    lead.setPhone1("+233500000000");
    lead.setEmail("lead@example.com");
    Automation automation = new Automation();
    automation.setName("Payment workflow");
    automation.setSteps(
        List.of(
            Map.of("channel", "SMS", "message", "Payment received"),
            Map.of("channel", "EMAIL", "subject", "Receipt", "message", "Thank you")));

    service.execute(automation, lead, "fallback");

    var order = inOrder(notifications);
    order.verify(notifications).sendSms("+233500000000", "Payment received");
    order.verify(notifications).sendEmail("lead@example.com", "Receipt", "Thank you");
  }
}
