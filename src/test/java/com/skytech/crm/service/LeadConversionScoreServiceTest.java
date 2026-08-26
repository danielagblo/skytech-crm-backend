package com.skytech.crm.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.skytech.crm.entity.Lead;
import com.skytech.crm.enums.LeadStatus;
import org.junit.jupiter.api.Test;

class LeadConversionScoreServiceTest {
  private final LeadConversionScoreService scores = new LeadConversionScoreService();

  @Test
  void calculatesDocumentedLifecycleScoresAndClampsTerminalStates() {
    Lead lead = new Lead();
    lead.setStatus(LeadStatus.NEW);
    assertThat(scores.calculate(lead)).isEqualTo(10);

    lead.setStatus(LeadStatus.CONTACTED);
    assertThat(scores.calculate(lead)).isEqualTo(35);

    lead.setMeetingArranged(true);
    assertThat(scores.calculate(lead)).isEqualTo(50);

    lead.setStatus(LeadStatus.QUALIFIED);
    assertThat(scores.calculate(lead)).isEqualTo(80);

    lead.setStatus(LeadStatus.CONVERTED);
    assertThat(scores.calculate(lead)).isEqualTo(100);

    lead.setStatus(LeadStatus.LOST);
    assertThat(scores.calculate(lead)).isZero();
  }
}
