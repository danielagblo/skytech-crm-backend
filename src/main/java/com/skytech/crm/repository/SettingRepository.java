package com.skytech.crm.repository;

import com.skytech.crm.entity.Setting;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SettingRepository extends JpaRepository<Setting, UUID> {
  Optional<Setting> findByCompanyId(UUID companyId);

  @Query("select s from Setting s where (s.companyId = :companyId) or "
      + "(s.companyId is null and :companyId is null)")
  Optional<Setting> findTenant(@Param("companyId") UUID companyId);
}
