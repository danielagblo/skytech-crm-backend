package com.skytech.crm.controller;

import com.skytech.crm.dto.request.*;
import com.skytech.crm.dto.response.ApiResponse;
import com.skytech.crm.enums.InvoiceStatus;
import com.skytech.crm.service.InvoiceService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController extends BaseController {
  private final InvoiceService invoices;

  @GetMapping
  ApiResponse<?> list(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) InvoiceStatus status,
      @RequestParam(required = false) UUID dealId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return ok(invoices.list(search, status, dealId, pageable));
  }

  @PostMapping
  ApiResponse<?> create(@Valid @RequestBody CreateInvoiceRequest request) {
    return ok(invoices.create(request));
  }

  @GetMapping("/{id}")
  ApiResponse<?> get(@PathVariable UUID id) {
    return ok(invoices.get(id));
  }

  @PutMapping("/{id}")
  ApiResponse<?> update(
      @PathVariable UUID id, @Valid @RequestBody UpdateInvoiceRequest request) {
    return ok(invoices.update(id, request));
  }

  @DeleteMapping("/{id}")
  ApiResponse<Void> delete(@PathVariable UUID id) {
    invoices.delete(id);
    return done("Invoice deleted");
  }

  @PostMapping("/{id}/issue")
  ApiResponse<?> issue(@PathVariable UUID id) {
    return ok(invoices.issue(id));
  }

  @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
    byte[] content = invoices.pdf(id);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .contentLength(content.length)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-" + id + ".pdf")
        .body(content);
  }

  @PostMapping("/{id}/send")
  ApiResponse<?> send(@PathVariable UUID id, @Valid @RequestBody InvoiceSendRequest request) {
    return ok(invoices.send(id, request));
  }

  @PostMapping("/{id}/payments")
  ApiResponse<?> payment(
      @PathVariable UUID id, @Valid @RequestBody InvoicePaymentRequest request) {
    return ok(invoices.recordPayment(id, request));
  }

  @PostMapping("/{id}/void")
  ApiResponse<?> voidInvoice(@PathVariable UUID id) {
    return ok(invoices.voidInvoice(id));
  }
}
