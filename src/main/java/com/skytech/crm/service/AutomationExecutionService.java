package com.skytech.crm.service;

import com.skytech.crm.entity.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutomationExecutionService {
  private final NotificationService notifications;

  public int execute(Automation automation, Lead lead, String fallbackMessage) {
    if (lead == null) return 0;
    boolean delivered = false;
    if (automation.getSteps() == null || automation.getSteps().isEmpty()) {
      return send(lead, "BOTH", automation.getName(), fallbackMessage) ? 1 : 0;
    }
    for (Map<String, Object> step : automation.getSteps()) {
      String channel =
          String.valueOf(step.getOrDefault("channel", step.getOrDefault("type", "BOTH")))
              .toUpperCase();
      String body =
          String.valueOf(step.getOrDefault("message", step.getOrDefault("body", fallbackMessage)));
      String subject = String.valueOf(step.getOrDefault("subject", automation.getName()));
      delivered |= send(lead, channel, subject, body);
    }
    return delivered ? 1 : 0;
  }

  public boolean executeStep(Automation automation, Lead lead, int stepIndex) {
    if (lead == null) return false;
    List<Map<String, Object>> steps = automation.getSteps();
    if (steps == null || steps.isEmpty())
      return send(lead, "BOTH", automation.getName(), automation.getName());
    if (stepIndex < 0 || stepIndex >= steps.size())
      throw new IllegalArgumentException("Automation step does not exist: " + stepIndex);
    Map<String, Object> step = steps.get(stepIndex);
    String channel = String.valueOf(step.getOrDefault("channel", step.getOrDefault("type", "BOTH"))).toUpperCase();
    String body = String.valueOf(step.getOrDefault("message", step.getOrDefault("body", automation.getName())));
    String subject = String.valueOf(step.getOrDefault("subject", automation.getName()));
    return send(lead, channel, subject, body);
  }

  private boolean send(Lead lead, String channel, String subject, String body) {
    boolean delivered = false;
    if (("SMS".equals(channel) || "BOTH".equals(channel))
        && lead.isSmsOptIn()
        && lead.getPhone1() != null
        && !lead.getPhone1().isBlank()) {
      notifications.sendSms(lead.getPhone1(), body);
      delivered = true;
    }
    if (("EMAIL".equals(channel) || "BOTH".equals(channel))
        && lead.isEmailOptIn()
        && lead.getEmail() != null
        && !lead.getEmail().isBlank()) {
      notifications.sendEmail(lead.getEmail(), subject, body);
      delivered = true;
    }
    return delivered;
  }
}
