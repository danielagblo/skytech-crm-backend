package com.skytech.crm.service;

import com.skytech.crm.dto.request.*;
import com.skytech.crm.dto.response.*;
import com.skytech.crm.entity.*;
import com.skytech.crm.enums.*;
import com.skytech.crm.exception.*;
import com.skytech.crm.mapper.CrmMapper;
import com.skytech.crm.repository.*;
import jakarta.persistence.criteria.Predicate;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeadService {
  private final LeadRepository leads;
  private final UserRepository users;
  private final DealRepository deals;
  private final SettingRepository settings;
  private final CurrentUserService current;
  private final FeatureGateService gates;
  private final ActivityService activity;
  private final CrmMapper mapper;

  @Transactional(readOnly = true)
  public Page<LeadResponse> list(
      String search,
      Priority priority,
      LeadStatus status,
      String source,
      String category,
      UUID assignee,
      Pageable p) {
    User me = current.get();
    Specification<Lead> s =
        (r, q, b) -> {
          List<Predicate> x = new ArrayList<>();
          if (search != null && !search.isBlank()) {
            String v = "%" + search.toLowerCase() + "%";
            x.add(
                b.or(
                    b.like(b.lower(r.get("firstName")), v),
                    b.like(b.lower(r.get("lastName")), v),
                    b.like(b.lower(r.get("email")), v),
                    b.like(b.lower(r.get("companyName")), v)));
          }
          if (priority != null) x.add(b.equal(r.get("priority"), priority));
          if (status != null) x.add(b.equal(r.get("status"), status));
          if (source != null) x.add(b.equal(r.get("leadSource"), source));
          if (category != null)
            x.add(
                b.or(
                    b.equal(r.get("category"), category), b.equal(r.get("industry"), category)));
          UUID target = me.getRole() == Role.AGENT ? me.getId() : assignee;
          if (target != null)
            x.add(
                b.isNotNull(
                    b.function(
                        "array_position", Integer.class, r.get("assignedTo"), b.literal(target))));
          return b.and(x.toArray(Predicate[]::new));
        };
    return leads.findAll(s, p).map(mapper::lead);
  }

  @Transactional
  public LeadResponse create(LeadRequest r) {
    User me = current.get();
    gates.require(me, Feature.UNLIMITED_LEADS);
    Lead l = new Lead();
    l.setCreatedBy(me);
    apply(l, r);
    if (me.getRole() == Role.AGENT) {
      l.setAssignedTo(new UUID[] {me.getId()});
    } else if (isAutoAssignEnabled()) {
      l.setAssignedTo(new UUID[] {leastLoadedActiveAgent()});
    }
    l = leads.save(l);
    activity.log(me.getId(), ActivityType.LEAD_STATUS_CHANGED, "LEAD", l.getId(), "Created lead");
    return mapper.lead(l);
  }

  @Transactional(readOnly = true)
  public LeadResponse get(UUID id) {
    Lead l = find(id);
    checkOwn(l);
    return mapper.lead(l);
  }

  @Transactional
  public LeadResponse update(UUID id, LeadRequest r) {
    Lead l = find(id);
    checkOwn(l);
    UUID[] original = l.getAssignedTo();
    LeadStatus old = l.getStatus();
    apply(l, r);
    if (current.get().getRole() == Role.AGENT) l.setAssignedTo(original);
    l = leads.save(l);
    activity.log(
        current.id(),
        ActivityType.LEAD_STATUS_CHANGED,
        "LEAD",
        id,
        "Updated lead"
            + (old != l.getStatus() ? " status from " + old + " to " + l.getStatus() : ""));
    return mapper.lead(l);
  }

  @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
  @Transactional
  public void delete(UUID id) {
    Lead l = find(id);
    l.setDeletedAt(OffsetDateTime.now());
    leads.save(l);
    activity.log(current.id(), ActivityType.LEAD_STATUS_CHANGED, "LEAD", id, "Deleted lead");
  }

  @Transactional(readOnly = true)
  public LeadStatsResponse stats() {
    User me = current.get();
    List<Lead> all = me.getRole() == Role.AGENT ? leads.findAssigned(me.getId()) : leads.findAll();
    Map<LeadStatus, Long> by = new EnumMap<>(LeadStatus.class);
    for (LeadStatus s : LeadStatus.values())
      by.put(s, all.stream().filter(l -> l.getStatus() == s).count());
    Map<String, Long> sources = new LinkedHashMap<>();
    all.stream()
        .filter(l -> l.getLeadSource() != null)
        .forEach(l -> sources.merge(l.getLeadSource(), 1L, Long::sum));
    double average = all.stream().mapToInt(Lead::getConversionScore).average().orElse(0);
    return new LeadStatsResponse(all.size(), by, sources, Math.round(average * 100.0) / 100.0);
  }

  @Transactional
  public DealResponse convert(UUID id, LeadConvertRequest req) {
    Lead l = find(id);
    checkOwn(l);
    if (l.getStatus() == LeadStatus.CONVERTED)
      throw new IllegalArgumentException("Lead is already converted");
    User me = current.get();
    Deal d = new Deal();
    d.setLead(l);
    d.setCreatedBy(me);
    d.setTitle(
        req != null && req.title() != null
            ? req.title()
            : ((l.getCompanyName() != null ? l.getCompanyName() : l.getFirstName())
                + " opportunity"));
    d.setStage(DealStage.PROSPECTING);
    d.setPriority(req == null ? l.getPriority() : req.priority());
    if (me.getRole() == Role.AGENT) d.setAssignedTo(me);
    else if (req != null && req.assignedToId() != null)
      d.setAssignedTo(
          users
              .findById(req.assignedToId())
              .orElseThrow(() -> new ResourceNotFoundException("User")));
    else d.setAssignedTo(me);
    if (req != null) d.setContractValue(req.contractValue());
    d = deals.save(d);
    l.setStatus(LeadStatus.CONVERTED);
    leads.save(l);
    activity.log(me.getId(), ActivityType.LEAD_STATUS_CHANGED, "LEAD", l.getId(), "Converted lead");
    activity.log(
        me.getId(),
        ActivityType.LEAD_STAGE_CHANGED,
        "DEAL",
        d.getId(),
        "Created deal from converted lead");
    return mapper.deal(d);
  }

  @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
  @Transactional
  public LeadResponse assign(UUID id, UUID[] ids, boolean auto) {
    Lead l = find(id);
    if (auto) {
      l.setAssignedTo(new UUID[] {leastLoadedActiveAgent()});
    } else {
      if (ids != null)
        for (UUID uid : ids)
          if (!users.existsById(uid)) throw new ResourceNotFoundException("User");
      l.setAssignedTo(ids);
    }
    leads.save(l);
    activity.log(current.id(), ActivityType.LEAD_STAGE_CHANGED, "LEAD", id, "Assigned lead");
    return mapper.lead(l);
  }

  @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
  @Transactional(readOnly = true)
  public LeadAssignmentConfigResponse autoConfig() {
    return settings.findAll().stream()
        .findFirst()
        .map(
            s ->
                new LeadAssignmentConfigResponse(
                    s.isAutoAssignEnabled(),
                    s.getLeadAssignmentConfig() == null ? Map.of() : s.getLeadAssignmentConfig()))
        .orElse(new LeadAssignmentConfigResponse(false, Map.of()));
  }

  @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
  @Transactional
  public LeadAssignmentConfigResponse autoConfig(LeadAssignmentConfigRequest request) {
    Setting s = settings.findAll().stream().findFirst().orElseGet(Setting::new);
    s.setAutoAssignEnabled(request.enabled());
    s.setLeadAssignmentConfig(request.config() == null ? Map.of() : request.config());
    settings.save(s);
    activity.log(
        current.id(),
        ActivityType.LEAD_STAGE_CHANGED,
        "SYSTEM",
        s.getId(),
        "Updated auto assignment config");
    return new LeadAssignmentConfigResponse(s.isAutoAssignEnabled(), s.getLeadAssignmentConfig());
  }

  private Lead find(UUID id) {
    return leads.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lead"));
  }

  private boolean isAutoAssignEnabled() {
    return settings.findAll().stream().findFirst().map(Setting::isAutoAssignEnabled).orElse(false);
  }

  // this version assigns to agents currently online ONLY, 
  // but we don't have a way to track online agents yet, 
  // so we will use the least loaded agent for now
  //
  // private UUID leastLoadedActiveAgent() {
  //   return users.findAll().stream()
  //       .filter(user -> user.getRole() == Role.AGENT && user.isActive())
  //       .min(
  //           Comparator.<User>comparingInt(user -> leads.findAssigned(user.getId()).size())
  //               .thenComparing(User::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
  //       .map(User::getId)
  //       .orElseThrow(() -> new IllegalArgumentException("No active agents available"));
  // }


  // this version assigns to the least loaded agent, regardless of whether they are online or not
  private UUID leastLoadedActiveAgent() {
    return users.findAll().stream()
        .filter(user -> user.getRole() == Role.AGENT && user.isActive())
        .min(
            Comparator.<User>comparingInt(user -> leads.findAssigned(user.getId()).size())
                .thenComparing(
                    User::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder())
                )
        )
        .map(User::getId)
        .orElseThrow(() -> new IllegalArgumentException("No active agents available"));
  }

  private void checkOwn(Lead l) {
    User me = current.get();
    if (me.getRole() == Role.AGENT
        && (l.getAssignedTo() == null
            || Arrays.stream(l.getAssignedTo()).noneMatch(me.getId()::equals)))
      throw new ForbiddenException("Lead is not assigned to you");
  }

  private void apply(Lead l, LeadRequest r) {
    validateCommunicationPreferences(r);
    if (r.getAssignedTo() != null)
      for (UUID id : r.getAssignedTo())
        if (!users.existsById(id)) throw new ResourceNotFoundException("Assignee");
    l.setAssignedTo(r.getAssignedTo());
    l.setFirstName(r.getFirstName());
    l.setLastName(r.getLastName());
    l.setEmail(r.getEmail());
    l.setPhone1(r.getPhone1());
    l.setPhone2(r.getPhone2());
    l.setWhatsapp(r.getWhatsapp());
    l.setCompanyName(r.getCompanyName());
    l.setRole(r.getRole());
    l.setAddress(r.getAddress());
    l.setIndustry(r.getIndustry());
    l.setCategory(r.getCategory() != null ? r.getCategory() : r.getIndustry());
    l.setLeadSource(r.getLeadSource());
    l.setPriority(r.getPriority());
    if (r.getStatus() != null) l.setStatus(r.getStatus());
    l.setLaunchTimeline(r.getLaunchTimeline());
    l.setHasPublicOffice(r.getHasPublicOffice());
    l.setMeetingArranged(r.getMeetingArranged());
    l.setBirthday(r.getBirthday());
    if (r.getSmsOptIn() != null) l.setSmsOptIn(r.getSmsOptIn());
    if (r.getEmailOptIn() != null) l.setEmailOptIn(r.getEmailOptIn());
    if (r.getNewsletterOptIn() != null) l.setNewsletterOptIn(r.getNewsletterOptIn());
    l.setDescription(r.getDescription());
    if (r.getConversionScore() != null) l.setConversionScore(r.getConversionScore());
  }

  private void validateCommunicationPreferences(LeadRequest r) {
    if (Boolean.TRUE.equals(r.getSmsOptIn())
        && (r.getPhone1() == null || r.getPhone1().isBlank()))
      throw new IllegalArgumentException("A phone number is required when SMS communication is selected");
    if ((Boolean.TRUE.equals(r.getEmailOptIn()) || Boolean.TRUE.equals(r.getNewsletterOptIn()))
        && (r.getEmail() == null || r.getEmail().isBlank()))
      throw new IllegalArgumentException(
          "An email address is required when email communication or the newsletter is selected");
  }
}
