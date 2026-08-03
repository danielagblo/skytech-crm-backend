package com.skytech.crm.repository;

import com.skytech.crm.entity.DealLog;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface DealLogRepository extends JpaRepository<DealLog, UUID> {
  org.springframework.data.domain.Page<DealLog> findByDealId(
      UUID dealId, org.springframework.data.domain.Pageable pageable);

  List<DealLog> findByDealIdIn(Collection<UUID> dealIds);

  @Query(
      "select l from DealLog l where l.followUpAt between :from and :to or l.settlementFollowUp"
          + " between :from and :to")
  List<DealLog> findFollowUps(OffsetDateTime from, OffsetDateTime to);
}
