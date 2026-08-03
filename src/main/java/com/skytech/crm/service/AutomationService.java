package com.skytech.crm.service;

import com.skytech.crm.dto.request.AutomationRequest;
import com.skytech.crm.dto.response.AutomationResponse;
import com.skytech.crm.dto.response.AutomationOptionsResponse;
import com.skytech.crm.entity.*;
import com.skytech.crm.enums.*;
import com.skytech.crm.exception.ResourceNotFoundException;
import com.skytech.crm.mapper.CrmMapper;
import com.skytech.crm.repository.AutomationRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AutomationService {
  private final AutomationRepository automations;
  private final CurrentUserService current;
  private final FeatureGateService gates;
  private final ActivityService activity;
  private final CrmMapper mapper;

  @Transactional(readOnly = true)
  public Page<AutomationResponse> list(Pageable p) {
    gates.require(current.get(), Feature.AUTOMATION_BUILDER);
    return automations.findAll(p).map(mapper::automation);
  }

  @Transactional(readOnly = true)
  public AutomationOptionsResponse options() {
    gates.require(current.get(), Feature.AUTOMATION_BUILDER);
    return new AutomationOptionsResponse(
        List.of(
            new AutomationOptionsResponse.TypeOption(
                AutomationType.BIRTHDAY, true, "LEAD_BIRTHDAY", List.of()),
            new AutomationOptionsResponse.TypeOption(
                AutomationType.PUBLIC_HOLIDAY, true, "DATE", List.of("date")),
            new AutomationOptionsResponse.TypeOption(
                AutomationType.PAYMENT, true, "DEAL_PAYMENT_RECORDED", List.of()),
            new AutomationOptionsResponse.TypeOption(
                AutomationType.PERSONAL, false, "NOT_CONFIGURED", List.of())),
        List.of("SMS", "EMAIL", "BOTH"),
        List.of("channel", "subject", "message"));
  }

  @Transactional
  public AutomationResponse create(AutomationRequest r) {
    gates.require(current.get(), Feature.AUTOMATION_BUILDER);
    Automation a = new Automation();
    a.setCreatedBy(current.get());
    apply(a, r);
    automations.save(a);
    activity.log(
        current.id(),
        ActivityType.LEAD_STAGE_CHANGED,
        "AUTOMATION",
        a.getId(),
        "Created automation");
    return mapper.automation(a);
  }

  @Transactional(readOnly = true)
  public AutomationResponse get(UUID id) {
    gates.require(current.get(), Feature.AUTOMATION_BUILDER);
    return mapper.automation(find(id));
  }

  @Transactional
  public AutomationResponse update(UUID id, AutomationRequest r) {
    gates.require(current.get(), Feature.AUTOMATION_BUILDER);
    Automation a = find(id);
    apply(a, r);
    automations.save(a);
    activity.log(
        current.id(), ActivityType.LEAD_STAGE_CHANGED, "AUTOMATION", id, "Updated automation");
    return mapper.automation(a);
  }

  @Transactional
  public void delete(UUID id) {
    gates.require(current.get(), Feature.AUTOMATION_BUILDER);
    automations.delete(find(id));
    activity.log(
        current.id(), ActivityType.LEAD_STAGE_CHANGED, "AUTOMATION", id, "Deleted automation");
  }

  @Transactional
  public AutomationResponse toggle(UUID id) {
    gates.require(current.get(), Feature.AUTOMATION_BUILDER);
    Automation a = find(id);
    a.setActive(!a.isActive());
    automations.save(a);
    activity.log(
        current.id(), ActivityType.LEAD_STAGE_CHANGED, "AUTOMATION", id, "Toggled automation");
    return mapper.automation(a);
  }

  @Transactional(readOnly = true)
  public Page<AutomationResponse> type(AutomationType t, Pageable p) {
    gates.require(current.get(), Feature.AUTOMATION_BUILDER);
    return automations.findByAutomationType(t, p).map(mapper::automation);
  }

  private Automation find(UUID id) {
    return automations.findById(id).orElseThrow(() -> new ResourceNotFoundException("Automation"));
  }

  private void apply(Automation a, AutomationRequest r) {
    validate(r);
    a.setAutomationType(r.getAutomationType());
    a.setName(r.getName());
    if (r.getActive() != null) a.setActive(r.getActive());
    a.setTriggerConfig(r.getTriggerConfig() == null ? Map.of() : r.getTriggerConfig());
    a.setSteps(r.getSteps() == null ? List.of() : r.getSteps());
  }

  private void validate(AutomationRequest request) {
    if (request.getAutomationType() == AutomationType.PUBLIC_HOLIDAY) {
      Object date =
          request.getTriggerConfig() == null ? null : request.getTriggerConfig().get("date");
      if (date == null || String.valueOf(date).isBlank())
        throw new IllegalArgumentException(
            "Public holiday automations require triggerConfig.date in YYYY-MM-DD format");
      try {
        java.time.LocalDate.parse(String.valueOf(date));
      } catch (java.time.format.DateTimeParseException exception) {
        throw new IllegalArgumentException(
            "Public holiday triggerConfig.date must use YYYY-MM-DD format");
      }
    }
    if (request.getSteps() == null) return;
    for (int index = 0; index < request.getSteps().size(); index++) {
      Map<String, Object> step = request.getSteps().get(index);
      if (step == null) throw new IllegalArgumentException("Automation step " + index + " is required");
      String channel = String.valueOf(step.getOrDefault("channel", step.get("type"))).toUpperCase();
      if (!Set.of("SMS", "EMAIL", "BOTH").contains(channel))
        throw new IllegalArgumentException(
            "Automation step " + index + " channel must be SMS, EMAIL, or BOTH");
      Object message = step.containsKey("message") ? step.get("message") : step.get("body");
      if (message == null || String.valueOf(message).isBlank())
        throw new IllegalArgumentException("Automation step " + index + " requires a message");
    }
  }
}
