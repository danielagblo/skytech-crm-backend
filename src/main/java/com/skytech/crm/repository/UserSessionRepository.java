package com.skytech.crm.repository;

import com.skytech.crm.entity.UserSession;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
  @Query("select s from UserSession s where s.user.id = :userId and "
      + "((s.companyId = :companyId) or (s.companyId is null and :companyId is null)) "
      + "and s.endedAt is null order by s.startedAt desc")
  List<UserSession> findOpen(UUID userId, UUID companyId);

  @Query("select s from UserSession s where s.user.id = :userId and "
      + "((s.companyId = :companyId) or (s.companyId is null and :companyId is null))")
  List<UserSession> findTenantSessions(UUID companyId, UUID userId);
  List<UserSession> findByEndedAtIsNullAndLastActivityAtBefore(OffsetDateTime cutoff);
}
