package com.skytech.crm.service;

import com.skytech.crm.dto.request.BroadcastRequest;
import com.skytech.crm.dto.response.*;
import com.skytech.crm.entity.*;
import com.skytech.crm.enums.*;
import com.skytech.crm.exception.*;
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
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class BroadcastService {
  private final BroadcastRepository broadcasts;
  private final LeadRepository leads;
  private final DealRepository deals;
  private final CurrentUserService current;
  private final FeatureGateService gates;
  private final NotificationService notifications;
  private final ActivityService activity;
  private final CrmMapper mapper;

  @Transactional(readOnly = true)
  public Page<BroadcastResponse> list(Pageable p) {
    gates.require(current.get(), Feature.BULK_BROADCAST);
    return broadcasts.findAll(p).map(mapper::broadcast);
  }

  @Transactional
  public BroadcastResponse create(BroadcastRequest r) {
    gates.require(current.get(), Feature.BULK_BROADCAST);
    BroadcastMessage b = new BroadcastMessage();
    b.setCreatedBy(current.get());
    apply(b, r);
    b.setStatus(r.getScheduledAt() == null ? BroadcastStatus.DRAFT : BroadcastStatus.WAITING);
    broadcasts.save(b);
    activity.log(
        current.id(),
        ActivityType.LEAD_STAGE_CHANGED,
        "AUTOMATION",
        b.getId(),
        "Created broadcast");
    return mapper.broadcast(b);
  }

  @Transactional(readOnly = true)
  public BroadcastResponse get(UUID id) {
    gates.require(current.get(), Feature.BULK_BROADCAST);
    return mapper.broadcast(find(id));
  }

  @Transactional
  public BroadcastResponse update(UUID id, BroadcastRequest r) {
    gates.require(current.get(), Feature.BULK_BROADCAST);
    BroadcastMessage b = find(id);
    if (b.getStatus() == BroadcastStatus.SENT)
      throw new IllegalArgumentException("Sent broadcasts cannot be edited");
    apply(b, r);
    b.setStatus(r.getScheduledAt() == null ? BroadcastStatus.DRAFT : BroadcastStatus.WAITING);
    broadcasts.save(b);
    activity.log(
        current.id(), ActivityType.LEAD_STAGE_CHANGED, "AUTOMATION", id, "Updated broadcast");
    return mapper.broadcast(b);
  }

  @Transactional
  public void delete(UUID id) {
    gates.require(current.get(), Feature.BULK_BROADCAST);
    BroadcastMessage b = find(id);
    if (b.getStatus() == BroadcastStatus.SENT)
      throw new IllegalArgumentException("Sent broadcasts cannot be deleted");
    broadcasts.delete(b);
    activity.log(
        current.id(), ActivityType.LEAD_STAGE_CHANGED, "AUTOMATION", id, "Deleted broadcast");
  }

  @Transactional
  public BroadcastResponse send(UUID id) {
    gates.require(current.get(), Feature.BULK_BROADCAST);
    return mapper.broadcast(dispatch(find(id), current.id()));
  }

  @PreAuthorize("permitAll()")
  @Transactional
  public int sendDue() {
    List<BroadcastMessage> due =
        broadcasts.findByStatusAndScheduledAtLessThanEqual(
            BroadcastStatus.WAITING, OffsetDateTime.now());
    for (BroadcastMessage b : due) {
      try {
        dispatch(b, null);
      } catch (Exception exception) {
        b.setStatus(BroadcastStatus.FAILED);
        broadcasts.save(b);
        activity.log(
            null,
            ActivityType.LEAD_STAGE_CHANGED,
            "AUTOMATION",
            b.getId(),
            "Scheduled broadcast failed: " + exception.getClass().getSimpleName());
      }
    }
    return due.size();
  }

  @Transactional
  public BroadcastResponse schedule(UUID id, OffsetDateTime when) {
    gates.require(current.get(), Feature.BULK_BROADCAST);
    if (when == null || !when.isAfter(OffsetDateTime.now()))
      throw new IllegalArgumentException("scheduledAt must be in the future");
    BroadcastMessage b = find(id);
    if (b.getStatus() == BroadcastStatus.SENT)
      throw new IllegalArgumentException("Sent broadcasts cannot be scheduled again");
    b.setScheduledAt(when);
    b.setStatus(BroadcastStatus.WAITING);
    broadcasts.save(b);
    activity.log(
        current.id(), ActivityType.LEAD_STAGE_CHANGED, "AUTOMATION", id, "Scheduled broadcast");
    return mapper.broadcast(b);
  }

  @Transactional(readOnly = true)
  public Page<BroadcastResponse> recent(int days, Pageable pageable) {
    gates.require(current.get(), Feature.BULK_BROADCAST);
    if (days < 1 || days > 365)
      throw new IllegalArgumentException("days must be between 1 and 365");
    return broadcasts
        .findByCreatedAtAfter(OffsetDateTime.now().minusDays(days), pageable)
        .map(mapper::broadcast);
  }

  @Transactional(readOnly = true)
  public ContactSegmentsResponse segments() {
    Map<DealStage, Long> byStage = new EnumMap<>(DealStage.class);
    for (DealStage s : DealStage.values())
      byStage.put(
          s,
          deals.findByStage(s).stream()
              .map(Deal::getLead)
              .filter(Objects::nonNull)
              .map(Lead::getId)
              .distinct()
              .count());
    return new ContactSegmentsResponse(leads.count(), byStage);
  }

  private BroadcastMessage find(UUID id) {
    return broadcasts.findById(id).orElseThrow(() -> new ResourceNotFoundException("Broadcast"));
  }

  private BroadcastMessage dispatch(BroadcastMessage b, UUID actorId) {
    if (b.getStatus() == BroadcastStatus.SENT)
      throw new IllegalArgumentException("Broadcast has already been sent");
    List<Lead> recipients = recipients(b.getSegmentFilter());
    int count = 0;
    for (Lead l : recipients) {
      if ("SMS".equals(b.getChannel())
          && l.isSmsOptIn()
          && l.getPhone1() != null
          && !l.getPhone1().isBlank()) {
        notifications.sendSms(l.getPhone1(), b.getMessageContent());
        count++;
      } else if ("EMAIL".equals(b.getChannel())
          && l.isEmailOptIn()
          && l.getEmail() != null
          && !l.getEmail().isBlank()) {
        notifications.sendEmail(l.getEmail(), b.getName(), b.getMessageContent());
        count++;
      }
    }
    b.setRecipientCount(count);
    b.setStatus(BroadcastStatus.SENT);
    b.setSentAt(OffsetDateTime.now());
    broadcasts.save(b);
    activity.log(
        actorId,
        ActivityType.LEAD_STAGE_CHANGED,
        "AUTOMATION",
        b.getId(),
        "Sent broadcast to " + count + " opted-in recipients");
    return b;
  }

  private void apply(BroadcastMessage b, BroadcastRequest r) {
    if ("SMS".equals(r.getChannel()) && r.getMessageContent().length() > 160)
      throw new IllegalArgumentException("SMS broadcasts are limited to 160 characters");
    b.setName(r.getName());
    b.setMessageContent(r.getMessageContent());
    b.setChannel(r.getChannel());
    b.setSegmentFilter(r.getSegmentFilter() == null ? Map.of() : r.getSegmentFilter());
    b.setScheduledAt(r.getScheduledAt());
  }

  private List<Lead> recipients(Map<String, Object> filter) {
    List<Lead> candidates = new ArrayList<>(leads.findAll());
    if (filter == null
        || filter.isEmpty()
        || Boolean.TRUE.equals(filter.get("all"))
        || "ALL".equals(filter.get("stage"))) return candidates;
    Set<UUID> targeted = new LinkedHashSet<>();
    addLeadIds(targeted, filter.get("leadIds"));
    addStageLeadIds(targeted, filter.get("stages"));
    addStageLeadIds(targeted, filter.get("stage"));
    if (!targeted.isEmpty()) candidates.removeIf(lead -> !targeted.contains(lead.getId()));
    Object status = filter.get("lead_status");
    if (status != null) {
      LeadStatus value;
      try {
        value = LeadStatus.valueOf(String.valueOf(status));
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Invalid lead status: " + status);
      }
      candidates.removeIf(lead -> lead.getStatus() != value);
    }
    Object priority = filter.get("priority");
    if (priority != null) {
      Priority value;
      try {
        value = Priority.valueOf(String.valueOf(priority));
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Invalid lead priority: " + priority);
      }
      candidates.removeIf(lead -> lead.getPriority() != value);
    }
    if (filter.get("source") != null)
      candidates.removeIf(
          lead -> !Objects.equals(lead.getLeadSource(), String.valueOf(filter.get("source"))));
    if (filter.get("category") != null)
      candidates.removeIf(
          lead -> !Objects.equals(lead.getCategory(), String.valueOf(filter.get("category"))));
    return candidates.stream().distinct().toList();
  }

  private void addLeadIds(Set<UUID> target, Object value) {
    for (Object item : valuesOf(value)) {
      try {
        target.add(UUID.fromString(String.valueOf(item)));
      } catch (IllegalArgumentException exception) {
        throw new IllegalArgumentException("Invalid broadcast lead id: " + item);
      }
    }
  }

  private void addStageLeadIds(Set<UUID> target, Object value) {
    for (Object item : valuesOf(value)) {
      DealStage dealStage;
      try {
        dealStage = DealStage.valueOf(String.valueOf(item));
      } catch (IllegalArgumentException exception) {
        throw new IllegalArgumentException("Invalid broadcast deal stage: " + item);
      }
      deals.findByStage(dealStage).stream()
          .map(Deal::getLead)
          .filter(Objects::nonNull)
          .map(Lead::getId)
          .forEach(target::add);
    }
  }

  private List<?> valuesOf(Object value) {
    if (value == null) return List.of();
    if (value instanceof Collection<?> collection) return new ArrayList<>(collection);
    if (value.getClass().isArray()) return Arrays.asList((Object[]) value);
    return List.of(value);
  }
}
