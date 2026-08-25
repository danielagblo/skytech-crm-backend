package com.skytech.crm.service;

import com.skytech.crm.dto.request.PublicLandingPageLeadRequest;
import com.skytech.crm.dto.response.PublicLandingPageLeadResponse;
import com.skytech.crm.entity.*;
import com.skytech.crm.enums.*;
import com.skytech.crm.repository.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicLandingPageLeadService {
  private final LeadRepository leads;
  private final DealRepository deals;
  private final UserRepository users;
  private final LeadAssignmentService assignments;
  private final LeadConversionScoreService conversionScores;

  @Value("${landing-page-leads.company-id:}")
  private String configuredCompanyId;

  @Transactional
  public PublicLandingPageLeadResponse create(PublicLandingPageLeadRequest request) {
    UUID companyId = resolveCompanyId();
    Name name = splitName(request.name());

    Lead lead = new Lead();
    lead.setCompanyId(companyId);
    lead.setFirstName(name.first());
    lead.setLastName(name.last());
    lead.setPhone1(clean(request.phone()));
    lead.setWhatsapp(clean(request.phone()));
    lead.setEmail(clean(request.email()));
    lead.setCompanyName(clean(request.company()));
    lead.setIndustry(request.industry().trim());
    lead.setCategory(request.industry().trim());
    lead.setLeadSource("GOOGLE");
    lead.setStatus(LeadStatus.NEW);
    lead.setPriority(priority(request.urgency()));
    lead.setLaunchTimeline(launchTimeline(request));
    // A submitted phone/email is contact data, not legal consent to send communications.
    lead.setSmsOptIn(false);
    lead.setEmailOptIn(false);
    lead.setNewsletterOptIn(false);
    lead.setDescription(notes(request));
    try {
      Optional<UUID> assignee = assignments.selectIfEnabled(companyId);
      if (assignee.isPresent()) lead.setAssignedTo(new UUID[] {assignee.get()});
    } catch (IllegalArgumentException exception) {
      log.warn("Public lead saved without assignment: {}", exception.getMessage());
    }
    lead.setConversionScore(conversionScores.calculate(lead));
    lead = leads.save(lead);

    Deal deal = new Deal();
    deal.setCompanyId(companyId);
    deal.setLead(lead);
    deal.setTitle(title(request, lead));
    deal.setStage(DealStage.PROSPECTING);
    deal.setPriority(lead.getPriority());
    deal.setNotes(lead.getDescription());
    if (lead.getAssignedTo() != null && lead.getAssignedTo().length > 0)
      users.findById(lead.getAssignedTo()[0]).ifPresent(deal::setAssignedTo);
    deal = deals.save(deal);

    return new PublicLandingPageLeadResponse(lead.getId(), deal.getId(), deal.getStage());
  }

  private UUID resolveCompanyId() {
    String configured = clean(configuredCompanyId);
    if (configured != null) {
      try {
        return UUID.fromString(configured);
      } catch (IllegalArgumentException exception) {
        throw new IllegalStateException("LANDING_PAGE_LEADS_COMPANY_ID is not a valid UUID");
      }
    }
    List<UUID> companies = users.findActiveCompanyIds();
    if (companies.size() != 1)
      throw new IllegalStateException(
          "LANDING_PAGE_LEADS_COMPANY_ID must be configured when there is not exactly one active tenant");
    return companies.getFirst();
  }

  private Name splitName(String value) {
    String normalized = value.trim().replaceAll("\\s+", " ");
    int separator = normalized.indexOf(' ');
    String first = separator < 0 ? normalized : normalized.substring(0, separator);
    String last = separator < 0 ? null : normalized.substring(separator + 1);
    if (first.length() > 100 || (last != null && last.length() > 100))
      throw new IllegalArgumentException("First name and last name must each be at most 100 characters");
    return new Name(first, last);
  }

  private Priority priority(String urgency) {
    return switch (Optional.ofNullable(clean(urgency)).orElse("")) {
      case "Very urgent" -> Priority.HIGH;
      case "Urgent" -> Priority.MEDIUM;
      case "Soon" -> Priority.LOW;
      default -> null;
    };
  }

  private String launchTimeline(PublicLandingPageLeadRequest request) {
    String timeline = clean(request.timeline());
    if (timeline != null
        && Set.of("IN_1_WEEK", "ONE_TO_TWO_MONTHS", "THREE_PLUS_MONTHS").contains(timeline))
      return timeline;
    return switch (Optional.ofNullable(clean(request.urgency())).orElse("")) {
      case "Very urgent" -> "IN_1_WEEK";
      case "Urgent" -> "ONE_TO_TWO_MONTHS";
      case "Soon" -> "THREE_PLUS_MONTHS";
      default -> null;
    };
  }

  private String title(PublicLandingPageLeadRequest request, Lead lead) {
    String subject = firstPresent(request.packageName(), request.building(), lead.getCompanyName(), lead.getFirstName());
    String value = subject + " opportunity";
    return value.length() <= 255 ? value : value.substring(0, 255);
  }

  private String notes(PublicLandingPageLeadRequest request) {
    List<String> values = new ArrayList<>();
    add(values, "Landing page", request.source());
    add(values, "Building", request.building());
    add(values, "Project type", request.projectType());
    add(values, "Budget", request.budget());
    add(values, "Timeline", request.timeline());
    add(values, "Urgency", request.urgency());
    add(values, "Coupon", request.coupon());
    add(values, "Coupon offer", request.couponLabel());
    add(values, "Referral", request.referral());
    add(values, "Package", request.packageName());
    add(values, "Package price", request.packagePrice());
    add(values, "Original message", request.message());
    return String.join("\n", values);
  }

  private void add(List<String> values, String label, String raw) {
    String value = clean(raw);
    if (value != null) values.add(label + ": " + value);
  }

  private String firstPresent(String... values) {
    for (String raw : values) {
      String value = clean(raw);
      if (value != null) return value;
    }
    return "Landing page lead";
  }

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private record Name(String first, String last) {}
}
