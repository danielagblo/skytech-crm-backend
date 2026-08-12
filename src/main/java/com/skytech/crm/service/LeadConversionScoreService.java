package com.skytech.crm.service;

import com.skytech.crm.entity.Lead;
import com.skytech.crm.enums.LeadStatus;
import org.springframework.stereotype.Service;

/** Calculates the conversion score from persisted CRM lifecycle events, never from client input. */
@Service
public class LeadConversionScoreService {
  public int calculate(Lead lead) {
    LeadStatus status = lead.getStatus() == null ? LeadStatus.NEW : lead.getStatus();
    int score =
        switch (status) {
          case NEW -> 10;
          case CONTACTED -> 35;
          case QUALIFIED -> 65;
          case CONVERTED -> 100;
          case LOST -> 0;
        };
    if (status != LeadStatus.CONVERTED
        && status != LeadStatus.LOST
        && Boolean.TRUE.equals(lead.getMeetingArranged())) score += 15;
    return Math.clamp(score, 0, 100);
  }
}
