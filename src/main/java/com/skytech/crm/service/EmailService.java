package com.skytech.crm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
  private final JavaMailSender mail;

  public void send(String to, String subject, String body) {
    SimpleMailMessage m = new SimpleMailMessage();
    m.setTo(to);
    m.setSubject(subject);
    m.setText(body);
    mail.send(m);
  }
}
