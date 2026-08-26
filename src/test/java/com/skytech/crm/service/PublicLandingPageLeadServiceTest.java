package com.skytech.crm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.skytech.crm.dto.request.PublicLandingPageLeadRequest;
import com.skytech.crm.entity.*;
import com.skytech.crm.enums.*;
import com.skytech.crm.repository.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

class PublicLandingPageLeadServiceTest {
  @Mock private LeadRepository leads;
  @Mock private DealRepository deals;
  @Mock private UserRepository users;
  @Mock private LeadAssignmentService assignments;

  private AutoCloseable mocks;
  private PublicLandingPageLeadService service;
  private UUID companyId;

  @BeforeEach
  void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    companyId = UUID.randomUUID();
    service =
        new PublicLandingPageLeadService(
            leads, deals, users, assignments, new LeadConversionScoreService());
    ReflectionTestUtils.setField(service, "configuredCompanyId", companyId.toString());
    when(assignments.selectIfEnabled(companyId)).thenReturn(Optional.empty());
    when(leads.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));
    when(deals.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0)));
  }

  @AfterEach
  void tearDown() throws Exception {
    mocks.close();
  }

  @Test
  void createsProspectingLeadAndDealWithoutInferringCommunicationConsent() {
    var request =
        new PublicLandingPageLeadRequest(
            "Ama Mensah",
            "+233201234567",
            "ama@example.com",
            "Ama Ventures",
            "Tech",
            "Website + SEO",
            "New Build",
            "GH₵6,500",
            "",
            "Very urgent",
            "WELCOME",
            "10% off",
            "Google",
            "Growth Website",
            "GH₵6,500",
            "/forms",
            "Original form summary");

    var response = service.create(request);

    ArgumentCaptor<Lead> leadCaptor = ArgumentCaptor.forClass(Lead.class);
    verify(leads).save(leadCaptor.capture());
    Lead lead = leadCaptor.getValue();
    assertThat(lead.getCompanyId()).isEqualTo(companyId);
    assertThat(lead.getFirstName()).isEqualTo("Ama");
    assertThat(lead.getLastName()).isEqualTo("Mensah");
    assertThat(lead.getPhone1()).isEqualTo("+233201234567");
    assertThat(lead.getCategory()).isEqualTo("Tech");
    assertThat(lead.getPriority()).isEqualTo(Priority.HIGH);
    assertThat(lead.getLaunchTimeline()).isEqualTo("IN_1_WEEK");
    assertThat(lead.isSmsOptIn()).isFalse();
    assertThat(lead.isEmailOptIn()).isFalse();
    assertThat(lead.isNewsletterOptIn()).isFalse();

    ArgumentCaptor<Deal> dealCaptor = ArgumentCaptor.forClass(Deal.class);
    verify(deals).save(dealCaptor.capture());
    assertThat(dealCaptor.getValue().getStage()).isEqualTo(DealStage.PROSPECTING);
    assertThat(dealCaptor.getValue().getLead()).isSameAs(lead);
    assertThat(response.stage()).isEqualTo(DealStage.PROSPECTING);
  }

  private <T extends com.skytech.crm.entity.BaseEntity> T withId(T entity) {
    entity.setId(UUID.randomUUID());
    return entity;
  }
}
