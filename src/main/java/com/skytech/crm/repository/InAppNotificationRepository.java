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

  @Modifying
  @Query(
      value =
          """
          INSERT INTO in_app_notifications
            (id, company_id, user_id, type, title, body, href, deduplication_key,
             created_at, updated_at)
          VALUES
            (gen_random_uuid(), :companyId, :userId, :type, :title, :body, :href,
             :deduplicationKey, NOW(), NOW())
          ON CONFLICT (user_id, deduplication_key) DO NOTHING
          """,
      nativeQuery = true)
  int insertIfAbsent(
      @Param("companyId") UUID companyId,
      @Param("userId") UUID userId,
      @Param("type") String type,
      @Param("title") String title,
      @Param("body") String body,
      @Param("href") String href,
      @Param("deduplicationKey") String deduplicationKey);
}
