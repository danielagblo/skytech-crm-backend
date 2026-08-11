package com.skytech.crm.repository;

import com.skytech.crm.entity.BroadcastMessage;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BroadcastRepository extends JpaRepository<BroadcastMessage, UUID> {
  org.springframework.data.domain.Page<BroadcastMessage> findByCompanyId(
      UUID companyId, org.springframework.data.domain.Pageable pageable);

  org.springframework.data.domain.Page<BroadcastMessage> findByCreatedAtAfter(
      OffsetDateTime since, org.springframework.data.domain.Pageable pageable);

  org.springframework.data.domain.Page<BroadcastMessage> findByCompanyIdAndCreatedAtAfter(
      UUID companyId, OffsetDateTime since, org.springframework.data.domain.Pageable pageable);

  List<BroadcastMessage> findByStatusAndScheduledAtLessThanEqual(
      com.skytech.crm.enums.BroadcastStatus status, OffsetDateTime now);
}
