package com.skytech.crm.scheduler;

import com.skytech.crm.entity.*;
import com.skytech.crm.enums.*;
import com.skytech.crm.repository.*;
import com.skytech.crm.service.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutomationScheduler {
  private final LeadRepository leads;
  private final DealRepository deals;
  private final DealLogRepository logs;
  private final CalendarEventRepository events;
  private final AutomationRepository automations;
  private final ActivityRepository activityRepo;
  private final ActivityService activity;
  private final NotificationService notifications;
  private final AutomationExecutionService execution;
  private final BroadcastService broadcasts;
  private final TaskService tasks;

  @Value("${app.time-zone:Africa/Accra}")
  private String timeZone;

  @Scheduled(cron = "0 0 8 * * *", zone = "${app.time-zone:Africa/Accra}")
  @Transactional
  public void birthdays() {
    LocalDate today = today();
    List<Automation> flows =
        automations.findByAutomationTypeAndIsActiveTrue(AutomationType.BIRTHDAY);
    for (Lead l :
        leads.findAll().stream()
            .filter(
                x ->
                    x.getBirthday() != null
                        && MonthDay.from(x.getBirthday()).equals(MonthDay.from(today)))
            .toList())
      for (Automation a : flows)
        execution.execute(
            a, l, "Happy birthday " + Optional.ofNullable(l.getFirstName()).orElse(""));
  }

  @Scheduled(cron = "0 0 7 * * *", zone = "${app.time-zone:Africa/Accra}")
  @Transactional
  public void holidays() {
    String today = today().toString();
    for (Automation a :
        automations.findByAutomationTypeAndIsActiveTrue(AutomationType.PUBLIC_HOLIDAY)) {
      Object date = a.getTriggerConfig() == null ? null : a.getTriggerConfig().get("date");
      if (today.equals(String.valueOf(date)))
        for (Lead l : leads.findAll()) execution.execute(a, l, "Season's greetings from Skytech");
    }
  }

  @Scheduled(cron = "0 0 7 * * *", zone = "${app.time-zone:Africa/Accra}")
  @Transactional
  public void personalAutomations() {
    String today = today().toString();
    for (Automation a :
        automations.findByAutomationTypeAndIsActiveTrue(AutomationType.PERSONAL)) {
      Map<String, Object> triggerConfig = a.getTriggerConfig() == null ? Map.of() : a.getTriggerConfig();
      Object date = triggerConfig.get("date");
      if (!today.equals(String.valueOf(date))) continue;
      Object contactIdsValue = triggerConfig.get("contactIds");
      if (!(contactIdsValue instanceof Collection<?> contactIds) || contactIds.isEmpty()) continue;
      for (Object contactId : contactIds) {
        try {
          UUID leadId = UUID.fromString(String.valueOf(contactId));
          leads.findById(leadId).ifPresent(lead -> execution.execute(a, lead, a.getName()));
        } catch (IllegalArgumentException exception) {
          log.warn("Skipping personal automation {} because contact id {} is invalid", a.getId(), contactId);
        }
      }
    }
  }

  @Scheduled(cron = "0 0 * * * *", zone = "${app.time-zone:Africa/Accra}")
  @Transactional
  public void retention() {
    LocalDate today = today();
    for (Deal d : deals.findExpiringBefore(today.plusDays(30))) {
      notice(d, d.getHostingExpiry(), ActivityType.HOSTING_EXPIRY_NOTICE, "Hosting");
      notice(d, d.getDomainExpiry(), ActivityType.DOMAIN_EXPIRY_NOTICE, "Domain");
      notice(d, d.getMaintenanceExpiry(), ActivityType.MAINTENANCE_EXPIRY_NOTICE, "Maintenance");
    }
  }

  @Scheduled(cron = "0 */15 * * * *", zone = "${app.time-zone:Africa/Accra}")
  @Transactional
  public void followUps() {
    OffsetDateTime now = OffsetDateTime.now(), until = now.plusMinutes(15);
    for (DealLog l : logs.findFollowUps(now, until)) {
      User u = l.getDeal().getAssignedTo() != null ? l.getDeal().getAssignedTo() : l.getCreatedBy();
      if (u != null) notifyUser(u, "Deal follow-up due: " + l.getDeal().getTitle());
    }
    for (CalendarEvent e : events.findByStartTimeBetween(now, until)) {
      if (e.getOwner() != null) notifyUser(e.getOwner(), "Calendar reminder: " + e.getTitle());
    }
    broadcasts.sendDue();
  }

  @Scheduled(cron = "0 0 0 * * *", zone = "${app.time-zone:Africa/Accra}")
  @Transactional
  public void overdue() {
    int count = tasks.markOverdue();
    if (count > 0) log.info("Marked {} tasks overdue", count);
  }

  private void notice(Deal d, LocalDate expiry, ActivityType type, String label) {
    if (expiry == null) return;
    long days = ChronoUnit.DAYS.between(today(), expiry);
    if (!(days == 30 || days == 7 || days == 1 || days < 0)) return;
    if (activityRepo.existsByEventTypeAndEntityIdAndCreatedAtAfter(
        type, d.getId(), OffsetDateTime.now().minusHours(23))) return;
    activity.log(
        null,
        type,
        "DEAL",
        d.getId(),
        label
            + " expires "
            + (days < 0 ? Math.abs(days) + " days overdue" : "in " + days + " days"));
    User recipient = d.getAssignedTo() != null ? d.getAssignedTo() : d.getCreatedBy();
    if (recipient != null) notifyUser(recipient, label + " renewal notice for " + d.getTitle());
  }

  private void notifyUser(User u, String body) {
    if (u.getPhone() != null) notifications.sendSms(u.getPhone(), body);
    else if (u.getEmail() != null)
      notifications.sendEmail(u.getEmail(), "Skytech CRM reminder", body);
  }

  private LocalDate today() {
    return LocalDate.now(ZoneId.of(timeZone));
  }
}
