package com.skytech.crm.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.skytech.crm.entity.*;
import com.skytech.crm.enums.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InvoicePdfServiceTest {
  @Test
  void generatesARealPdfDocument() {
    Invoice invoice = new Invoice();
    invoice.setInvoiceNumber("INV-2026-000001");
    invoice.setStatus(InvoiceStatus.ISSUED);
    invoice.setIssueDate(LocalDate.of(2026, 8, 3));
    invoice.setDueDate(LocalDate.of(2026, 8, 10));
    invoice.setRecipientName("Acme Limited");
    invoice.setRecipientEmail("billing@example.com");
    invoice.setCurrency("GHS");
    invoice.setSubtotal(new BigDecimal("100.00"));
    invoice.setTaxRate(new BigDecimal("15.00"));
    invoice.setTaxAmount(new BigDecimal("15.00"));
    invoice.setTotal(new BigDecimal("115.00"));
    invoice.setBalanceDue(new BigDecimal("115.00"));
    InvoiceItem item = new InvoiceItem();
    item.setInvoice(invoice);
    item.setDescription("CRM implementation");
    item.setQuantity(BigDecimal.ONE);
    item.setUnitPrice(new BigDecimal("100.00"));
    item.setAmount(new BigDecimal("100.00"));
    invoice.getItems().add(item);

    byte[] result = new InvoicePdfService().generate(invoice);

    assertThat(result).hasSizeGreaterThan(500);
    assertThat(new String(result, 0, 5, java.nio.charset.StandardCharsets.US_ASCII))
        .isEqualTo("%PDF-");
  }
}
