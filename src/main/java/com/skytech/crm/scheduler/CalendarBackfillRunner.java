package com.skytech.crm.scheduler;

import com.skytech.crm.entity.*;
import com.skytech.crm.repository.*;
import com.skytech.crm.service.CalendarSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Back-fills calendar_events for any dated records that were created before
 * calendar syncing existed, so the calendar reflects the whole CRM.
 */
@Slf4j
@Component
@ConditionalOnProperty(
    name = "app.calendar.backfill.enabled",
    havingValue = "true",
    matchIfMissing = true)
@RequiredArgsConstructor
public class CalendarBackfillRunner implements ApplicationRunner {
  private final TaskRepository tasks;
  private final DealLogRepository logs;
  private final BroadcastRepository broadcasts;
  private final AutomationRepository automations;
  private final InvoiceRepository invoices;
  private final DealRepository deals;
  private final CalendarSyncService calendar;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    int count = 0;
    for (Task task : tasks.findAll()) {
      calendar.syncTask(task);
      count++;
    }
    for (DealLog log : logs.findAll()) {
      calendar.syncDealLog(log);
      count++;
    }
    for (BroadcastMessage broadcast : broadcasts.findAll()) {
      calendar.syncBroadcast(broadcast);
      count++;
    }
    for (Automation automation : automations.findAll()) {
      calendar.syncAutomation(automation);
      count++;
    }
    for (Invoice invoice : invoices.findAll()) {
      calendar.syncInvoice(invoice);
      count++;
    }
    for (Deal deal : deals.findAll()) {
      calendar.syncDealRenewals(deal);
      count++;
    }
    log.info("Calendar backfill processed {} dated records", count);
  }
}