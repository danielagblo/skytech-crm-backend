package com.skytech.crm.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.skytech.crm.entity.Task;
import com.skytech.crm.enums.*;
import com.skytech.crm.mapper.CrmMapper;
import com.skytech.crm.repository.*;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
  @Mock TaskRepository tasks;
  @Mock SubTaskRepository subs;
  @Mock TaskCommentRepository comments;
  @Mock UserRepository users;
  @Mock LeadRepository leads;
  @Mock DealRepository deals;
  @Mock CurrentUserService current;
  @Mock ActivityService activity;
  @Mock CrmMapper mapper;
  @InjectMocks TaskService service;

  @Test
  void overdueJobChangesOnlyNewlyOverdueTasksAndLogsThem() {
    Task todo = new Task();
    todo.setId(java.util.UUID.randomUUID());
    todo.setStatus(TaskStatus.TODO);
    Task already = new Task();
    already.setId(java.util.UUID.randomUUID());
    already.setStatus(TaskStatus.OVERDUE);
    when(tasks.findByDueDateBeforeAndStatusNot(any(OffsetDateTime.class), eq(TaskStatus.DONE)))
        .thenReturn(List.of(todo, already));

    assertThat(service.markOverdue()).isEqualTo(1);
    assertThat(todo.getStatus()).isEqualTo(TaskStatus.OVERDUE);
    verify(tasks).save(todo);
    verify(tasks, never()).save(already);
    verify(activity)
        .log(
            null,
            ActivityType.TASK_STATUS_CHANGED,
            "TASK",
            todo.getId(),
            "Task automatically marked overdue");
  }
}
