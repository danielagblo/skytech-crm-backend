package com.skytech.crm.repository;

import com.skytech.crm.entity.DealLogComment;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DealLogCommentRepository extends JpaRepository<DealLogComment, UUID> {
  org.springframework.data.domain.Page<DealLogComment> findByDealLogId(
      UUID id, org.springframework.data.domain.Pageable pageable);
}
