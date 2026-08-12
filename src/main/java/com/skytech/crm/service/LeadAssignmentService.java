package com.skytech.crm.service;

import com.skytech.crm.dto.request.LeadAssignmentConfigRequest;
import com.skytech.crm.dto.response.LeadAssignmentConfigResponse;
import com.skytech.crm.entity.*;
import com.skytech.crm.enums.Role;
import com.skytech.crm.repository.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeadAssignmentService {
  private static final String LEAST_LOADED = "LEAST_LOADED";
  private static final String ROUND_ROBIN = "ROUND_ROBIN";

  private final SettingRepository settings;
  private final UserRepository users;
  private final LeadRepository leads;
  private final CurrentUserService current;

  @Transactional(readOnly = true)
  public LeadAssignmentConfigResponse get() {
    return result(findSetting());
  }

  @Transactional
  public LeadAssignmentConfigResponse update(LeadAssignmentConfigRequest request) {
    Setting setting = findSetting();
    setting.setCompanyId(current.get().getCompanyId());
    setting.setAutoAssignEnabled(request.enabled());
    setting.setLeadAssignmentConfig(normalize(request.config()));
    return result(settings.save(setting));
  }

  @Transactional
  public Optional<UUID> selectIfEnabled() {
    Setting setting = findSetting();
    return setting.isAutoAssignEnabled() ? Optional.of(select(setting)) : Optional.empty();
  }

  @Transactional
  public UUID selectAgent() {
    return select(findSetting());
  }

  private UUID select(Setting setting) {
    UUID companyId = current.get().getCompanyId();
    List<User> candidates = users.lockAssignmentCandidates(companyId, Role.AGENT);
    if (candidates.isEmpty()) throw new IllegalArgumentException("No active agents available");
    String strategy = strategy(setting.getLeadAssignmentConfig());
    if (ROUND_ROBIN.equals(strategy)) {
      int index = Math.floorMod(setting.getAssignmentCursor(), candidates.size());
      setting.setAssignmentCursor(setting.getAssignmentCursor() + 1);
      if (setting.getId() != null) settings.save(setting);
      return candidates.get(index).getId();
    }
    return candidates.stream()
        .min(Comparator.comparingLong(user -> leads.countAssigned(companyId, user.getId())))
        .orElseThrow()
        .getId();
  }

  private Setting findSetting() {
    UUID companyId = current.get().getCompanyId();
    return settings.findTenant(companyId).orElseGet(() -> {
      Setting created = new Setting();
      created.setCompanyId(companyId);
      created.setLeadAssignmentConfig(Map.of("strategy", LEAST_LOADED));
      return created;
    });
  }

  private Map<String, Object> normalize(Map<String, Object> config) {
    if (config == null || config.isEmpty()) return Map.of("strategy", LEAST_LOADED);
    Set<String> unsupported = new LinkedHashSet<>(config.keySet());
    unsupported.remove("strategy");
    if (!unsupported.isEmpty())
      throw new IllegalArgumentException("Unsupported lead assignment config fields: " + unsupported);
    return Map.of("strategy", strategy(config));
  }

  private String strategy(Map<String, Object> config) {
    String value = String.valueOf(
        config == null ? LEAST_LOADED : config.getOrDefault("strategy", LEAST_LOADED))
        .toUpperCase(Locale.ROOT);
    if (!Set.of(LEAST_LOADED, ROUND_ROBIN).contains(value))
      throw new IllegalArgumentException("strategy must be LEAST_LOADED or ROUND_ROBIN");
    return value;
  }

  private LeadAssignmentConfigResponse result(Setting setting) {
    return new LeadAssignmentConfigResponse(
        setting.isAutoAssignEnabled(), Map.of("strategy", strategy(setting.getLeadAssignmentConfig())));
  }
}
