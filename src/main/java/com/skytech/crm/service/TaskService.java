package com.skytech.crm.service;

import com.skytech.crm.dto.request.*;
import com.skytech.crm.dto.response.*;
import com.skytech.crm.entity.*;
import com.skytech.crm.enums.*;
import com.skytech.crm.exception.*;
import com.skytech.crm.mapper.CrmMapper;
import com.skytech.crm.repository.*;
import jakarta.persistence.criteria.Predicate;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {
  private final TaskRepository tasks;
  private final SubTaskRepository subs;
  private final TaskCommentRepository comments;
  private final UserRepository users;
  private final LeadRepository leads;
  private final DealRepository deals;
  private final CalendarEventRepository calendarEvents;
  private final CurrentUserService current;
  private final ActivityService activity;
  private final CrmMapper mapper;
  private static final String TASK_EVENT_TYPE = "TASK_DUE";
  private static final String TASK_EVENT_MARKER_PREFIX = "\n[TASK_ID=";
  private static final String TASK_EVENT_MARKER_SUFFIX = "]";

  @Transactional(readOnly = true)
  public Page<TaskResponse> list(
      String search,
      TaskStatus status,
      UUID assignee,
      Priority priority,
      Boolean overdue,
      Pageable p) {
    Specification<Task> s =
        (r, q, b) -> {
          List<Predicate> x = new ArrayList<>();
          if (search != null && !search.isBlank())
            x.add(b.like(b.lower(r.get("title")), "%" + search.toLowerCase() + "%"));
          if (status != null) x.add(b.equal(r.get("status"), status));
          if (priority != null) x.add(b.equal(r.get("priority"), priority));
          if (Boolean.TRUE.equals(overdue))
            x.add(
                b.and(
                    b.lessThan(r.get("dueDate"), OffsetDateTime.now()),
                    b.notEqual(r.get("status"), TaskStatus.DONE)));
          UUID owner = current.get().getRole() == Role.AGENT ? current.id() : assignee;
          if (owner != null) x.add(b.equal(r.join("assignees").get("id"), owner));
          return b.and(x.toArray(Predicate[]::new));
        };
    return tasks.findAll(s, p).map(mapper::task);
  }

  @Transactional
  public TaskResponse create(TaskRequest r) {
    Task t = new Task();
    User actor = current.get();
    t.setCreatedBy(actor);
    apply(t, r);
    if (actor.getRole() == Role.AGENT) t.setAssignees(new HashSet<>(Set.of(actor)));
    t = tasks.save(t);
    syncTaskCalendarEvent(t);
    activity.log(current.id(), ActivityType.TASK_STATUS_CHANGED, "TASK", t.getId(), "Created task");
    return mapper.task(t);
  }

  @Transactional(readOnly = true)
  public TaskResponse get(UUID id) {
    return mapper.task(find(id));
  }

  @Transactional
  public TaskResponse update(UUID id, TaskRequest r) {
    Task t = find(id);
    if (r.getVersion() != null && !Objects.equals(r.getVersion(), t.getVersion()))
      throw new org.springframework.orm.ObjectOptimisticLockingFailureException(Task.class, id);
    Set<User> originalAssignees = new HashSet<>(t.getAssignees());
    apply(t, r);
    if (current.get().getRole() == Role.AGENT) t.setAssignees(originalAssignees);
    t = tasks.save(t);
    syncTaskCalendarEvent(t);
    activity.log(current.id(), ActivityType.TASK_STATUS_CHANGED, "TASK", id, "Updated task");
    return mapper.task(t);
  }

  @Transactional
  public void delete(UUID id) {
    Task task = find(id);
    deleteTaskCalendarEvent(task.getId());
    tasks.delete(task);
    activity.log(current.id(), ActivityType.TASK_STATUS_CHANGED, "TASK", id, "Deleted task");
  }

  @Transactional
  public TaskResponse status(UUID id, TaskStatus status, String reason) {
    Task t = find(id);
    t.setStatus(status);
    if (reason != null && !reason.isBlank()) t.setCompletionReason(reason.trim());
    if (status == TaskStatus.DONE && t.getCompletionReason() == null)
      t.setCompletionReason("Completed");
    tasks.save(t);
    activity.log(
        current.id(),
        status == TaskStatus.DONE ? ActivityType.TASK_APPROVED : ActivityType.TASK_STATUS_CHANGED,
        "TASK",
        id,
        "Changed task status to " + status);
    return mapper.task(t);
  }

  @Transactional(readOnly = true)
  public TaskStatsResponse stats() {
    User actor = current.get();
    if (actor.getRole() == Role.AGENT)
      return new TaskStatsResponse(
          tasks.countByAssigneesId(actor.getId()),
          tasks.countByStatusAndAssigneesId(TaskStatus.DONE, actor.getId()),
          tasks.countByStatusAndAssigneesId(TaskStatus.OVERDUE, actor.getId()));
    return new TaskStatsResponse(
        tasks.count(),
        tasks.countByStatus(TaskStatus.DONE),
        tasks.countByStatus(TaskStatus.OVERDUE));
  }

  @Transactional(readOnly = true)
  public Page<SubTaskResponse> subtasks(UUID task, Pageable pageable) {
    find(task);
    return subs.findByTaskId(task, pageable).map(mapper::subTask);
  }

  @Transactional
  public SubTaskResponse createSub(UUID task, SubTaskRequest r) {
    Task t = find(task);
    SubTask s = new SubTask();
    s.setTask(t);
    setSub(s, r);
    s = subs.save(s);
    activity.log(current.id(), ActivityType.SUBTASK_CREATED, "TASK", task, "Created subtask");
    return mapper.subTask(s);
  }

  @Transactional
  public SubTaskResponse updateSub(UUID task, UUID id, SubTaskRequest r) {
    find(task);
    SubTask s = subs.findById(id).orElseThrow(() -> new ResourceNotFoundException("Subtask"));
    if (!s.getTask().getId().equals(task)) throw new ResourceNotFoundException("Subtask");
    setSub(s, r);
    subs.save(s);
    activity.log(current.id(), ActivityType.SUBTASK_CREATED, "TASK", task, "Updated subtask");
    return mapper.subTask(s);
  }

  @Transactional
  public void deleteSub(UUID task, UUID id) {
    find(task);
    SubTask s = subs.findById(id).orElseThrow(() -> new ResourceNotFoundException("Subtask"));
    if (!s.getTask().getId().equals(task)) throw new ResourceNotFoundException("Subtask");
    subs.delete(s);
    activity.log(current.id(), ActivityType.SUBTASK_CREATED, "TASK", task, "Deleted subtask");
  }

  @Transactional(readOnly = true)
  public Page<CommentResponse> comments(UUID task, Pageable pageable) {
    find(task);
    return comments.findByTaskId(task, pageable).map(mapper::comment);
  }

  @Transactional
  public CommentResponse comment(UUID task, CommentRequest r, UUID parent) {
    Task t = find(task);
    TaskComment c = new TaskComment();
    c.setTask(t);
    c.setAuthor(current.get());
    c.setBody(r.getBody());
    if (parent != null) {
      TaskComment p =
          comments.findById(parent).orElseThrow(() -> new ResourceNotFoundException("Comment"));
      if (!p.getTask().getId().equals(task))
        throw new IllegalArgumentException("Parent comment belongs to another task");
      c.setParentComment(p);
    }
    comments.save(c);
    activity.log(
        current.id(), ActivityType.COMMENT_RECEIVED_TASK, "TASK", task, "Added task comment");
    return mapper.comment(c);
  }

  @Transactional
  public CommentResponse updateComment(UUID task, UUID id, CommentRequest r) {
    find(task);
    TaskComment c =
        comments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Comment"));
    if (!c.getTask().getId().equals(task)) throw new ResourceNotFoundException("Comment");
    if (!c.getAuthor().getId().equals(current.id()))
      throw new ForbiddenException("Only the author may edit this comment");
    c.setBody(r.getBody());
    comments.save(c);
    activity.log(
        current.id(), ActivityType.COMMENT_RECEIVED_TASK, "TASK", task, "Updated task comment");
    return mapper.comment(c);
  }

  @Transactional
  public void deleteComment(UUID task, UUID id) {
    find(task);
    TaskComment c =
        comments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Comment"));
    if (!c.getTask().getId().equals(task)) throw new ResourceNotFoundException("Comment");
    if (!c.getAuthor().getId().equals(current.id()))
      throw new ForbiddenException("Only the author may delete this comment");
    comments.delete(c);
    activity.log(
        current.id(), ActivityType.COMMENT_RECEIVED_TASK, "TASK", task, "Deleted task comment");
  }

  @Transactional
  public int markOverdue() {
    List<Task> overdue =
        tasks.findByDueDateBeforeAndStatusNot(OffsetDateTime.now(), TaskStatus.DONE).stream()
            .filter(t -> t.getStatus() != TaskStatus.OVERDUE)
            .toList();
    for (Task t : overdue) {
      t.setStatus(TaskStatus.OVERDUE);
      tasks.save(t);
      activity.log(
          null,
          ActivityType.TASK_STATUS_CHANGED,
          "TASK",
          t.getId(),
          "Task automatically marked overdue");
    }
    return overdue.size();
  }

  private Task find(UUID id) {
    Task task = tasks.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task"));
    User actor = current.get();
    if (actor.getRole() == Role.AGENT
        && task.getAssignees().stream().noneMatch(user -> user.getId().equals(actor.getId())))
      throw new ForbiddenException("Task is not assigned to you");
    return task;
  }

  private void apply(Task t, TaskRequest r) {
    t.setTitle(r.getTitle());
    t.setDescription(r.getDescription());
    if (r.getStatus() != null) t.setStatus(r.getStatus());
    t.setPriority(r.getPriority());
    if (r.getAllowReminder() != null) t.setAllowReminder(r.getAllowReminder());
    t.setDueDate(r.getDueDate());
    if (r.getLinkedLeadId() != null)
      t.setLinkedLead(
          leads
              .findById(r.getLinkedLeadId())
              .orElseThrow(() -> new ResourceNotFoundException("Lead")));
    if (r.getLinkedDealId() != null)
      t.setLinkedDeal(
          deals
              .findById(r.getLinkedDealId())
              .orElseThrow(() -> new ResourceNotFoundException("Deal")));
    if (r.getAssigneeIds() != null) {
      Set<User> a = new HashSet<>(users.findAllById(r.getAssigneeIds()));
      if (a.size() != r.getAssigneeIds().size()) throw new ResourceNotFoundException("Assignee");
      t.setAssignees(a);
    }
  }

  private void setSub(SubTask s, SubTaskRequest r) {
    s.setTitle(r.getTitle());
    s.setDescription(r.getDescription());
    s.setPriority(r.getPriority());
    if (r.getComplete() != null) s.setComplete(r.getComplete());
  }

  private void syncTaskCalendarEvent(Task task) {
    deleteTaskCalendarEvent(task.getId());
    if (task.getDueDate() == null) return;
    CalendarEvent event = new CalendarEvent();
    event.setOwner(task.getCreatedBy());
    event.setTitle(task.getTitle());
    event.setDescription(taskDescription(task));
    event.setStartTime(task.getDueDate());
    event.setEndTime(task.getDueDate().plusMinutes(30));
    event.setEventType(TASK_EVENT_TYPE);
    event.setLinkedLead(task.getLinkedLead());
    event.setLinkedDeal(task.getLinkedDeal());
    event.setAssignees(
        task.getAssignees().stream().map(User::getId).toArray(UUID[]::new));
    calendarEvents.save(event);
  }

  private void deleteTaskCalendarEvent(UUID taskId) {
    calendarEvents
        .findAll()
        .stream()
        .filter(event -> TASK_EVENT_TYPE.equals(event.getEventType()))
        .filter(event -> taskDescriptionMatches(event.getDescription(), taskId))
        .forEach(calendarEvents::delete);
  }

  private boolean taskDescriptionMatches(String description, UUID taskId) {
    if (description == null || taskId == null) return false;
    String marker = TASK_EVENT_MARKER_PREFIX + taskId + TASK_EVENT_MARKER_SUFFIX;
    return description.endsWith(marker);
  }

  private String taskDescription(Task task) {
    String base = task.getDescription() == null ? "" : task.getDescription();
    return base + TASK_EVENT_MARKER_PREFIX + task.getId() + TASK_EVENT_MARKER_SUFFIX;
  }
}
