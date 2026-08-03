package com.skytech.crm.repository;

import com.skytech.crm.entity.Setting;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingRepository extends JpaRepository<Setting, UUID> {
  Optional<Setting> findByCompanyId(UUID companyId);
}
