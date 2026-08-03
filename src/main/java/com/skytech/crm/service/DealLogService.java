package com.skytech.crm.service;

import com.skytech.crm.dto.request.*;
import com.skytech.crm.dto.response.*;
import com.skytech.crm.entity.*;
import com.skytech.crm.enums.ActivityType;
import com.skytech.crm.exception.*;
import com.skytech.crm.mapper.CrmMapper;
import com.skytech.crm.repository.*;
import java.math.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DealLogService {
  private final DealRepository deals;
  private final DealLogRepository logs;
  private final DealLogCommentRepository comments;
  private final AutomationRepository automations;
  private final AutomationExecutionService execution;
  private final CurrentUserService current;
  private final ActivityService activity;
  private final CrmMapper mapper;

  @Transactional(readOnly = true)
  public org.springframework.data.domain.Page<DealLogResponse> list(
      UUID dealId, org.springframework.data.domain.Pageable pageable) {
    deal(dealId);
    return logs.findByDealId(dealId, pageable).map(mapper::dealLog);
  }

  @Transactional
  public DealLogResponse create(UUID dealId, DealLogRequest r) {
    Deal d = deal(dealId);
    validateTypeSpecificFields(r);
    DealLog l = new DealLog();
    l.setDeal(d);
    l.setCreatedBy(current.get());
    apply(l, r);
    l = logs.save(l);
    applyPaymentDelta(d, BigDecimal.ZERO, l.getAmountPaid());
    applyRetention(d, l);
    if (Optional.ofNullable(l.getAmountPaid()).orElse(BigDecimal.ZERO).signum() > 0)
      triggerPayment(d, l.getAmountPaid());
    activity.log(
        current.id(),
        ActivityType.LEAD_LOG_CALL,
        "DEAL",
        dealId,
        "Created " + l.getLogType() + " deal log");
    return mapper.dealLog(l);
  }

  @Transactional(readOnly = true)
  public DealLogResponse get(UUID dealId, UUID id) {
    return mapper.dealLog(log(dealId, id));
  }

  @Transactional
  public DealLogResponse update(UUID dealId, UUID id, DealLogRequest r) {
    DealLog l = log(dealId, id);
    validateTypeSpecificFields(r);
    BigDecimal old = l.getAmountPaid();
    apply(l, r);
    l = logs.save(l);
    applyPaymentDelta(l.getDeal(), old, l.getAmountPaid());
    applyRetention(l.getDeal(), l);
    if (Optional.ofNullable(l.getAmountPaid())
            .orElse(BigDecimal.ZERO)
            .compareTo(Optional.ofNullable(old).orElse(BigDecimal.ZERO))
        > 0)
      triggerPayment(
          l.getDeal(),
          l.getAmountPaid().subtract(Optional.ofNullable(old).orElse(BigDecimal.ZERO)));
    activity.log(current.id(), ActivityType.LEAD_LOG_CALL, "DEAL", dealId, "Updated deal log");
    return mapper.dealLog(l);
  }

  @Transactional
  public void delete(UUID dealId, UUID id) {
    DealLog l = log(dealId, id);
    applyPaymentDelta(l.getDeal(), l.getAmountPaid(), BigDecimal.ZERO);
    logs.delete(l);
    activity.log(current.id(), ActivityType.LEAD_LOG_CALL, "DEAL", dealId, "Deleted deal log");
  }

  @Transactional(readOnly = true)
  public org.springframework.data.domain.Page<CommentResponse> comments(
      UUID dealId, UUID logId, org.springframework.data.domain.Pageable pageable) {
    log(dealId, logId);
    return comments.findByDealLogId(logId, pageable).map(mapper::comment);
  }

  @Transactional
  public CommentResponse comment(UUID dealId, UUID logId, CommentRequest r, UUID parent) {
    DealLog l = log(dealId, logId);
    DealLogComment c = new DealLogComment();
    c.setDealLog(l);
    c.setAuthor(current.get());
    c.setBody(r.getBody());
    if (parent != null) {
      DealLogComment p =
          comments.findById(parent).orElseThrow(() -> new ResourceNotFoundException("Comment"));
      if (!p.getDealLog().getId().equals(logId))
        throw new IllegalArgumentException("Parent comment belongs to another log");
      c.setParentComment(p);
    }
    c = comments.save(c);
    activity.log(
        current.id(), ActivityType.COMMENT_RECEIVED_LEAD, "DEAL", dealId, "Added deal log comment");
    return mapper.comment(c);
  }

  @Transactional
  public CommentResponse updateComment(UUID dealId, UUID logId, UUID id, CommentRequest r) {
    log(dealId, logId);
    DealLogComment c =
        comments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Comment"));
    if (!c.getDealLog().getId().equals(logId)) throw new ResourceNotFoundException("Comment");
    if (!c.getAuthor().getId().equals(current.id()))
      throw new ForbiddenException("Only the author may edit this comment");
    c.setBody(r.getBody());
    comments.save(c);
    activity.log(
        current.id(),
        ActivityType.COMMENT_RECEIVED_LEAD,
        "DEAL",
        dealId,
        "Updated deal log comment");
    return mapper.comment(c);
  }

  @Transactional
  public void deleteComment(UUID dealId, UUID logId, UUID id) {
    log(dealId, logId);
    DealLogComment c =
        comments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Comment"));
    if (!c.getDealLog().getId().equals(logId)) throw new ResourceNotFoundException("Comment");
    if (!c.getAuthor().getId().equals(current.id()))
      throw new ForbiddenException("Only the author may delete this comment");
    comments.delete(c);
    activity.log(
        current.id(),
        ActivityType.COMMENT_RECEIVED_LEAD,
        "DEAL",
        dealId,
        "Deleted deal log comment");
  }

  private Deal deal(UUID id) {
    Deal d = deals.findById(id).orElseThrow(() -> new ResourceNotFoundException("Deal"));
    User me = current.get();
    if (me.getRole() == com.skytech.crm.enums.Role.AGENT
        && (d.getAssignedTo() == null || !d.getAssignedTo().getId().equals(me.getId())))
      throw new ForbiddenException("Deal is not assigned to you");
    return d;
  }

  private DealLog log(UUID dealId, UUID id) {
    deal(dealId);
    DealLog l = logs.findById(id).orElseThrow(() -> new ResourceNotFoundException("Deal log"));
    if (!l.getDeal().getId().equals(dealId)) throw new ResourceNotFoundException("Deal log");
    return l;
  }

  private void apply(DealLog l, DealLogRequest r) {
    l.setLogType(r.getLogType());
    l.setContactMode(r.getContactMode());
    l.setResponseType(r.getResponseType());
    l.setCallDirection(r.getCallDirection());
    l.setCallDurationSeconds(r.getCallDurationSeconds());
    l.setCallOutcome(r.getCallOutcome());
    l.setFollowUpAt(r.getFollowUpAt());
    l.setSettlementValue(r.getSettlementValue());
    l.setSettlementFollowUp(r.getSettlementFollowUp());
    l.setSpecialConditions(r.getSpecialConditions());
    l.setAmountPaid(r.getAmountPaid());
    l.setPaymentMode(r.getPaymentMode());
    l.setInvoiceNumber(r.getInvoiceNumber());
    l.setReceiptNumber(r.getReceiptNumber());
    l.setInvoiceIssued(r.getInvoiceIssued());
    l.setServiceType(r.getServiceType());
    l.setExpiryDate(r.getExpiryDate());
    l.setRetentionAmount(r.getRetentionAmount());
    l.setRetentionInvoice(r.getRetentionInvoice());
    l.setRetentionReceipt(r.getRetentionReceipt());
    l.setAutoReviewScore(review(r));
    l.setBody(r.getBody());
  }

  private void validateTypeSpecificFields(DealLogRequest request) {
    switch (request.getLogType()) {
      case "NEGOTIATION" -> {
        if (request.getContactMode() == null || request.getResponseType() == null)
          throw new IllegalArgumentException(
              "Negotiation logs require contactMode and responseType");
      }
      case "SETTLEMENT" -> {
        if (request.getSettlementValue() == null || request.getSettlementValue().signum() <= 0)
          throw new IllegalArgumentException("Settlement logs require a positive settlementValue");
      }
      case "PAYMENT" -> {
        if (request.getAmountPaid() == null || request.getAmountPaid().signum() <= 0)
          throw new IllegalArgumentException("Payment logs require a positive amountPaid");
        if (request.getPaymentMode() == null)
          throw new IllegalArgumentException("Payment logs require paymentMode");
      }
      case "CLIENT_RETENTION" -> {
        if (request.getServiceType() == null || request.getExpiryDate() == null)
          throw new IllegalArgumentException(
              "Client retention logs require serviceType and expiryDate");
      }
      default ->
          throw new IllegalArgumentException("Unsupported log type: " + request.getLogType());
    }
  }

  private int review(DealLogRequest r) {
    if ("POSITIVE".equals(r.getResponseType())) return 5;
    if ("NEGATIVE".equals(r.getResponseType())) return 2;
    if ("NO_RESPONSE".equals(r.getResponseType())) return 1;
    return 3;
  }

  private void applyPaymentDelta(Deal d, BigDecimal oldAmount, BigDecimal newAmount) {
    BigDecimal oldValue = Optional.ofNullable(oldAmount).orElse(BigDecimal.ZERO),
        newValue = Optional.ofNullable(newAmount).orElse(BigDecimal.ZERO);
    if (oldValue.compareTo(newValue) == 0) return;
    BigDecimal paid =
        Optional.ofNullable(d.getTotalPaid())
            .orElse(BigDecimal.ZERO)
            .subtract(oldValue)
            .add(newValue)
            .max(BigDecimal.ZERO);
    BigDecimal contract = Optional.ofNullable(d.getContractValue()).orElse(BigDecimal.ZERO);
    d.setTotalPaid(paid);
    d.setArrears(contract.subtract(paid).max(BigDecimal.ZERO));
    d.setPaidInFull(contract.signum() > 0 && paid.compareTo(contract) >= 0);
    deals.save(d);
  }

  private void applyRetention(Deal d, DealLog l) {
    if (l.getExpiryDate() == null || l.getServiceType() == null) return;
    switch (l.getServiceType()) {
      case "HOSTING" -> d.setHostingExpiry(l.getExpiryDate());
      case "DOMAIN" -> d.setDomainExpiry(l.getExpiryDate());
      case "MAINTENANCE" -> d.setMaintenanceExpiry(l.getExpiryDate());
      default ->
          throw new IllegalArgumentException("Unsupported service type: " + l.getServiceType());
    }
    deals.save(d);
  }

  private void triggerPayment(Deal deal, BigDecimal amount) {
    for (Automation automation :
        automations.findByAutomationTypeAndIsActiveTrue(
            com.skytech.crm.enums.AutomationType.PAYMENT))
      execution.execute(
          automation, deal.getLead(), "Payment of " + amount + " received for " + deal.getTitle());
  }
}
