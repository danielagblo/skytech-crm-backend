package com.skytech.crm.service;

import com.skytech.crm.entity.*;
import com.skytech.crm.enums.*;
import com.skytech.crm.repository.CalendarEventRepository;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps calendar_events in sync with every dated record in the CRM: task due
 * dates, negotiation/settlement follow-ups, broadcast schedules, date-based
 * automations, invoice due dates and deal renewals (hosting/domain/maintenance).
 *
 * <p>Each generated event carries its source marker at the end of the
 * description (e.g. "\n[DEAL_LOG=<id>]") so edits and deletions only remove the
 * events they own.
 */
@Service
@RequiredArgsConstructor
public class CalendarSyncService {
  private static final ZoneId ZONE = ZoneId.of("Africa/Accra");

  private static final String TASK_TAG = "TASK";
  private static final String LOG_TAG = "DEAL_LOG";
  private static final String BROADCAST_TAG = "BROADCAST";
  private static final String AUTOMATION_TAG = "AUTOMATION";
  private static final String INVOICE_TAG = "INVOICE";
  private static final String DEAL_TAG = "DEAL";

  private final CalendarEventRepository events;

  @Transactional
  public void syncTask(Task task) {
    removeFor(TASK_TAG, task.getId());
    if (task.getDueDate() == null) return;
    CalendarEvent event = new CalendarEvent();
    event.setOwner(task.getCreatedBy());
    event.setTitle(task.getTitle());
    event.setDescription(description(task.getDescription(), TASK_TAG, task.getId()));
    event.setStartTime(task.getDueDate());
    event.setEndTime(task.getDueDate().plusMinutes(30));
    event.setEventType("TASK_DUE");
    event.setLinkedLead(task.getLinkedLead());
    event.setLinkedDeal(task.getLinkedDeal());
    event.setAssignees(
        task.getAssignees().stream().map(User::getId).toArray(UUID[]::new));
    events.save(event);
  }

  @Transactional
  public void syncDealLog(DealLog log) {
    removeFor(LOG_TAG, log.getId());
    Deal deal = log.getDeal();
    if (deal == null) return;
    User owner = deal.getAssignedTo() != null ? deal.getAssignedTo() : log.getCreatedBy();
    addFollowUp(
        LOG_TAG,
        log.getId(),
        deal,
        owner,
        "Negotiation follow-up · " + deal.getTitle(),
        log.getFollowUpAt());
    addFollowUp(
        LOG_TAG,
        log.getId(),
        deal,
        owner,
        "Settlement follow-up · " + deal.getTitle(),
        log.getSettlementFollowUp());
  }

  @Transactional
  public void syncBroadcast(BroadcastMessage broadcast) {
    removeFor(BROADCAST_TAG, broadcast.getId());
    if (broadcast.getScheduledAt() == null) return;
    if (broadcast.getStatus() == BroadcastStatus.SENT) return;
    CalendarEvent event = new CalendarEvent();
    event.setOwner(broadcast.getCreatedBy());
    event.setTitle("Broadcast: " + broadcast.getName());
    event.setDescription(
        "Scheduled "
            + broadcast.getChannel()
            + " broadcast. ["
            + BROADCAST_TAG
            + "="
            + broadcast.getId()
            + "]");
    event.setStartTime(broadcast.getScheduledAt());
    event.setEndTime(broadcast.getScheduledAt().plusMinutes(30));
    event.setEventType("REMINDER");
    event.setAssignees(ownerAssignees(broadcast.getCreatedBy()));
    events.save(event);
  }

  @Transactional
  public void syncAutomation(Automation automation) {
    removeFor(AUTOMATION_TAG, automation.getId());
    if (!isActive(automation)) return;
    Object date =
        automation.getTriggerConfig() == null
            ? null
            : automation.getTriggerConfig().get("date");
    if (date == null) return;
    LocalDate when;
    try {
      when = LocalDate.parse(String.valueOf(date));
    } catch (DateTimeParseException exception) {
      return;
    }
    OffsetDateTime start = when.atStartOfDay(ZONE).toOffsetDateTime();
    CalendarEvent event = new CalendarEvent();
    event.setOwner(automation.getCreatedBy());
    event.setTitle("Automation: " + automation.getName());
    event.setDescription(
        "Scheduled "
            + automation.getAutomationType()
            + " automation. ["
            + AUTOMATION_TAG
            + "="
            + automation.getId()
            + "]");
    event.setStartTime(start);
    event.setEndTime(start.plusMinutes(30));
    event.setEventType("REMINDER");
    event.setAssignees(ownerAssignees(automation.getCreatedBy()));
    events.save(event);
  }

