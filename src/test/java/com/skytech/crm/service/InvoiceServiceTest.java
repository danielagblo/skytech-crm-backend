package com.skytech.crm.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.skytech.crm.dto.request.*;
import com.skytech.crm.config.InvoiceConfig;
import com.skytech.crm.entity.*;
import com.skytech.crm.enums.*;
import com.skytech.crm.mapper.CrmMapper;
import com.skytech.crm.repository.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {
  @Mock InvoiceRepository invoices;
  @Mock InvoicePaymentRepository payments;
  @Mock DealRepository deals;
  @Mock DealLogRepository dealLogs;
  @Mock DealLogService dealLogService;
  @Mock CurrentUserService current;
  @Mock ActivityService activity;
  @Mock InvoicePdfService pdf;
  @Mock CrmMapper mapper;
  @Mock ApplicationEventPublisher events;
  @Mock InvoiceConfig config;
  @Mock CalendarSyncService calendar;
  @InjectMocks InvoiceService service;

  @Test
  void createsDraftWithServerCalculatedTotals() {
    UUID dealId = UUID.randomUUID();
    User admin = admin();
    Deal deal = deal(dealId, admin);
    when(current.get()).thenReturn(admin);
    when(deals.findById(dealId)).thenReturn(Optional.of(deal));
    when(invoices.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    InvoiceRequest request = request(dealId);
    request.setTaxRate(new BigDecimal("10"));
    request.setDiscountAmount(new BigDecimal("5"));
    request.setItems(
        List.of(
            new InvoiceRequest.InvoiceItemRequest(
                "CRM implementation", new BigDecimal("2"), new BigDecimal("100"),
                List.of())));

    service.create(request);

    ArgumentCaptor<Invoice> saved = ArgumentCaptor.forClass(Invoice.class);
    verify(invoices).save(saved.capture());
    Invoice invoice = saved.getValue();
    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
    assertThat(invoice.getSubtotal()).isEqualByComparingTo("200.00");
    assertThat(invoice.getTaxAmount()).isEqualByComparingTo("20.00");
    assertThat(invoice.getTotal()).isEqualByComparingTo("215.00");
    assertThat(invoice.getBalanceDue()).isEqualByComparingTo("215.00");
    assertThat(invoice.getItems()).singleElement().satisfies(item -> {
      assertThat(item.getAmount()).isEqualByComparingTo("200.00");
      assertThat(item.getInvoice()).isSameAs(invoice);
    });
  }

  @Test
  void issuingAssignsImmutableFormattedNumber() {
    UUID invoiceId = UUID.randomUUID(), dealId = UUID.randomUUID();
    User admin = admin();
    Invoice invoice = invoice(invoiceId, deal(dealId, admin));
    when(current.get()).thenReturn(admin);
    when(current.id()).thenReturn(admin.getId());
    when(invoices.findById(invoiceId)).thenReturn(Optional.of(invoice));
    when(deals.findById(dealId)).thenReturn(Optional.of(invoice.getDeal()));
    when(invoices.nextNumber()).thenReturn(42L);
    when(invoices.save(invoice)).thenReturn(invoice);

    service.issue(invoiceId);

    assertThat(invoice.getInvoiceNumber()).matches("INV-\\d{4}-000042");
    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
    assertThat(invoice.getIssueDate()).isNotNull();
    assertThat(invoice.getIssuedAt()).isNotNull();
  }

  @Test
  void invoicePaymentAlsoCreatesDealPaymentLog() {
    UUID invoiceId = UUID.randomUUID(), dealId = UUID.randomUUID();
    User admin = admin();
    Invoice invoice = invoice(invoiceId, deal(dealId, admin));
    invoice.setStatus(InvoiceStatus.ISSUED);
    invoice.setInvoiceNumber("INV-2026-000001");
    invoice.setTotal(new BigDecimal("100"));
    invoice.setBalanceDue(new BigDecimal("100"));
    when(current.get()).thenReturn(admin);
    when(current.id()).thenReturn(admin.getId());
    when(invoices.findById(invoiceId)).thenReturn(Optional.of(invoice));
    when(deals.findById(dealId)).thenReturn(Optional.of(invoice.getDeal()));
    when(invoices.save(invoice)).thenReturn(invoice);
    when(payments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    service.recordPayment(
        invoiceId,
        new InvoicePaymentRequest(
            new BigDecimal("40"), "BANK_TRANSFER", "TX-100", null));

    ArgumentCaptor<DealLogRequest> log = ArgumentCaptor.forClass(DealLogRequest.class);
    verify(dealLogService).create(eq(dealId), log.capture());
    assertThat(log.getValue().getLogType()).isEqualTo("PAYMENT");
    assertThat(log.getValue().getInvoiceNumber()).isEqualTo("INV-2026-000001");
    assertThat(invoice.getAmountPaid()).isEqualByComparingTo("40.00");
    assertThat(invoice.getBalanceDue()).isEqualByComparingTo("60.00");
    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
  }

  @Test
  void sendingQueuesAfterCommitDeliveryEvent() {
    UUID invoiceId = UUID.randomUUID(), dealId = UUID.randomUUID();
    User admin = admin();
    Invoice invoice = invoice(invoiceId, deal(dealId, admin));
    invoice.setStatus(InvoiceStatus.ISSUED);
    invoice.setInvoiceNumber("INV-2026-000001");
    invoice.setRecipientEmail("billing@example.com");
    when(current.get()).thenReturn(admin);
    when(current.id()).thenReturn(admin.getId());
    when(invoices.findById(invoiceId)).thenReturn(Optional.of(invoice));
    when(deals.findById(dealId)).thenReturn(Optional.of(invoice.getDeal()));
    when(invoices.save(invoice)).thenReturn(invoice);

    service.send(invoiceId, new InvoiceSendRequest(null, null, null));

    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.SENDING);
    ArgumentCaptor<InvoiceSendRequested> event =
        ArgumentCaptor.forClass(InvoiceSendRequested.class);
    verify(events).publishEvent(event.capture());
    assertThat(event.getValue().email()).isEqualTo("billing@example.com");
  }

  private User admin() {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setCompanyId(UUID.randomUUID());
    user.setRole(Role.ADMIN);
    return user;
  }

  private Deal deal(UUID id, User owner) {
    Deal deal = new Deal();
    deal.setId(id);
    deal.setTitle("CRM implementation");
    deal.setAssignedTo(owner);
    return deal;
  }

  private Invoice invoice(UUID id, Deal deal) {
    Invoice invoice = new Invoice();
    invoice.setId(id);
    invoice.setDeal(deal);
    invoice.setRecipientName("Acme Limited");
    return invoice;
  }

  private InvoiceRequest request(UUID dealId) {
    return new CreateInvoiceRequest()
        .setDealId(dealId)
        .setRecipientName("Acme Limited")
        .setCurrency("GHS")
        .setTaxRate(BigDecimal.ZERO)
        .setDiscountAmount(BigDecimal.ZERO)
        .setItems(
            List.of(
                new InvoiceRequest.InvoiceItemRequest(
                    "CRM implementation", BigDecimal.ONE, new BigDecimal("100"), List.of())));
  }
}
