package com.skytech.crm.repository;

import com.skytech.crm.entity.Automation;
import com.skytech.crm.enums.AutomationType;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface AutomationRepository extends JpaRepository<Automation, UUID> {
  List<Automation> findByAutomationTypeAndIsActiveTrue(AutomationType type);

  List<Automation> findByCompanyIdAndAutomationTypeAndIsActiveTrue(UUID companyId, AutomationType type);

  @Query("select a from Automation a where ((a.companyId = :companyId) or "
      + "(a.companyId is null and :companyId is null)) and a.automationType = :type and a.isActive = true")
  List<Automation> findTenantActiveByType(UUID companyId, AutomationType type);

  @Query("select a from Automation a where (a.companyId = :companyId) or "
      + "(a.companyId is null and :companyId is null)")
  org.springframework.data.domain.Page<Automation> findTenant(
      UUID companyId, org.springframework.data.domain.Pageable pageable);

  @Query("select a from Automation a where ((a.companyId = :companyId) or "
      + "(a.companyId is null and :companyId is null)) and a.automationType = :type")
  org.springframework.data.domain.Page<Automation> findTenantByType(
      UUID companyId, AutomationType type, org.springframework.data.domain.Pageable pageable);

  org.springframework.data.domain.Page<Automation> findByAutomationType(
      AutomationType type, org.springframework.data.domain.Pageable pageable);
}
