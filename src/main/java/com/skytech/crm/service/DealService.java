package com.skytech.crm.service;

import com.skytech.crm.dto.request.DealRequest;
import com.skytech.crm.dto.response.DealResponse;
import com.skytech.crm.entity.*;
import com.skytech.crm.enums.*;
import com.skytech.crm.exception.*;
import com.skytech.crm.mapper.CrmMapper;
import com.skytech.crm.repository.*;
import jakarta.persistence.criteria.Predicate;
import java.math.*;
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
public class DealService {
  private final DealRepository deals;
  private final DealLogRepository dealLogs;
  private final LeadRepository leads;
  private final UserRepository users;
  private final CurrentUserService current;
  private final ActivityService activity;
  private final CrmMapper mapper;

  @Transactional(readOnly = true)
  public Page<DealResponse> list(
      String search, DealStage stage, UUID assignee, Priority priority, Pageable p) {
    User me = current.get();
    Specification<Deal> s =
        (r, q, b) -> {
          List<Predicate> x = new ArrayList<>();
          if (search != null && !search.isBlank()) {
            String value = "%" + search.toLowerCase() + "%";
            var lead = r.join("lead", jakarta.persistence.criteria.JoinType.LEFT);
            x.add(
                b.or(
                    b.like(b.lower(r.get("title")), value),
                    b.like(b.lower(lead.get("firstName")), value),
                    b.like(b.lower(lead.get("lastName")), value),
                    b.like(b.lower(lead.get("email")), value),
                    b.like(b.lower(lead.get("companyName")), value)));
          }
          if (stage != null) x.add(b.equal(r.get("stage"), stage));
          if (priority != null) x.add(b.equal(r.get("priority"), priority));
          UUID owner = me.getRole() == Role.AGENT ? me.getId() : assignee;
          if (owner != null) x.add(b.equal(r.get("assignedTo").get("id"), owner));
          return b.and(x.toArray(Predicate[]::new));
        };
    return deals.findAll(s, p).map(mapper::deal);
  }

  @Transactional
  public DealResponse create(DealRequest r) {
    User me = current.get();
    validateLeadAccess(r.getLeadId(), me);
    Deal d = new Deal();
    d.setCreatedBy(me);
    apply(d, r);
    if (me.getRole() == Role.AGENT) d.setAssignedTo(me);
    d = deals.save(d);
    activity.log(me.getId(), ActivityType.LEAD_STAGE_CHANGED, "DEAL", d.getId(), "Created deal");
    return mapper.deal(d);
  }

  @Transactional(readOnly = true)
  public DealResponse get(UUID id) {
    Deal d = find(id);
    checkOwn(d);
    return mapper.deal(d);
  }

  @Transactional
  public DealResponse update(UUID id, DealRequest r) {
    Deal d = find(id);
    checkOwn(d);
    if (r.getVersion() != null && !Objects.equals(r.getVersion(), d.getVersion()))
      throw new org.springframework.orm.ObjectOptimisticLockingFailureException(Deal.class, id);
    User originalAssignee = d.getAssignedTo();
    validateLeadAccess(r.getLeadId(), current.get());
    apply(d, r);
    if (current.get().getRole() == Role.AGENT) d.setAssignedTo(originalAssignee);
    recalculate(d);
    d = deals.save(d);
    activity.log(current.id(), ActivityType.LEAD_STAGE_CHANGED, "DEAL", id, "Updated deal");
    return mapper.deal(d);
  }

  @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
  @Transactional
  public void delete(UUID id) {
    Deal d = find(id);
    d.setDeletedAt(OffsetDateTime.now());
    deals.save(d);
    activity.log(current.id(), ActivityType.LEAD_STAGE_CHANGED, "DEAL", id, "Deleted deal");
  }

  @Transactional
  public DealResponse stage(UUID id, DealStage stage) {
    Deal d = find(id);
    checkOwn(d);
    DealStage old = d.getStage();
    if (old == stage) return mapper.deal(d);
    d.setStage(stage);
    d = deals.save(d);
    DealLog transition = new DealLog();
    transition.setDeal(d);
    transition.setCreatedBy(current.get());
    transition.setLogType(stage.name());
    transition.setBody("Stage changed from " + old + " to " + stage);
    transition.setAutoReviewScore(3);
    dealLogs.save(transition);
    activity.log(
        current.id(),
        ActivityType.LEAD_STAGE_CHANGED,
        "DEAL",
        id,
        "Moved deal from " + old + " to " + stage);
    return mapper.deal(d);
  }

  @Transactional(readOnly = true)
  public Map<DealStage, List<DealResponse>> pipeline() {
    Map<DealStage, List<DealResponse>> out = new EnumMap<>(DealStage.class);
    for (DealStage stage : DealStage.values())
      out.put(
          stage,
          deals.findByStage(stage).stream().filter(this::canView).map(mapper::deal).toList());
    return out;
  }

  private Deal find(UUID id) {
    return deals.findById(id).orElseThrow(() -> new ResourceNotFoundException("Deal"));
  }

  private boolean canView(Deal d) {
    User me = current.get();
    return me.getRole() != Role.AGENT
        || (d.getAssignedTo() != null && d.getAssignedTo().getId().equals(me.getId()));
  }

  private void checkOwn(Deal d) {
    if (!canView(d)) throw new ForbiddenException("Deal is not assigned to you");
  }

  private void apply(Deal d, DealRequest r) {
    d.setLead(
        r.getLeadId() == null
            ? null
            : leads
                .findById(r.getLeadId())
                .orElseThrow(() -> new ResourceNotFoundException("Lead")));
    d.setAssignedTo(
        r.getAssignedToId() == null
            ? null
            : users
                .findById(r.getAssignedToId())
                .orElseThrow(() -> new ResourceNotFoundException("User")));
    d.setTitle(r.getTitle());
    if (r.getStage() != null) d.setStage(r.getStage());
    d.setPriority(r.getPriority());
    d.setContractValue(r.getContractValue());
    if (r.getTotalPaid() != null) d.setTotalPaid(r.getTotalPaid());
    d.setHostingExpiry(r.getHostingExpiry());
    d.setDomainExpiry(r.getDomainExpiry());
    d.setMaintenanceExpiry(r.getMaintenanceExpiry());
    d.setHostingCost(r.getHostingCost());
    d.setDomainCost(r.getDomainCost());
    d.setMaintenanceCost(r.getMaintenanceCost());
    d.setNotes(r.getNotes());
    recalculate(d);
  }

  private void validateLeadAccess(UUID leadId, User actor) {
    if (leadId == null || actor.getRole() != Role.AGENT) return;
    Lead lead = leads.findById(leadId).orElseThrow(() -> new ResourceNotFoundException("Lead"));
    if (lead.getAssignedTo() == null
        || Arrays.stream(lead.getAssignedTo()).noneMatch(actor.getId()::equals))
      throw new ForbiddenException("Lead is not assigned to you");
  }

  private void recalculate(Deal d) {
    BigDecimal contract = Optional.ofNullable(d.getContractValue()).orElse(BigDecimal.ZERO),
        paid = Optional.ofNullable(d.getTotalPaid()).orElse(BigDecimal.ZERO);
    d.setArrears(contract.subtract(paid).max(BigDecimal.ZERO));
    d.setPaidInFull(contract.signum() > 0 && paid.compareTo(contract) >= 0);
  }
}
