package com.skytech.crm.repository;

import com.skytech.crm.entity.DepartmentTarget;
import com.skytech.crm.enums.TargetMetric;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DepartmentTargetRepository extends JpaRepository<DepartmentTarget, UUID> {
  List<DepartmentTarget> findByCompanyIdAndPeriod(UUID companyId, String period);

  Optional<DepartmentTarget> findByCompanyIdAndPeriodAndMetricType(
      UUID companyId, String period, TargetMetric metricType);

  @Modifying
  @Query(
      value =
          """
          INSERT INTO department_targets
            (id, company_id, period, metric_type, target_value, enabled, created_at, updated_at)
          VALUES
            (gen_random_uuid(), :companyId, :period, :metricType, :targetValue, :enabled, NOW(), NOW())
          ON CONFLICT (company_id, period, metric_type)
          DO UPDATE SET target_value = EXCLUDED.target_value,
                        enabled = EXCLUDED.enabled,
                        updated_at = NOW()
          """,
      nativeQuery = true)
  int upsert(
      @Param("companyId") UUID companyId,
      @Param("period") String period,
      @Param("metricType") String metricType,
      @Param("targetValue") java.math.BigDecimal targetValue,
      @Param("enabled") boolean enabled);
}
