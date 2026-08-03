package com.skytech.crm.service;

import com.skytech.crm.dto.request.CalendarEventRequest;
import com.skytech.crm.dto.response.CalendarEventResponse;
import com.skytech.crm.entity.*;
import com.skytech.crm.enums.ActivityType;
import com.skytech.crm.exception.*;
import com.skytech.crm.mapper.CrmMapper;
import com.skytech.crm.repository.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CalendarService {
  private final CalendarEventRepository events;
  private final LeadRepository leads;
  private final DealRepository deals;
  private final UserRepository users;
  private final CurrentUserService current;
  private final ActivityService activity;
  private final CrmMapper mapper;

  @Transactional(readOnly = true)
  public org.springframework.data.domain.Page<CalendarEventResponse> list(
      OffsetDateTime from, OffsetDateTime to, org.springframework.data.domain.Pageable pageable) {
    if (from == null) from = OffsetDateTime.now().minusMonths(1);
    if (to == null) to = OffsetDateTime.now().plusMonths(1);
    return events.findInRange(from, to, pageable).map(mapper::calendar);
  }

  @Transactional
  public CalendarEventResponse create(CalendarEventRequest r) {
    CalendarEvent e = new CalendarEvent();
    e.setOwner(current.get());
    apply(e, r);
    events.save(e);
    activity.log(
        current.id(),
        ActivityType.LEAD_STAGE_CHANGED,
        "SYSTEM",
        e.getId(),
        "Created calendar event");
    return mapper.calendar(e);
  }

  @Transactional(readOnly = true)
  public CalendarEventResponse get(UUID id) {
    return mapper.calendar(find(id));
  }

  @Transactional
  public CalendarEventResponse update(UUID id, CalendarEventRequest r) {
    CalendarEvent e = find(id);
    apply(e, r);
    events.save(e);
    activity.log(
        current.id(), ActivityType.LEAD_STAGE_CHANGED, "SYSTEM", id, "Updated calendar event");
    return mapper.calendar(e);
  }

  @Transactional
  public void delete(UUID id) {
    events.delete(find(id));
    activity.log(
        current.id(), ActivityType.LEAD_STAGE_CHANGED, "SYSTEM", id, "Deleted calendar event");
  }

  private CalendarEvent find(UUID id) {
    return events.findById(id).orElseThrow(() -> new ResourceNotFoundException("Calendar event"));
  }

  private void apply(CalendarEvent e, CalendarEventRequest r) {
    if (!r.getEndTime().isAfter(r.getStartTime()))
      throw new IllegalArgumentException("endTime must be after startTime");
    if (r.getAssignees() != null)
      for (UUID id : r.getAssignees())
        if (!users.existsById(id)) throw new ResourceNotFoundException("Assignee");
    e.setTitle(r.getTitle());
    e.setDescription(r.getDescription());
    e.setStartTime(r.getStartTime());
    e.setEndTime(r.getEndTime());
    e.setEventType(r.getEventType());
    e.setAssignees(r.getAssignees());
    if (r.getLinkedLeadId() != null)
      e.setLinkedLead(
          leads
              .findById(r.getLinkedLeadId())
              .orElseThrow(() -> new ResourceNotFoundException("Lead")));
    else e.setLinkedLead(null);
    if (r.getLinkedDealId() != null)
      e.setLinkedDeal(
          deals
              .findById(r.getLinkedDealId())
              .orElseThrow(() -> new ResourceNotFoundException("Deal")));
    else e.setLinkedDeal(null);
  }
}
