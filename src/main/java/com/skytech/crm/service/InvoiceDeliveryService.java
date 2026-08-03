package com.skytech.crm.service;

import com.skytech.crm.entity.Invoice;
import com.skytech.crm.enums.*;
import com.skytech.crm.repository.InvoiceRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceDeliveryService {
  private final InvoiceRepository invoices;
  private final InvoicePdfService pdf;
  private final EmailService email;
  private final ActivityService activity;

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void deliver(InvoiceSendRequested request) {
    Invoice invoice = invoices.findById(request.invoiceId()).orElse(null);
    if (invoice == null || invoice.getStatus() != InvoiceStatus.SENDING) return;
    try {
      byte[] document = pdf.generate(invoice);
      String fileName = invoice.getInvoiceNumber() + ".pdf";
      email.sendWithAttachment(
          request.email(), request.subject(), request.message(), fileName, document);
      invoice.setStatus(deliveredStatus(invoice));
      invoice.setSentAt(OffsetDateTime.now());
      invoice.setLastSendError(null);
      invoices.save(invoice);
      activity.log(
          request.actorId(),
          ActivityType.LEAD_STAGE_CHANGED,
          "DEAL",
          invoice.getId(),
          "Sent invoice " + invoice.getInvoiceNumber() + " to " + request.email());
    } catch (Exception exception) {
      log.warn("Invoice {} delivery failed: {}", invoice.getId(), exception.getMessage());
      invoice.setStatus(InvoiceStatus.SEND_FAILED);
      invoice.setLastSendError(safeError(exception));
      invoices.save(invoice);
      activity.log(
          request.actorId(),
          ActivityType.LEAD_STAGE_CHANGED,
          "DEAL",
          invoice.getId(),
          "Invoice delivery failed for " + invoice.getInvoiceNumber());
    }
  }

  private InvoiceStatus deliveredStatus(Invoice invoice) {
    if (invoice.getBalanceDue().signum() == 0) return InvoiceStatus.PAID;
    if (invoice.getAmountPaid().signum() > 0) return InvoiceStatus.PARTIALLY_PAID;
    return InvoiceStatus.SENT;
  }

  private String safeError(Exception exception) {
    String message = exception.getMessage();
    if (message == null || message.isBlank()) message = exception.getClass().getSimpleName();
    return message.length() > 500 ? message.substring(0, 500) : message;
  }
}
