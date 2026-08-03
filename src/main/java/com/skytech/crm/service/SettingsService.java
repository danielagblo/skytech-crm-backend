package com.skytech.crm.service;

import com.skytech.crm.dto.request.*;
import com.skytech.crm.dto.response.*;
import com.skytech.crm.entity.Setting;
import com.skytech.crm.enums.ActivityType;
import com.skytech.crm.repository.SettingRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SettingsService {
  private final SettingRepository settings;
  private final CurrentUserService current;
  private final ActivityService activity;

  @Transactional(readOnly = true)
  public SettingsResponse get() {
    return result(one());
  }

  @Transactional
  public SettingsResponse update(SettingsRequest request) {
    Setting s = one();
    s.setGeneralConfig(request.general());
    settings.save(s);
    activity.log(
        current.id(), ActivityType.LEAD_STAGE_CHANGED, "SYSTEM", s.getId(), "Updated settings");
    return result(s);
  }

  @Transactional(readOnly = true)
  public LeadAssignmentConfigResponse assignment() {
    Setting s = one();
    return assignmentResult(s);
  }

  @Transactional
  public LeadAssignmentConfigResponse assignment(LeadAssignmentConfigRequest request) {
    Setting s = one();
    s.setAutoAssignEnabled(request.enabled());
    s.setLeadAssignmentConfig(request.config() == null ? Map.of() : request.config());
    settings.save(s);
    activity.log(
        current.id(),
        ActivityType.LEAD_STAGE_CHANGED,
        "SYSTEM",
        s.getId(),
        "Updated lead assignment settings");
    return assignmentResult(s);
  }

  private Setting one() {
    return settings.findAll().stream().findFirst().orElseGet(Setting::new);
  }

  private LeadAssignmentConfigResponse assignmentResult(Setting s) {
    return new LeadAssignmentConfigResponse(
        s.isAutoAssignEnabled(), Optional.ofNullable(s.getLeadAssignmentConfig()).orElse(Map.of()));
  }

  private SettingsResponse result(Setting s) {
    return new SettingsResponse(
        Optional.ofNullable(s.getGeneralConfig()).orElse(Map.of()), assignmentResult(s));
  }
}
