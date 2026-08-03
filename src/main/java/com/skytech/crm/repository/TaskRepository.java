package com.skytech.crm.repository;

import com.skytech.crm.entity.Task;
import com.skytech.crm.enums.TaskStatus;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {
  long countByStatus(TaskStatus status);

  long countByStatusAndAssigneesId(TaskStatus status, UUID userId);

  long countByAssigneesId(UUID userId);

  List<Task> findByDueDateBeforeAndStatusNot(OffsetDateTime now, TaskStatus status);
}
