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
  private final LeadAssignmentService assignments;

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
    return assignments.get();
  }

  @Transactional
  public LeadAssignmentConfigResponse assignment(LeadAssignmentConfigRequest request) {
    LeadAssignmentConfigResponse result = assignments.update(request);
    activity.log(
        current.id(),
        ActivityType.LEAD_STAGE_CHANGED,
        "SYSTEM",
        null,
        "Updated lead assignment settings");
    return result;
  }

  private Setting one() {
    UUID companyId = current.get().getCompanyId();
    return settings.findTenant(companyId).orElseGet(() -> {
      Setting value = new Setting();
      value.setCompanyId(companyId);
      return value;
    });
  }

  private SettingsResponse result(Setting s) {
    return new SettingsResponse(
        Optional.ofNullable(s.getGeneralConfig()).orElse(Map.of()), assignments.get());
  }
}
