package com.skytech.crm.repository;

import com.skytech.crm.entity.Automation;
import com.skytech.crm.enums.AutomationType;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutomationRepository extends JpaRepository<Automation, UUID> {
  List<Automation> findByAutomationTypeAndIsActiveTrue(AutomationType type);

  org.springframework.data.domain.Page<Automation> findByAutomationType(
      AutomationType type, org.springframework.data.domain.Pageable pageable);
}
