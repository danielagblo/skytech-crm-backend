package com.skytech.crm.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class EmailServiceTest {
  @Test
  void sendsFromTheConfiguredVerifiedAddress() {
    JavaMailSender sender = mock(JavaMailSender.class);
    EmailService service = new EmailService(sender, "noreply@skytech.example");

    service.send("agent@example.com", "OTP", "123456");

    verify(sender)
        .send(
            argThat(
                (SimpleMailMessage message) ->
                    "noreply@skytech.example".equals(message.getFrom())
                        && "agent@example.com".equals(message.getTo()[0])));
  }

  @Test
  void rejectsDeliveryUntilAnEmailSenderIsConfigured() {
    EmailService service = new EmailService(mock(JavaMailSender.class), "");

    assertThatThrownBy(() -> service.send("agent@example.com", "OTP", "123456"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Email sender is not configured");
  }
}
