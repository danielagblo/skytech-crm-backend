package com.skytech.crm.repository;

import com.skytech.crm.entity.SubTask;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubTaskRepository extends JpaRepository<SubTask, UUID> {
  org.springframework.data.domain.Page<SubTask> findByTaskId(
      UUID id, org.springframework.data.domain.Pageable pageable);
}
