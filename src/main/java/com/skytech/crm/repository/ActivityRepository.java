package com.skytech.crm.repository;

import com.skytech.crm.entity.Activity;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface ActivityRepository
    extends JpaRepository<Activity, UUID>, JpaSpecificationExecutor<Activity> {
  Page<Activity> findByCreatedAtAfter(OffsetDateTime since, Pageable pageable);

  boolean existsByEventTypeAndEntityIdAndCreatedAtAfter(
      com.skytech.crm.enums.ActivityType type, UUID entityId, OffsetDateTime after);
}
