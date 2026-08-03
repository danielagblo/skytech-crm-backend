package com.skytech.crm.repository;

import com.skytech.crm.entity.TaskComment;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskCommentRepository extends JpaRepository<TaskComment, UUID> {
  org.springframework.data.domain.Page<TaskComment> findByTaskId(
      UUID id, org.springframework.data.domain.Pageable pageable);
}
