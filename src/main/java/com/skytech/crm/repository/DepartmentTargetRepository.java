package com.skytech.crm.repository;

import com.skytech.crm.entity.DepartmentTarget;
import com.skytech.crm.enums.TargetMetric;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface DepartmentTargetRepository extends JpaRepository<DepartmentTarget, UUID> {
  List<DepartmentTarget> findByCompanyIdAndPeriod(UUID companyId, String period);

  Optional<DepartmentTarget> findByCompanyIdAndPeriodAndMetricType(
      UUID companyId, String period, TargetMetric metricType);
}