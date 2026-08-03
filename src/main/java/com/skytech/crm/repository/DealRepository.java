package com.skytech.crm.repository;

import com.skytech.crm.entity.Deal;
import com.skytech.crm.enums.DealStage;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface DealRepository extends JpaRepository<Deal, UUID>, JpaSpecificationExecutor<Deal> {
  List<Deal> findByStage(DealStage stage);

  List<Deal> findByAssignedToId(UUID id);

  @Query(
      "select d from Deal d where d.hostingExpiry <= :to or d.domainExpiry <= :to or"
          + " d.maintenanceExpiry <= :to")
  List<Deal> findExpiringBefore(LocalDate to);
}
