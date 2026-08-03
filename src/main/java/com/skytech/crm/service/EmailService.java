package com.skytech.crm.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
  private final JavaMailSender mail;
  private final String from;

  public EmailService(JavaMailSender mail, @Value("${mail.from:}") String from) {
    this.mail = mail;
    this.from = from;
  }

  public void send(String to, String subject, String body) {
    if (from == null || from.isBlank())
      throw new IllegalStateException("Email sender is not configured");
    SimpleMailMessage m = new SimpleMailMessage();
    m.setFrom(from);
    m.setTo(to);
    m.setSubject(subject);
    m.setText(body);
    mail.send(m);
  }
}
