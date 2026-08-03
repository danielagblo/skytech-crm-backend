package com.skytech.crm.service;

import com.skytech.crm.entity.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutomationExecutionService {
  private final NotificationService notifications;

  public void execute(Automation automation, Lead lead, String fallbackMessage) {
    if (lead == null) return;
    if (automation.getSteps() == null || automation.getSteps().isEmpty()) {
      send(lead, "BOTH", automation.getName(), fallbackMessage);
      return;
    }
    for (Map<String, Object> step : automation.getSteps()) {
      String channel =
          String.valueOf(step.getOrDefault("channel", step.getOrDefault("type", "BOTH")))
              .toUpperCase();
      String body =
          String.valueOf(step.getOrDefault("message", step.getOrDefault("body", fallbackMessage)));
      String subject = String.valueOf(step.getOrDefault("subject", automation.getName()));
      send(lead, channel, subject, body);
    }
  }

  private void send(Lead lead, String channel, String subject, String body) {
    if (("SMS".equals(channel) || "BOTH".equals(channel))
        && lead.isSmsOptIn()
        && lead.getPhone1() != null
        && !lead.getPhone1().isBlank()) {
      notifications.sendSms(lead.getPhone1(), body);
    }
    if (("EMAIL".equals(channel) || "BOTH".equals(channel))
        && lead.isEmailOptIn()
        && lead.getEmail() != null
        && !lead.getEmail().isBlank()) {
      notifications.sendEmail(lead.getEmail(), subject, body);
    }
  }
}
