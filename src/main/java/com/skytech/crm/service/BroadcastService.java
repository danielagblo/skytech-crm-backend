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
  private final SmsService sms;
  private final EmailService email;
  private final ActivityService activity;
  private final CrmMapper mapper;
  private final CalendarSyncService calendar;

  @Transactional(readOnly = true)
  public Page<BroadcastResponse> list(Pageable p) {
    gates.require(current.get(), Feature.BULK_BROADCAST);
    return broadcasts.findByCompanyId(current.get().getCompanyId(), p).map(mapper::broadcast);
  }

  @Transactional
  public BroadcastResponse create(BroadcastRequest r) {
    gates.require(current.get(), Feature.BULK_BROADCAST);
    BroadcastMessage b = new BroadcastMessage();
    b.setCreatedBy(current.get());
    b.setCompanyId(current.get().getCompanyId());
    apply(b, r);
    b.setStatus(r.getScheduledAt() == null ? BroadcastStatus.DRAFT : BroadcastStatus.WAITING);
    broadcasts.save(b);
    calendar.syncBroadcast(b);
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
    calendar.syncBroadcast(b);
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
    calendar.syncBroadcast(b);
    broadcasts.delete(b);
    activity.log(
        current.id(), ActivityType.LEAD_STAGE_CHANGED, "AUTOMATION", id, "Deleted broadcast");
  }

  @Transactional
  public BroadcastResponse send(UUID id) {
    gates.require(current.get(), Feature.BULK_BROADCAST);
    BroadcastMessage b = dispatch(find(id), current.id());
    calendar.syncBroadcast(b);
    return mapper.broadcast(b);
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
        calendar.syncBroadcast(b);
      } catch (Exception exception) {
        b.setStatus(BroadcastStatus.FAILED);
        b.setFailureDetails(safeFailure(exception));
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
    calendar.syncBroadcast(b);
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
        .findByCompanyIdAndCreatedAtAfter(
            current.get().getCompanyId(), OffsetDateTime.now().minusDays(days), pageable)
        .map(mapper::broadcast);
  }

  @Transactional(readOnly = true)
  public ContactSegmentsResponse segments() {
    Map<DealStage, Long> byStage = new EnumMap<>(DealStage.class);
    for (DealStage s : DealStage.values())
      byStage.put(
          s,
          deals.findByStage(s).stream()
              .filter(deal -> Objects.equals(deal.getCompanyId(), current.get().getCompanyId()))
              .map(Deal::getLead)
              .filter(Objects::nonNull)
              .map(Lead::getId)
              .distinct()
              .count());
    long total =
        leads.findAll().stream()
            .filter(lead -> Objects.equals(lead.getCompanyId(), current.get().getCompanyId()))
            .count();
    return new ContactSegmentsResponse(total, byStage);
  }

  private BroadcastMessage find(UUID id) {
    BroadcastMessage broadcast =
        broadcasts.findById(id).orElseThrow(() -> new ResourceNotFoundException("Broadcast"));
    if (!Objects.equals(broadcast.getCompanyId(), current.get().getCompanyId()))
      throw new ResourceNotFoundException("Broadcast");
    return broadcast;
  }

  private BroadcastMessage dispatch(BroadcastMessage b, UUID actorId) {
    if (b.getStatus() == BroadcastStatus.SENT)
      throw new IllegalArgumentException("Broadcast has already been sent");
    List<Lead> recipients = recipients(b);
    int count = 0;
    List<String> failures = new ArrayList<>();
    for (Lead l : recipients) {
      if ("SMS".equals(b.getChannel())
          && l.isSmsOptIn()
          && l.getPhone1() != null
          && !l.getPhone1().isBlank()) {
        try {
          sms.send(l.getPhone1(), b.getMessageContent());
          count++;
        } catch (Exception exception) {
          failures.add("lead " + l.getId() + ": " + safeFailure(exception));
        }
      } else if ("EMAIL".equals(b.getChannel())
          && l.isEmailOptIn()
          && l.getEmail() != null
          && !l.getEmail().isBlank()) {
        try {
          email.send(l.getEmail(), b.getName(), b.getMessageContent());
          count++;
        } catch (Exception exception) {
          failures.add("lead " + l.getId() + ": " + safeFailure(exception));
        }
      }
    }
    b.setRecipientCount(count);
    b.setFailureDetails(failures.isEmpty() ? null : truncate(String.join("; ", failures), 2000));
    b.setStatus(failures.isEmpty() ? BroadcastStatus.SENT : BroadcastStatus.FAILED);
    b.setSentAt(failures.isEmpty() ? OffsetDateTime.now() : null);
    broadcasts.save(b);
    activity.log(
        actorId,
        ActivityType.LEAD_STAGE_CHANGED,
        "AUTOMATION",
        b.getId(),
        (failures.isEmpty() ? "Sent" : "Failed to fully send")
            + " broadcast; delivered to "
            + count
            + " opted-in recipients");
    return b;
  }

  private void apply(BroadcastMessage b, BroadcastRequest r) {
    if ("SMS".equals(r.getChannel()) && r.getMessageContent().length() > 160)
      throw new IllegalArgumentException("SMS broadcasts are limited to 160 characters");
    b.setName(r.getName());
    b.setMessageContent(r.getMessageContent());
    b.setChannel(r.getChannel());
    b.setSegmentFilter(r.getSegmentFilter() == null ? Map.of() : r.getSegmentFilter());
    b.setContactIds(r.getContactIds());
    validateExplicitContacts(r);
    b.setFailureDetails(null);
    b.setScheduledAt(r.getScheduledAt());
  }

  private void validateExplicitContacts(BroadcastRequest request) {
    Set<UUID> selected = new LinkedHashSet<>();
    if (request.getContactIds() != null) selected.addAll(Arrays.asList(request.getContactIds()));
    Map<String, Object> filter = request.getSegmentFilter();
    if (filter != null) {
      addLeadIds(selected, filter.get("leadIds"));
      addLeadIds(selected, filter.get("lead_ids"));
    }
    if (selected.isEmpty()) return;
    List<Lead> contacts = leads.findAllById(selected);
    if (contacts.size() != selected.size()) throw new ResourceNotFoundException("Broadcast contact");
    UUID companyId = current.get().getCompanyId();
    if (contacts.stream().anyMatch(lead -> !Objects.equals(lead.getCompanyId(), companyId)))
      throw new ForbiddenException("Broadcast contacts must belong to the current tenant");
  }

  private List<Lead> recipients(BroadcastMessage broadcast) {
    Map<String, Object> filter =
        broadcast.getSegmentFilter() == null ? Map.of() : broadcast.getSegmentFilter();
    List<Lead> candidates =
        leads.findAll().stream()
            .filter(lead -> Objects.equals(lead.getCompanyId(), broadcast.getCompanyId()))
            .toList();
    boolean all = Boolean.TRUE.equals(filter.get("all")) || "ALL".equals(filter.get("stage"));
    Set<UUID> targeted = new LinkedHashSet<>();
    if (broadcast.getContactIds() != null) targeted.addAll(Arrays.asList(broadcast.getContactIds()));
    addLeadIds(targeted, filter.get("leadIds"));
    addLeadIds(targeted, filter.get("lead_ids"));
    addStageLeadIds(targeted, filter.get("stages"), broadcast.getCompanyId());
    addStageLeadIds(targeted, filter.get("stage"), broadcast.getCompanyId());
    if (!all && !targeted.isEmpty())
      candidates = candidates.stream().filter(lead -> targeted.contains(lead.getId())).toList();
    Object status = filter.get("lead_status");
    if (status != null) {
      LeadStatus value;
      try {
        value = LeadStatus.valueOf(String.valueOf(status));
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Invalid lead status: " + status);
      }
      candidates = candidates.stream().filter(lead -> lead.getStatus() == value).toList();
    }
    Object priority = filter.get("priority");
    if (priority != null) {
      Priority value;
      try {
        value = Priority.valueOf(String.valueOf(priority));
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Invalid lead priority: " + priority);
      }
      candidates = candidates.stream().filter(lead -> lead.getPriority() == value).toList();
    }
    if (filter.get("source") != null)
      candidates =
          candidates.stream()
              .filter(
                  lead -> Objects.equals(lead.getLeadSource(), String.valueOf(filter.get("source"))))
              .toList();
    if (filter.get("category") != null)
      candidates =
          candidates.stream()
              .filter(
                  lead -> Objects.equals(lead.getCategory(), String.valueOf(filter.get("category"))))
              .toList();
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

  private void addStageLeadIds(Set<UUID> target, Object value, UUID companyId) {
    for (Object item : valuesOf(value)) {
      DealStage dealStage;
      try {
        dealStage = DealStage.valueOf(String.valueOf(item));
      } catch (IllegalArgumentException exception) {
        throw new IllegalArgumentException("Invalid broadcast deal stage: " + item);
      }
      deals.findByStage(dealStage).stream()
          .filter(deal -> Objects.equals(deal.getCompanyId(), companyId))
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

  private String safeFailure(Exception exception) {
    String message = exception.getMessage();
    return truncate(
        message == null || message.isBlank() ? exception.getClass().getSimpleName() : message, 500);
  }

  private String truncate(String value, int length) {
    return value.length() > length ? value.substring(0, length) : value;
  }
}
