package com.skytech.crm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
  private final SmsService sms;
  private final EmailService email;

  @Async
  public void sendSms(String phone, String message) {
    try {
      sms.send(phone, message);
    } catch (Exception e) {
      log.warn("SMS delivery failed for {}: {}", phone, e.getMessage());
    }
  }

  @Async
  public void sendEmail(String to, String subject, String body) {
    try {
      email.send(to, subject, body);
    } catch (Exception e) {
      log.warn("Email delivery failed for {}: {}", to, e.getMessage());
    }
  }
}
