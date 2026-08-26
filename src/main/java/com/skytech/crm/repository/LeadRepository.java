package com.skytech.crm.repository;

import com.skytech.crm.entity.Lead;
import com.skytech.crm.enums.LeadStatus;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface LeadRepository extends JpaRepository<Lead, UUID>, JpaSpecificationExecutor<Lead> {
  List<Lead> findByBirthday(LocalDate date);

  @Query(
      value = "select * from leads l where l.deleted_at is null and :id = any(l.assigned_to)",
      nativeQuery = true)
  List<Lead> findAssigned(@Param("id") UUID id);

  @Query(
      value = "select count(*) from leads l where l.deleted_at is null "
          + "and ((l.company_id = :companyId) or (l.company_id is null and :companyId is null)) "
          + "and :userId = any(l.assigned_to)",
      nativeQuery = true)
  long countAssigned(@Param("companyId") UUID companyId, @Param("userId") UUID userId);

  long countByStatus(LeadStatus status);
}
