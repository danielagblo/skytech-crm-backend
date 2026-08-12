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
  private static final Set<String> CATEGORIES =
      Set.of(
          "Hospitality",
          "Retail & E-commerce",
          "Education",
          "Tourism & Logistics",
          "Real estate & construction",
          "Healthcare",
          "Tech",
          "NGO",
          "Religion",
          "Other");
  private final LeadRepository leads;
  private final UserRepository users;
  private final DealRepository deals;
  private final LeadAssignmentService assignments;
  private final LeadConversionScoreService conversionScores;
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
    l.setCompanyId(me.getCompanyId());
    apply(l, r, me.getRole() != Role.AGENT);
    if (me.getRole() == Role.AGENT) {
      l.setAssignedTo(new UUID[] {me.getId()});
    } else if (l.getAssignedTo() == null || l.getAssignedTo().length == 0) {
      Optional<UUID> automaticAssignee = assignments.selectIfEnabled();
      if (automaticAssignee.isPresent())
        l.setAssignedTo(new UUID[] {automaticAssignee.get()});
    }
    l.setConversionScore(conversionScores.calculate(l));
    l = leads.save(l);
    Deal deal = createProspectingDeal(l, me);
    activity.log(me.getId(), ActivityType.LEAD_STATUS_CHANGED, "LEAD", l.getId(), "Created lead");
    activity.log(
        me.getId(),
        ActivityType.LEAD_STAGE_CHANGED,
        "DEAL",
        deal.getId(),
        "Created prospecting deal for new lead");
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
    apply(l, r, current.get().getRole() != Role.AGENT);
    if (current.get().getRole() == Role.AGENT) l.setAssignedTo(original);
    l.setConversionScore(conversionScores.calculate(l));
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
    User me = current.get();
    Deal d = deals.findByLeadId(l.getId()).orElseGet(() -> createProspectingDeal(l, me));
    d.setTitle(
        req != null && req.title() != null
            ? req.title()
            : ((l.getCompanyName() != null ? l.getCompanyName() : l.getFirstName())
                + " opportunity"));
    if (d.getStage() == null) d.setStage(DealStage.PROSPECTING);
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
    l.setConversionScore(conversionScores.calculate(l));
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
      l.setAssignedTo(new UUID[] {assignments.selectAgent()});
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
    return assignments.get();
  }

  @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
  @Transactional
  public LeadAssignmentConfigResponse autoConfig(LeadAssignmentConfigRequest request) {
    return assignments.update(request);
  }

  private Lead find(UUID id) {
    return leads.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lead"));
  }

  private void checkOwn(Lead l) {
    User me = current.get();
    if (me.getRole() == Role.AGENT
        && (l.getAssignedTo() == null
            || Arrays.stream(l.getAssignedTo()).noneMatch(me.getId()::equals)))
      throw new ForbiddenException("Lead is not assigned to you");
  }

  private void apply(Lead l, LeadRequest r, boolean allowAssignments) {
    validateCommunicationPreferences(r);
    if (allowAssignments) {
      validateAssignees(r.getAssignedTo());
      l.setAssignedTo(r.getAssignedTo());
    }
    l.setFirstName(r.getFirstName());
    l.setLastName(r.getLastName());
    l.setEmail(r.getEmail());
    l.setPhone1(r.getPhone1());
    l.setPhone2(r.getPhone2());
    l.setWhatsapp(r.getWhatsapp());
    l.setCompanyName(r.getCompanyName());
    l.setRole(r.getRole());
    l.setAddress(r.getAddress());
    String category = canonicalCategory(r);
    l.setCategory(category);
    l.setIndustry(category);
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
  }

  private void validateAssignees(UUID[] ids) {
    if (ids == null || ids.length == 0) return;
    UUID companyId = current.get().getCompanyId();
    Set<UUID> requested = new LinkedHashSet<>(Arrays.asList(ids));
    if (requested.contains(null) || requested.size() != ids.length)
      throw new IllegalArgumentException("assignedTo must contain unique agent IDs");
    Map<UUID, User> found = new HashMap<>();
    users.findAllById(requested).forEach(user -> found.put(user.getId(), user));
    for (UUID id : requested) {
      User user = found.get(id);
      if (user == null) throw new ResourceNotFoundException("Assignee");
      if (user.getRole() != Role.AGENT
          || !user.isActive()
          || !Objects.equals(user.getCompanyId(), companyId))
        throw new IllegalArgumentException(
            "Every assignedTo entry must be an active agent in the current tenant");
    }
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

  private String canonicalCategory(LeadRequest request) {
    String category = normalize(request.getCategory());
    String industry = normalize(request.getIndustry());
    if (category != null && industry != null && !category.equals(industry))
      throw new IllegalArgumentException("category and the legacy industry alias must match");
    String value = category != null ? category : industry;
    if (value != null && !CATEGORIES.contains(value))
      throw new IllegalArgumentException("category must be one of: " + String.join(", ", CATEGORIES));
    return value;
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private Deal createProspectingDeal(Lead lead, User actor) {
    Deal deal = new Deal();
    deal.setCompanyId(lead.getCompanyId());
    deal.setLead(lead);
    deal.setCreatedBy(actor);
    deal.setTitle(
        (lead.getCompanyName() != null && !lead.getCompanyName().isBlank()
                ? lead.getCompanyName()
                : Optional.ofNullable(lead.getFirstName()).orElse("Lead"))
            + " opportunity");
    deal.setStage(DealStage.PROSPECTING);
    deal.setPriority(lead.getPriority());
    UUID[] assignees = lead.getAssignedTo();
    if (assignees != null && assignees.length > 0)
      deal.setAssignedTo(
          users
              .findById(assignees[0])
              .orElseThrow(() -> new ResourceNotFoundException("Assignee")));
    else deal.setAssignedTo(actor);
    return deals.save(deal);
  }
}
