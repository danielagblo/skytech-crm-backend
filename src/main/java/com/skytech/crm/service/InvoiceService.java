package com.skytech.crm.service;

import com.skytech.crm.dto.request.*;
import com.skytech.crm.dto.response.*;
import com.skytech.crm.config.InvoiceConfig;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceService {
  private final InvoiceRepository invoices;
  private final InvoicePaymentRepository payments;
  private final DealRepository deals;
  private final DealLogRepository dealLogs;
  private final DealLogService dealLogService;
  private final CurrentUserService current;
  private final ActivityService activity;
  private final InvoicePdfService pdf;
  private final CrmMapper mapper;
  private final ApplicationEventPublisher events;
  private final InvoiceConfig config;

  @Transactional(readOnly = true)
  public Page<InvoiceResponse> list(
      String search, InvoiceStatus status, UUID dealId, Pageable pageable) {
    User actor = current.get();
    Specification<Invoice> specification =
        (root, query, builder) -> {
          List<Predicate> predicates = new ArrayList<>();
          var deal = root.join("deal");
          if (search != null && !search.isBlank()) {
            String value = "%" + search.toLowerCase() + "%";
            predicates.add(
                builder.or(
                    builder.like(builder.lower(root.get("invoiceNumber")), value),
                    builder.like(builder.lower(root.get("recipientName")), value),
                    builder.like(builder.lower(root.get("recipientCompany")), value),
                    builder.like(builder.lower(root.get("recipientEmail")), value),
                    builder.like(builder.lower(deal.get("title")), value)));
          }
          if (status != null) predicates.add(builder.equal(root.get("status"), status));
          if (dealId != null) predicates.add(builder.equal(deal.get("id"), dealId));
          if (actor.getRole() == Role.AGENT)
            predicates.add(builder.equal(deal.get("assignedTo").get("id"), actor.getId()));
          return builder.and(predicates.toArray(Predicate[]::new));
        };
    return invoices.findAll(specification, pageable).map(mapper::invoice);
  }

  @Transactional
  public InvoiceResponse create(InvoiceRequest request) {
    User actor = current.get();
    Deal deal = accessibleDeal(request.getDealId(), actor);
    Invoice invoice = new Invoice();
    invoice.setDeal(deal);
    invoice.setCreatedBy(actor);
    invoice.setCompanyId(actor.getCompanyId());
    invoice.setIssuerName(
        config.issuerName() == null || config.issuerName().isBlank()
            ? "Skytech"
            : config.issuerName());
    invoice.setIssuerEmail(config.issuerEmail());
    invoice.setIssuerPhone(config.issuerPhone());
    invoice.setIssuerAddress(config.issuerAddress());
    invoice.setIssuerTaxId(config.issuerTaxId());
    invoice.setPaymentInstructions(config.paymentInstructions());
    apply(invoice, request, deal);
    invoice = invoices.save(invoice);
    activity.log(
        actor.getId(),
        ActivityType.LEAD_STAGE_CHANGED,
        "DEAL",
        invoice.getId(),
        "Created draft invoice for " + deal.getTitle());
    return mapper.invoice(invoice);
  }

  @Transactional(readOnly = true)
  public InvoiceResponse get(UUID id) {
    return mapper.invoice(accessible(id));
  }

  @Transactional
  public InvoiceResponse update(UUID id, InvoiceRequest request) {
    Invoice invoice = accessible(id);
    requireStatus(invoice, InvoiceStatus.DRAFT, "Only draft invoices can be edited");
    checkVersion(invoice, request.getVersion());
    Deal deal = accessibleDeal(request.getDealId(), current.get());
    invoice.setDeal(deal);
    apply(invoice, request, deal);
    invoice = invoices.save(invoice);
    activity.log(
        current.id(), ActivityType.LEAD_STAGE_CHANGED, "DEAL", id, "Updated draft invoice");
    return mapper.invoice(invoice);
  }

  @Transactional
  public InvoiceResponse issue(UUID id) {
    Invoice invoice = accessible(id);
    requireStatus(invoice, InvoiceStatus.DRAFT, "Only draft invoices can be issued");
    LocalDate today = LocalDate.now();
    if (invoice.getDueDate() == null) invoice.setDueDate(today.plusDays(14));
    if (invoice.getDueDate() != null && invoice.getDueDate().isBefore(today))
      throw new IllegalArgumentException("dueDate cannot be before the issue date");
    long sequence = invoices.nextNumber();
    invoice.setInvoiceNumber("INV-" + today.getYear() + "-" + String.format("%06d", sequence));
    invoice.setIssueDate(today);
    invoice.setIssuedAt(OffsetDateTime.now());
    invoice.setStatus(InvoiceStatus.ISSUED);
    invoice = invoices.save(invoice);
    activity.log(
        current.id(),
        ActivityType.LEAD_STAGE_CHANGED,
        "DEAL",
        id,
        "Issued invoice " + invoice.getInvoiceNumber());
    return mapper.invoice(invoice);
  }

  @Transactional(readOnly = true)
  public byte[] pdf(UUID id) {
    Invoice invoice = accessible(id);
    if (invoice.getStatus() == InvoiceStatus.DRAFT)
      throw new IllegalArgumentException("Issue the invoice before generating its PDF");
    return pdf.generate(invoice);
  }

  @Transactional
  public InvoiceResponse send(UUID id, InvoiceSendRequest request) {
    Invoice invoice = accessible(id);
    if (Set.of(InvoiceStatus.DRAFT, InvoiceStatus.VOID).contains(invoice.getStatus()))
      throw new IllegalArgumentException("Invoice cannot be sent in status " + invoice.getStatus());
    if (invoice.getStatus() == InvoiceStatus.SENDING
        && invoice.getSendRequestedAt() != null
        && invoice.getSendRequestedAt().isAfter(OffsetDateTime.now().minusMinutes(10)))
      throw new IllegalArgumentException("Invoice delivery is already in progress");
    String recipient =
        request.email() == null || request.email().isBlank()
            ? invoice.getRecipientEmail()
            : request.email();
    if (recipient == null || recipient.isBlank())
      throw new IllegalArgumentException("A recipient email is required");
    String subject =
        request.subject() == null || request.subject().isBlank()
            ? "Invoice " + invoice.getInvoiceNumber() + " from Skytech"
            : request.subject();
    String message =
        request.message() == null || request.message().isBlank()
            ? "Please find invoice " + invoice.getInvoiceNumber() + " attached."
            : request.message();
    invoice.setRecipientEmail(recipient);
    invoice.setStatus(InvoiceStatus.SENDING);
    invoice.setSendRequestedAt(OffsetDateTime.now());
    invoice.setLastSendError(null);
    invoice = invoices.save(invoice);
    events.publishEvent(new InvoiceSendRequested(id, current.id(), recipient, subject, message));
    activity.log(
        current.id(),
        ActivityType.LEAD_STAGE_CHANGED,
        "DEAL",
        id,
        "Queued invoice " + invoice.getInvoiceNumber() + " for delivery");
    return mapper.invoice(invoice);
  }

  @Transactional
  public InvoiceResponse recordPayment(UUID id, InvoicePaymentRequest request) {
    Invoice invoice = accessible(id);
    if (Set.of(InvoiceStatus.DRAFT, InvoiceStatus.VOID, InvoiceStatus.SENDING)
        .contains(invoice.getStatus()))
      throw new IllegalArgumentException("Invoice must be issued and active before recording payment");
    if (request.amount().compareTo(invoice.getBalanceDue()) > 0)
      throw new IllegalArgumentException("Payment cannot exceed the invoice balance");

    DealLogRequest logRequest =
        new CreateDealLogRequest()
            .setLogType("PAYMENT")
            .setAmountPaid(request.amount())
            .setPaymentMode(request.paymentMode())
            .setInvoiceNumber(invoice.getInvoiceNumber())
            .setReceiptNumber(request.reference())
            .setInvoiceIssued(true)
            .setBody("Payment recorded against invoice " + invoice.getInvoiceNumber());
    DealLogResponse log = dealLogService.create(invoice.getDeal().getId(), logRequest);

    InvoicePayment payment = new InvoicePayment();
    payment.setInvoice(invoice);
    payment.setCompanyId(invoice.getCompanyId());
    payment.setRecordedBy(current.get());
    payment.setAmount(request.amount());
    payment.setPaymentMode(request.paymentMode());
    payment.setReference(request.reference());
    payment.setPaidAt(request.paidAt() == null ? OffsetDateTime.now() : request.paidAt());
    if (log != null && log.id() != null) dealLogs.findById(log.id()).ifPresent(payment::setDealLog);
    payments.save(payment);
    invoice.getPayments().add(payment);
    invoice.setAmountPaid(invoice.getAmountPaid().add(request.amount()).setScale(2));
    invoice.setBalanceDue(invoice.getTotal().subtract(invoice.getAmountPaid()).max(BigDecimal.ZERO));
    if (invoice.getBalanceDue().signum() == 0) {
      invoice.setStatus(InvoiceStatus.PAID);
      invoice.setPaidAt(payment.getPaidAt());
    } else {
      invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
    }
    invoice = invoices.save(invoice);
    activity.log(
        current.id(),
        ActivityType.LEAD_LOG_CALL,
        "DEAL",
        id,
        "Recorded payment against invoice " + invoice.getInvoiceNumber());
    return mapper.invoice(invoice);
  }

  @Transactional
  public InvoiceResponse voidInvoice(UUID id) {
    Invoice invoice = accessible(id);
    if (invoice.getStatus() == InvoiceStatus.DRAFT)
      throw new IllegalArgumentException("Delete a draft invoice instead of voiding it");
    if (invoice.getStatus() == InvoiceStatus.VOID) return mapper.invoice(invoice);
    if (invoice.getAmountPaid().signum() > 0)
      throw new IllegalArgumentException("An invoice with recorded payments cannot be voided");
    invoice.setStatus(InvoiceStatus.VOID);
    invoice.setVoidedAt(OffsetDateTime.now());
    invoice = invoices.save(invoice);
    activity.log(
        current.id(),
        ActivityType.LEAD_STAGE_CHANGED,
        "DEAL",
        id,
        "Voided invoice " + invoice.getInvoiceNumber());
    return mapper.invoice(invoice);
  }

  @Transactional
  public void delete(UUID id) {
    Invoice invoice = accessible(id);
    requireStatus(invoice, InvoiceStatus.DRAFT, "Only draft invoices can be deleted");
    invoices.delete(invoice);
    activity.log(current.id(), ActivityType.LEAD_STAGE_CHANGED, "DEAL", id, "Deleted draft invoice");
  }

  private Invoice accessible(UUID id) {
    Invoice invoice =
        invoices.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice"));
    accessibleDeal(invoice.getDeal().getId(), current.get());
    return invoice;
  }

  private Deal accessibleDeal(UUID id, User actor) {
    Deal deal = deals.findById(id).orElseThrow(() -> new ResourceNotFoundException("Deal"));
    if (actor.getRole() == Role.AGENT
        && (deal.getAssignedTo() == null || !deal.getAssignedTo().getId().equals(actor.getId())))
      throw new ForbiddenException("Deal is not assigned to you");
    return deal;
  }

  private void apply(Invoice invoice, InvoiceRequest request, Deal deal) {
    Lead lead = deal.getLead();
    String recipientName = request.getRecipientName();
    if (recipientName == null || recipientName.isBlank()) recipientName = leadName(lead);
    if (recipientName == null || recipientName.isBlank())
      throw new IllegalArgumentException("recipientName is required when the deal has no named lead");
    invoice.setRecipientName(recipientName);
    invoice.setRecipientCompany(
        request.getRecipientCompany() != null
            ? request.getRecipientCompany()
            : lead == null ? null : lead.getCompanyName());
    invoice.setRecipientEmail(
        request.getRecipientEmail() != null
            ? request.getRecipientEmail()
            : lead == null ? null : lead.getEmail());
    invoice.setRecipientAddress(
        request.getRecipientAddress() != null
            ? request.getRecipientAddress()
            : lead == null ? null : lead.getAddress());
    invoice.setDueDate(request.getDueDate());
    invoice.setCurrency(request.getCurrency().toUpperCase(Locale.ROOT));
    invoice.setTaxRate(request.getTaxRate());
    invoice.setDiscountAmount(request.getDiscountAmount());
    invoice.setNotes(request.getNotes());
    invoice.setTerms(request.getTerms());
    invoice.getItems().clear();
    int position = 0;
    BigDecimal subtotal = BigDecimal.ZERO;
    for (InvoiceRequest.InvoiceItemRequest itemRequest : request.getItems()) {
      InvoiceItem item = new InvoiceItem();
      item.setInvoice(invoice);
      item.setCompanyId(invoice.getCompanyId());
      item.setDescription(itemRequest.description());
      item.setQuantity(itemRequest.quantity());
      item.setUnitPrice(itemRequest.unitPrice());
      item.setAmount(
          itemRequest
              .quantity()
              .multiply(itemRequest.unitPrice())
              .setScale(2, RoundingMode.HALF_UP));
      item.setPosition(position++);
      invoice.getItems().add(item);
      subtotal = subtotal.add(item.getAmount());
    }
    BigDecimal tax =
        subtotal
            .multiply(request.getTaxRate())
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    BigDecimal beforeDiscount = subtotal.add(tax);
    if (request.getDiscountAmount().compareTo(beforeDiscount) > 0)
      throw new IllegalArgumentException("discountAmount cannot exceed subtotal plus tax");
    invoice.setSubtotal(subtotal.setScale(2));
    invoice.setTaxAmount(tax);
    invoice.setTotal(beforeDiscount.subtract(request.getDiscountAmount()).setScale(2));
    invoice.setBalanceDue(invoice.getTotal().subtract(invoice.getAmountPaid()).max(BigDecimal.ZERO));
  }

  private String leadName(Lead lead) {
    if (lead == null) return null;
    String first = Optional.ofNullable(lead.getFirstName()).orElse("").trim();
    String last = Optional.ofNullable(lead.getLastName()).orElse("").trim();
    String name = (first + " " + last).trim();
    return name.isBlank() ? lead.getCompanyName() : name;
  }

  private void requireStatus(Invoice invoice, InvoiceStatus status, String message) {
    if (invoice.getStatus() != status) throw new IllegalArgumentException(message);
  }

  private void checkVersion(Invoice invoice, Long version) {
    if (version != null && !Objects.equals(version, invoice.getVersion()))
      throw new ObjectOptimisticLockingFailureException(Invoice.class, invoice.getId());
  }
}
