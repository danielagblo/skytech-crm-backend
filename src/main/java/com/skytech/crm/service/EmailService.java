package com.skytech.crm.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.core.io.InputStreamSource;
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
    requireSender();
    SimpleMailMessage m = new SimpleMailMessage();
    m.setFrom(from);
    m.setTo(to);
    m.setSubject(subject);
    m.setText(body);
    mail.send(m);
  }

  public void sendWithAttachment(
      String to, String subject, String body, String fileName, byte[] content) {
    requireSender();
    MimeMessage message = mail.createMimeMessage();
    try {
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(from);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(body, false);
      InputStreamSource source = () -> new ByteArrayInputStream(content);
      helper.addAttachment(fileName, source, "application/pdf");
      mail.send(message);
    } catch (MessagingException exception) {
      throw new IllegalStateException("Unable to construct invoice email", exception);
    }
  }

  private void requireSender() {
    if (from == null || from.isBlank())
      throw new IllegalStateException("Email sender is not configured");
  }
}
