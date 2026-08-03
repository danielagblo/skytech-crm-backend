package com.skytech.crm.repository;

import com.skytech.crm.entity.InvoicePayment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, UUID> {}
