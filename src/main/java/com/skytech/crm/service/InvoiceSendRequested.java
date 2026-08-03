package com.skytech.crm.service;

import java.util.UUID;

public record InvoiceSendRequested(
    UUID invoiceId, UUID actorId, String email, String subject, String message) {}