  @Transactional
  public void syncInvoice(Invoice invoice) {
    removeFor(INVOICE_TAG, invoice.getId());
    if (invoice.getDueDate() == null) return;
    boolean activeStatus =
        Set.of(
                InvoiceStatus.ISSUED,
                InvoiceStatus.SENDING,
                InvoiceStatus.SENT,
                InvoiceStatus.SEND_FAILED,
                InvoiceStatus.PARTIALLY_PAID)
            .contains(invoice.getStatus());
    if (!activeStatus) return;
    Deal deal = invoice.getDeal();
    User owner =
        deal != null && deal.getAssignedTo() != null ? deal.getAssignedTo() : invoice.getCreatedBy();
    OffsetDateTime start = invoice.getDueDate().atStartOfDay(ZONE).toOffsetDateTime();
    CalendarEvent event = new CalendarEvent();
    event.setOwner(owner);
    event.setTitle(
        "Invoice payment due: "
            + (invoice.getInvoiceNumber() == null ? "Unnumbered" : invoice.getInvoiceNumber()));
    event.setDescription(
        "Payment due for "
            + invoice.getRecipientName()
            + " on "
            + invoice.getDueDate()
            + ". ["
            + INVOICE_TAG
            + "="
            + invoice.getId()
            + "]");
    event.setStartTime(start);
    event.setEndTime(start.plusHours(1));
    event.setEventType("PAYMENT_DUE");
    event.setLinkedDeal(deal);
    event.setLinkedLead(deal == null ? null : deal.getLead());
    event.setAssignees(ownerAssignees(owner));
    events.save(event);
  }

  @Transactional
  public void syncDealRenewals(Deal deal) {
    removeFor(DEAL_TAG, deal.getId());
    if (deal.getDeletedAt() != null) return;
    renewal(deal, "hosting", deal.getHostingExpiry());
    renewal(deal, "domain", deal.getDomainExpiry());
    renewal(deal, "maintenance", deal.getMaintenanceExpiry());
  }

  private void renewal(Deal deal, String service, LocalDate expiry) {
    if (expiry == null) return;
    User owner = deal.getAssignedTo() != null ? deal.getAssignedTo() : deal.getCreatedBy();
    OffsetDateTime start = expiry.atStartOfDay(ZONE).toOffsetDateTime();
    CalendarEvent event = new CalendarEvent();
    event.setOwner(owner);
    event.setTitle("Renewal: " + service + " expires");
    event.setDescription(
        deal.getTitle()
            + " — "
            + service
            + " expires on "
            + expiry
            + ". ["
            + DEAL_TAG
            + "="
            + deal.getId()
            + "]");
    event.setStartTime(start);
    event.setEndTime(start.plusHours(1));
    event.setEventType("REMINDER");
    event.setLinkedDeal(deal);
    event.setLinkedLead(deal.getLead());
    event.setAssignees(ownerAssignees(owner));
    events.save(event);
  }

  private void addFollowUp(
      String tag,
      UUID logId,
      Deal deal,
      User owner,
      String title,
      OffsetDateTime when) {
    if (when == null) return;
    CalendarEvent event = new CalendarEvent();
    event.setOwner(owner);
    event.setTitle(title);
    event.setDescription("[" + tag + "=" + logId + "]");
    event.setStartTime(when);
    event.setEndTime(when.plusMinutes(30));
    event.setEventType("CALL_LOG_FOLLOWUP");
    event.setLinkedDeal(deal);
    event.setLinkedLead(deal.getLead());
    event.setAssignees(ownerAssignees(owner));
    events.save(event);
  }

  private void removeFor(String tag, UUID sourceId) {
    String marker = "[" + tag + "=" + sourceId + "]";
    events.findAll().stream()
        .filter(event -> event.getDescription() != null)
        .filter(event -> event.getDescription().endsWith(marker))
        .forEach(events::delete);
  }

  private String description(String base, String tag, UUID id) {
    String text = base == null ? "" : base;
    return text + "\n[" + tag + "=" + id + "]";
  }

  private UUID[] ownerAssignees(User owner) {
    return owner == null ? new UUID[0] : new UUID[] {owner.getId()};
  }

  private boolean isActive(Automation automation) {
    return Boolean.TRUE.equals(Optional.ofNullable(automation).map(Automation::isActive).orElse(false));
  }
}