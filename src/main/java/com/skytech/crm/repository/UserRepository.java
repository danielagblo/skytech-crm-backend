package com.skytech.crm.repository;

import com.skytech.crm.entity.User;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
  Optional<User> findByEmailIgnoreCase(String email);

  Optional<User> findByUsernameIgnoreCase(String username);

  boolean existsByEmailIgnoreCase(String email);

  @Query("select u from User u where ((u.companyId = :companyId) or "
      + "(u.companyId is null and :companyId is null)) and u.role = :role and u.isActive = true "
      + "order by u.createdAt asc, u.id asc")
  List<User> findAssignmentCandidates(@Param("companyId") UUID companyId, @Param("role") com.skytech.crm.enums.Role role);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from User u where ((u.companyId = :companyId) or "
      + "(u.companyId is null and :companyId is null)) and u.role = :role and u.isActive = true "
      + "order by u.createdAt asc, u.id asc")
  List<User> lockAssignmentCandidates(@Param("companyId") UUID companyId, @Param("role") com.skytech.crm.enums.Role role);
}
