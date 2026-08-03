package com.skytech.crm.repository;

import com.skytech.crm.entity.User;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
  Optional<User> findByEmailIgnoreCase(String email);

  Optional<User> findByUsernameIgnoreCase(String username);

  boolean existsByEmailIgnoreCase(String email);
}
