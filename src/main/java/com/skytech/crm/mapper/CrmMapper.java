package com.skytech.crm.mapper;

import com.skytech.crm.dto.response.*;
import com.skytech.crm.entity.*;
import java.util.*;
import java.util.stream.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CrmMapper {
  @Mapping(target = "active", source = "active")
  UserResponse user(User value);

  @Mapping(target = "createdById", source = "createdBy.id")
  LeadResponse lead(Lead value);

  @Mapping(target = "leadId", source = "lead.id")
  @Mapping(target = "createdById", source = "createdBy.id")
  @Mapping(target = "assignedToId", source = "assignedTo.id")
  @Mapping(target = "paidInFull", source = "paidInFull")
  DealResponse deal(Deal value);

  @Mapping(target = "dealId", source = "deal.id")
  @Mapping(target = "createdById", source = "createdBy.id")
  DealLogResponse dealLog(DealLog value);

  @Mapping(target = "createdById", source = "createdBy.id")
  @Mapping(target = "linkedLeadId", source = "linkedLead.id")
  @Mapping(target = "linkedDealId", source = "linkedDeal.id")
  @Mapping(target = "assigneeIds", expression = "java(ids(value.getAssignees()))")
  TaskResponse task(Task value);

  @Mapping(target = "ownerId", source = "owner.id")
  @Mapping(target = "linkedLeadId", source = "linkedLead.id")
  @Mapping(target = "linkedDealId", source = "linkedDeal.id")
  CalendarEventResponse calendar(CalendarEvent value);

  @Mapping(target = "active", source = "active")
  @Mapping(target = "createdById", source = "createdBy.id")
  AutomationResponse automation(Automation value);

  @Mapping(target = "createdById", source = "createdBy.id")
  BroadcastResponse broadcast(BroadcastMessage value);

  @Mapping(target = "actorId", source = "actor.id")
  ActivityResponse activity(Activity value);

  @Mapping(target = "dealId", source = "deal.id")
  @Mapping(target = "createdById", source = "createdBy.id")
  InvoiceResponse invoice(Invoice value);

  InvoiceResponse.Item invoiceItem(InvoiceItem value);

  @Mapping(target = "dealLogId", source = "dealLog.id")
  @Mapping(target = "recordedById", source = "recordedBy.id")
  InvoiceResponse.Payment invoicePayment(InvoicePayment value);

  @Mapping(target = "taskId", source = "task.id")
  @Mapping(target = "complete", source = "complete")
  SubTaskResponse subTask(SubTask value);

  default Set<UUID> ids(Set<User> users) {
    return users == null ? Set.of() : users.stream().map(User::getId).collect(Collectors.toSet());
  }

  default CommentResponse comment(TaskComment c) {
    return new CommentResponse(
        c.getId(),
        c.getParentComment() == null ? null : c.getParentComment().getId(),
        c.getAuthor() == null ? null : c.getAuthor().getId(),
        c.getAuthor() == null ? null : c.getAuthor().fullName(),
        c.getBody(),
        c.getCreatedAt());
  }

  default CommentResponse comment(DealLogComment c) {
    return new CommentResponse(
        c.getId(),
        c.getParentComment() == null ? null : c.getParentComment().getId(),
        c.getAuthor() == null ? null : c.getAuthor().getId(),
        c.getAuthor() == null ? null : c.getAuthor().fullName(),
        c.getBody(),
        c.getCreatedAt());
  }
}
