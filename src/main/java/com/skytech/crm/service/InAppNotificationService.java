package com.skytech.crm.service;

import com.skytech.crm.dto.response.InAppNotificationResponse;
import com.skytech.crm.entity.*;
import com.skytech.crm.enums.Role;
import com.skytech.crm.exception.ResourceNotFoundException;
import com.skytech.crm.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InAppNotificationService {
  private final InAppNotificationRepository notifications;
  private final UserRepository users;
  private final InvoiceRepository invoices;
  private final CurrentUserService current;

  @Transactional(readOnly = true)
  public Page<InAppNotificationResponse> list(Pageable pageable) {
    return notifications.findByUserId(current.id(), pageable).map(this::response);
  }

  @Transactional(readOnly = true)
  public long unreadCount() {
    return notifications.countByUserIdAndReadAtIsNull(current.id());
  }

  @Transactional
  public InAppNotificationResponse markRead(UUID id) {
    InAppNotification item = notifications.findByIdAndUserId(id, current.id())
        .orElseThrow(() -> new ResourceNotFoundException("Notification"));
    if (item.getReadAt() == null) item.setReadAt(OffsetDateTime.now());
    return response(notifications.save(item));
  }

  @Transactional
  public void markAllRead() {
    notifications.markAllRead(current.id(), OffsetDateTime.now());
  }

  @Transactional
  public void notifyPaymentLogged(Deal deal, DealLog log, User actor) {
    String amount = Optional.ofNullable(log.getAmountPaid()).orElse(BigDecimal.ZERO).toPlainString();
    users.findAll().stream()
        .filter(User::isActive)
        .filter(user -> user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER)
        .filter(user -> Objects.equals(user.getCompanyId(), actor.getCompanyId()))
        .filter(user -> !user.getId().equals(actor.getId()))
        .forEach(user -> create(
            user,
            "PAYMENT_LOGGED",
            "Payment logged",
            actor.fullName() + " logged GHS " + amount + " for " + deal.getTitle() + ".",
            "/pipeline/" + deal.getId(),
            "payment-log:" + log.getId()));
  }

  @Transactional
  public void announceTargets(String period, User actor) {
    users.findAll().stream()
        .filter(User::isActive)
        .filter(user -> Objects.equals(user.getCompanyId(), actor.getCompanyId()))
        .forEach(user -> create(
            user,
            "TARGET_SET",
            "New department targets",
            actor.fullName() + " published department targets for " + period + ".",
            "/settings/department-targets",
            "targets:" + period + ":" + user.getId()));
  }

  @Transactional
  public void notifyTaskReason(Task task, User actor, String reason) {
    users.findAll().stream()
        .filter(User::isActive)
        .filter(user -> user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER)
        .filter(user -> Objects.equals(user.getCompanyId(), actor.getCompanyId()))
        .filter(user -> !user.getId().equals(actor.getId()))
        .forEach(
            user ->
                create(
                    user,
                    "TASK_REASON_SUBMITTED",
                    "Task reason submitted",
                    actor.fullName() + " submitted a reason for " + task.getTitle() + ": " + reason,
                    "/tasks/" + task.getId(),
                    null));
  }

  @Scheduled(cron = "0 15 7 * * *", zone = "${app.time-zone:Africa/Accra}")
  @Transactional
  public void createUpcomingInvoiceReminders() {
    LocalDate today = LocalDate.now();
    LocalDate cutoff = today.plusDays(3);
    invoices.findAll().stream()
        .filter(invoice -> invoice.getDueDate() != null)
        .filter(invoice -> !invoice.getDueDate().isBefore(today) && !invoice.getDueDate().isAfter(cutoff))
        .filter(invoice -> invoice.getBalanceDue() != null && invoice.getBalanceDue().signum() > 0)
        .forEach(invoice -> {
          User recipient = invoice.getDeal().getAssignedTo() != null
              ? invoice.getDeal().getAssignedTo()
              : invoice.getCreatedBy();
          if (recipient != null)
            create(
                recipient,
                "INVOICE_DUE",
                "Invoice due soon",
                invoice.getInvoiceNumber() + " is due " + invoice.getDueDate()
                    + " with GHS " + invoice.getBalanceDue().toPlainString() + " outstanding.",
                "/settings/invoices",
                "invoice-due:" + invoice.getId() + ":" + invoice.getDueDate());
        });
  }

  private void create(User user, String type, String title, String body, String href, String key) {
    if (key != null && notifications.existsByUserIdAndDeduplicationKey(user.getId(), key)) return;
    InAppNotification item = new InAppNotification();
    item.setCompanyId(user.getCompanyId());
    item.setUser(user);
    item.setType(type);
    item.setTitle(title);
    item.setBody(body);
    item.setHref(href);
    item.setDeduplicationKey(key);
    notifications.save(item);
  }

  private InAppNotificationResponse response(InAppNotification item) {
    return new InAppNotificationResponse(
        item.getId(), item.getType(), item.getTitle(), item.getBody(), item.getHref(),
        item.getReadAt() != null, item.getCreatedAt());
  }
}
