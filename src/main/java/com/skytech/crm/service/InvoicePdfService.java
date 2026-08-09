package com.skytech.crm.service;

import com.skytech.crm.entity.*;
import java.io.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.stereotype.Service;

@Service
public class InvoicePdfService {
  private static final PDFont REGULAR =
      new PDType1Font(Standard14Fonts.FontName.HELVETICA);
  private static final PDFont BOLD =
      new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

  public byte[] generate(Invoice invoice) {
    try (PDDocument document = new PDDocument();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      PageWriter writer = new PageWriter(document);
      writer.newPage();
      writer.text(invoice.getIssuerName(), BOLD, 18);
      writer.optional(invoice.getIssuerAddress());
      writer.optional(invoice.getIssuerEmail());
      writer.optional(invoice.getIssuerPhone());
      if (invoice.getIssuerTaxId() != null && !invoice.getIssuerTaxId().isBlank())
        writer.text("Tax ID: " + invoice.getIssuerTaxId(), REGULAR, 9);
      writer.text("INVOICE", BOLD, 24);
      writer.space(8);
      writer.text("Invoice number: " + value(invoice.getInvoiceNumber()), BOLD, 11);
      writer.text("Issue date: " + date(invoice.getIssueDate()), REGULAR, 10);
      writer.text("Due date: " + date(invoice.getDueDate()), REGULAR, 10);
      writer.space(10);
      writer.text("Bill to", BOLD, 12);
      writer.text(invoice.getRecipientName(), REGULAR, 10);
      writer.optional(invoice.getRecipientCompany());
      writer.optional(invoice.getRecipientEmail());
      writer.optional(invoice.getRecipientAddress());
      writer.space(12);
      writer.tableHeader();
      for (InvoiceItem item : invoice.getItems()) writer.item(item, invoice.getCurrency());
      writer.space(8);
      writer.text("Subtotal: " + money(invoice.getCurrency(), invoice.getSubtotal()), REGULAR, 10);
      writer.text(
          "Tax (" + invoice.getTaxRate().stripTrailingZeros().toPlainString() + "%): "
              + money(invoice.getCurrency(), invoice.getTaxAmount()),
          REGULAR,
          10);
      writer.text(
          "Discount: " + money(invoice.getCurrency(), invoice.getDiscountAmount()), REGULAR, 10);
      writer.text("Total: " + money(invoice.getCurrency(), invoice.getTotal()), BOLD, 12);
      writer.text(
          "Amount paid: " + money(invoice.getCurrency(), invoice.getAmountPaid()), REGULAR, 10);
      writer.text(
          "Balance due: " + money(invoice.getCurrency(), invoice.getBalanceDue()), BOLD, 12);
      if (invoice.getNotes() != null && !invoice.getNotes().isBlank()) {
        writer.space(10);
        writer.text("Notes", BOLD, 11);
        writer.wrapped(invoice.getNotes());
      }
      if (invoice.getTerms() != null && !invoice.getTerms().isBlank()) {
        writer.space(8);
        writer.text("Terms", BOLD, 11);
        writer.wrapped(invoice.getTerms());
      }
      if (invoice.getPaymentInstructions() != null
          && !invoice.getPaymentInstructions().isBlank()) {
        writer.space(8);
        writer.text("Payment instructions", BOLD, 11);
        writer.wrapped(invoice.getPaymentInstructions());
      }
      writer.close();
      document.save(output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to generate invoice PDF", exception);
    }
  }

  private String date(java.time.LocalDate date) {
    return date == null ? "-" : date.format(DateTimeFormatter.ISO_LOCAL_DATE);
  }

  private String value(String value) {
    return value == null ? "DRAFT" : value;
  }

  private String money(String currency, BigDecimal amount) {
    return currency + " " + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
  }

  private final class PageWriter {
    private final PDDocument document;
    private PDPageContentStream stream;
    private float y;

    private PageWriter(PDDocument document) {
      this.document = document;
    }

    private void newPage() throws IOException {
      close();
      PDPage page = new PDPage(PDRectangle.A4);
      document.addPage(page);
      stream = new PDPageContentStream(document, page);
      y = 790;
    }

    private void ensure(float required) throws IOException {
      if (y - required < 55) newPage();
    }

    private void text(String text, PDFont font, float size) throws IOException {
      ensure(size + 6);
      stream.beginText();
      stream.setFont(font, size);
      stream.newLineAtOffset(55, y);
      stream.showText(safe(text));
      stream.endText();
      y -= size + 5;
    }

    private void optional(String text) throws IOException {
      if (text != null && !text.isBlank()) wrapped(text);
    }

    private void wrapped(String text) throws IOException {
      String normalized = text.replaceAll("[\\r\\n]+", " ").trim();
      while (normalized.length() > 90) {
        int split = normalized.lastIndexOf(' ', 90);
        if (split < 1) split = 90;
        text(normalized.substring(0, split), REGULAR, 9);
        normalized = normalized.substring(split).trim();
      }
      if (!normalized.isEmpty()) text(normalized, REGULAR, 9);
    }

    private void tableHeader() throws IOException {
      text("Description                              Qty       Unit price       Amount", BOLD, 9);
      text("--------------------------------------------------------------------------------", REGULAR, 8);
    }

    private void item(InvoiceItem item, String currency) throws IOException {
      if (y < 85) {
        newPage();
        tableHeader();
      }
      String description = item.getDescription();
      if (description.length() > 36) description = description.substring(0, 33) + "...";
      String line =
          String.format(
              "%-36s %8s %14s %14s",
              description,
              item.getQuantity().stripTrailingZeros().toPlainString(),
              money(currency, item.getUnitPrice()),
              money(currency, item.getAmount()));
      text(line, REGULAR, 8);
      for (String subLine : item.getSubLines()) {
        String sub = subLine;
        if (sub.length() > 60) sub = sub.substring(0, 57) + "...";
        text("    -- " + sub, REGULAR, 7);
      }
    }

    private void space(float amount) throws IOException {
      ensure(amount);
      y -= amount;
    }

    private String safe(String value) {
      String normalized = value == null
          ? ""
          : value
              .replace('\u2018', '\'')
              .replace('\u2019', '\'')
              .replace('\u201c', '"')
              .replace('\u201d', '"')
              .replace('\u2013', '-')
              .replace('\u2014', '-');
      return normalized.replaceAll("[^\\x20-\\x7E]", "?");
    }

    private void close() throws IOException {
      if (stream != null) {
        stream.close();
        stream = null;
      }
    }
  }
}
