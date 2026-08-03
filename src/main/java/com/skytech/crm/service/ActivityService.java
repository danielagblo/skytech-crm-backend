package com.skytech.crm.service;

import com.skytech.crm.dto.request.ActivityRequest;
import com.skytech.crm.dto.response.ActivityResponse;
import com.skytech.crm.entity.*;
import com.skytech.crm.enums.ActivityType;
import com.skytech.crm.mapper.CrmMapper;
import com.skytech.crm.repository.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityService {
  private final ActivityRepository activities;
  private final UserRepository users;
  private final CrmMapper mapper;
  private final CurrentUserService current;

  @Transactional
  public void log(
      UUID actorId, ActivityType type, String entityType, UUID entityId, String description) {
    persist(actorId, type, entityType, entityId, description);
  }

  private Activity persist(
      UUID actorId, ActivityType type, String entityType, UUID entityId, String description) {
    Activity a = new Activity();
    if (actorId != null) users.findById(actorId).ifPresent(a::setActor);
    a.setEventType(type);
    a.setEntityType(entityType);
    a.setEntityId(entityId);
    a.setDescription(description);
    return activities.save(a);
  }

  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
  public void logRejectedLogin(UUID actorId, String email) {
    log(
        actorId,
        ActivityType.UNAUTHORIZED_LOGIN,
        "SYSTEM",
        actorId,
        "Rejected login attempt for " + email);
  }

  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
  public void logRejectedOtp(UUID actorId) {
    log(
        actorId,
        ActivityType.UNAUTHORIZED_LOGIN,
        "SYSTEM",
        actorId,
        "Rejected OTP verification attempt");
  }

  @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
  @Transactional
  public ActivityResponse create(ActivityRequest request) {
    Activity a =
        persist(
            current.id(),
            request.eventType(),
            request.entityType(),
            request.entityId(),
            request.description());
    a.setMetadata(request.metadata() == null ? Map.of() : request.metadata());
    return mapper.activity(activities.save(a));
  }

  @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
  @Transactional(readOnly = true)
  public Page<ActivityResponse> list(String filter, int days, Pageable pageable) {
    String normalized = filter == null ? "ALL" : filter.toUpperCase();
    if (!Set.of("ALL", "LEADS", "SYSTEMS", "AUTOMATION", "AGENT_ACTION").contains(normalized))
      throw new IllegalArgumentException("Unsupported activity filter: " + filter);
    if (days < 1 || days > 365)
      throw new IllegalArgumentException("days must be between 1 and 365");
    OffsetDateTime since = OffsetDateTime.now().minusDays(days);
    org.springframework.data.jpa.domain.Specification<Activity> spec =
        (r, q, b) -> {
          var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
          predicates.add(b.greaterThan(r.get("createdAt"), since));
          String f = normalized;
          if ("LEADS".equals(f)) predicates.add(r.get("entityType").in("LEAD", "DEAL"));
          else if ("SYSTEMS".equals(f)) predicates.add(b.equal(r.get("entityType"), "SYSTEM"));
          else if ("AUTOMATION".equals(f))
            predicates.add(b.equal(r.get("entityType"), "AUTOMATION"));
          else if ("AGENT_ACTION".equals(f)) predicates.add(b.isNotNull(r.get("actor")));
          return b.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    return activities.findAll(spec, pageable).map(mapper::activity);
  }
}
