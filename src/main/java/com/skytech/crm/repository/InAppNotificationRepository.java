package com.skytech.crm.repository;

import com.skytech.crm.entity.InAppNotification;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, UUID> {
  Page<InAppNotification> findByUserId(UUID userId, Pageable pageable);
  long countByUserIdAndReadAtIsNull(UUID userId);
  Optional<InAppNotification> findByIdAndUserId(UUID id, UUID userId);
  boolean existsByUserIdAndDeduplicationKey(UUID userId, String deduplicationKey);

  @Modifying
  @Query("update InAppNotification n set n.readAt = :readAt where n.user.id = :userId and n.readAt is null")
  int markAllRead(@Param("userId") UUID userId, @Param("readAt") java.time.OffsetDateTime readAt);
}
