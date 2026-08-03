package com.skytech.crm.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {
  private final String sid, token, from;

  public SmsService(
      @Value("${twilio.account-sid:}") String sid,
      @Value("${twilio.auth-token:}") String token,
      @Value("${twilio.from-number:}") String from) {
    this.sid = sid;
    this.token = token;
    this.from = from;
  }

  public void send(String phone, String body) {
    if (sid.isBlank() || token.isBlank() || from.isBlank())
      throw new IllegalStateException("Twilio is not configured");
    Twilio.init(sid, token);
    Message.creator(new PhoneNumber(phone), new PhoneNumber(from), body).create();
  }
}
