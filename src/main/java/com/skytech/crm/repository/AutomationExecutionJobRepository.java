package com.skytech.crm.repository;

import com.skytech.crm.entity.AutomationExecutionJob;
import com.skytech.crm.enums.AutomationJobStatus;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

public interface AutomationExecutionJobRepository extends JpaRepository<AutomationExecutionJob, UUID> {
  boolean existsByAutomationIdAndLeadIdAndDealIdAndStepIndexAndScheduledAt(
      UUID automationId, UUID leadId, UUID dealId, int stepIndex, OffsetDateTime scheduledAt);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select j from AutomationExecutionJob j where j.status = :status and j.scheduledAt <= :now order by j.scheduledAt asc")
  List<AutomationExecutionJob> lockDue(AutomationJobStatus status, OffsetDateTime now, Pageable pageable);

  List<AutomationExecutionJob> findByDealIdAndStatus(UUID dealId, AutomationJobStatus status);

  Optional<AutomationExecutionJob> findFirstByAutomationIdAndStatusOrderByScheduledAtAsc(
      UUID automationId, AutomationJobStatus status);
}
