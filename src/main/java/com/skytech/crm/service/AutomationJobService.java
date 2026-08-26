package com.skytech.crm.service;

import com.skytech.crm.entity.*;
import com.skytech.crm.enums.*;
import com.skytech.crm.repository.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AutomationJobService {
  private static final int MAX_ATTEMPTS = 3;
  private final AutomationRepository automations;
  private final AutomationExecutionJobRepository jobs;
  private final AutomationExecutionService execution;

  @Transactional
  public void schedule(AutomationType type, Deal deal, OffsetDateTime triggerAt) {
    Lead lead = deal == null ? null : deal.getLead();
    if (lead == null) return;
    UUID companyId = deal.getCompanyId();
    for (Automation automation :
        automations.findTenantActiveByType(companyId, type)) {
      scheduleAutomation(automation, lead, deal, triggerAt);
    }
  }

  @Transactional
  public void cancelPaymentReminders(UUID dealId) {
    Set<AutomationType> reminderTypes =
        Set.of(AutomationType.PAYMENT_DUE, AutomationType.PAYMENT_OVERDUE, AutomationType.PAYMENT_RECOVERY);
    for (AutomationExecutionJob job : jobs.findByDealIdAndStatus(dealId, AutomationJobStatus.PENDING))
      if (reminderTypes.contains(job.getAutomation().getAutomationType()))
        job.setStatus(AutomationJobStatus.CANCELLED);
  }

  private void scheduleAutomation(Automation automation, Lead lead, Deal deal, OffsetDateTime triggerAt) {
    List<Map<String, Object>> steps = automation.getSteps();
    int count = steps == null || steps.isEmpty() ? 1 : steps.size();
    OffsetDateTime scheduled = triggerAt;
    for (int index = 0; index < count; index++) {
      if (steps != null && !steps.isEmpty())
        scheduled = scheduled.plusDays(waitDays(steps.get(index)));
      if (jobs.existsByAutomationIdAndLeadIdAndDealIdAndStepIndexAndScheduledAt(
          automation.getId(), lead.getId(), deal.getId(), index, scheduled)) continue;
      AutomationExecutionJob job = new AutomationExecutionJob();
      job.setCompanyId(automation.getCompanyId());
      job.setAutomation(automation);
      job.setLead(lead);
      job.setDeal(deal);
      job.setStepIndex(index);
      job.setScheduledAt(scheduled);
      jobs.save(job);
      if (automation.getNextRunAt() == null || scheduled.isBefore(automation.getNextRunAt()))
        automation.setNextRunAt(scheduled);
    }
    automation.setExecutionState("WAITING");
    automations.save(automation);
  }

  private long waitDays(Map<String, Object> step) {
    Object value = step.containsKey("wait_days") ? step.get("wait_days") : step.get("waitDays");
    if (value == null) return 0;
    try {
      long days = Long.parseLong(String.valueOf(value));
      if (days < 0) throw new IllegalArgumentException("wait_days cannot be negative");
      return days;
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException("wait_days must be a whole number");
    }
  }

  @Scheduled(
      fixedDelayString = "${automation.jobs.poll-ms:60000}",
      initialDelayString = "${automation.jobs.initial-delay-ms:60000}")
  @Transactional
  public void executeDueJobs() {
    List<AutomationExecutionJob> due =
        jobs.lockDue(AutomationJobStatus.PENDING, OffsetDateTime.now(), PageRequest.of(0, 50));
    for (AutomationExecutionJob job : due) process(job);
  }

  private void process(AutomationExecutionJob job) {
    job.setStatus(AutomationJobStatus.PROCESSING);
    job.setAttemptCount(job.getAttemptCount() + 1);
    jobs.save(job);
    Automation automation = job.getAutomation();
    try {
      boolean delivered = execution.executeStep(automation, job.getLead(), job.getStepIndex());
      job.setStatus(AutomationJobStatus.COMPLETED);
      job.setLastError(null);
      if (delivered) automation.setRecipientCount(automation.getRecipientCount() + 1);
      automation.setLastExecutedAt(OffsetDateTime.now());
    } catch (Exception exception) {
      String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
      job.setLastError(message.length() > 1000 ? message.substring(0, 1000) : message);
      if (job.getAttemptCount() >= MAX_ATTEMPTS) {
        job.setStatus(AutomationJobStatus.FAILED);
        automation.setExecutionState("FAILED");
        automation.setFailureReason(job.getLastError());
      } else {
        job.setStatus(AutomationJobStatus.PENDING);
        job.setScheduledAt(OffsetDateTime.now().plusMinutes(job.getAttemptCount() * 5L));
      }
    }
    jobs.save(job);
    Optional<AutomationExecutionJob> next =
        jobs.findFirstByAutomationIdAndStatusOrderByScheduledAtAsc(
            automation.getId(), AutomationJobStatus.PENDING);
    automation.setNextRunAt(next.map(AutomationExecutionJob::getScheduledAt).orElse(null));
    if (next.isEmpty() && !"FAILED".equals(automation.getExecutionState()))
      automation.setExecutionState("COMPLETED");
    automations.save(automation);
  }
}
