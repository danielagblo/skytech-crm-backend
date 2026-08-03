package com.skytech.crm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.skytech.crm.entity.Invoice;
import com.skytech.crm.enums.InvoiceStatus;
import com.skytech.crm.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class InvoiceDeliveryServiceTest {
  @Test
  void recordsDeliveryFailureWithoutThrowingToTheCaller() {
    InvoiceRepository invoices = mock(InvoiceRepository.class);
    InvoicePdfService pdf = mock(InvoicePdfService.class);
    EmailService email = mock(EmailService.class);
    ActivityService activity = mock(ActivityService.class);
    UUID invoiceId = UUID.randomUUID();
    Invoice invoice = new Invoice();
    invoice.setId(invoiceId);
    invoice.setInvoiceNumber("INV-2026-000001");
    invoice.setStatus(InvoiceStatus.SENDING);
    invoice.setAmountPaid(BigDecimal.ZERO);
    invoice.setBalanceDue(BigDecimal.TEN);
    when(invoices.findById(invoiceId)).thenReturn(Optional.of(invoice));
    when(pdf.generate(invoice)).thenReturn(new byte[] {1, 2, 3});
    doThrow(new IllegalStateException("Email sender is not configured"))
        .when(email)
        .sendWithAttachment(anyString(), anyString(), anyString(), anyString(), any());

    new InvoiceDeliveryService(invoices, pdf, email, activity)
        .deliver(
            new InvoiceSendRequested(
                invoiceId, UUID.randomUUID(), "billing@example.com", "Invoice", "Attached"));

    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.SEND_FAILED);
    assertThat(invoice.getLastSendError()).contains("not configured");
    verify(invoices).save(invoice);
  }
}
