package com.skytech.crm.repository;

import com.skytech.crm.entity.Invoice;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;

public interface InvoiceRepository
    extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {
  @Query(value = "select nextval('invoice_number_seq')", nativeQuery = true)
  long nextNumber();
}
