package com.skytech.crm.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.skytech.crm.dto.request.*;
import com.skytech.crm.entity.*;
import com.skytech.crm.enums.Role;
import com.skytech.crm.mapper.CrmMapper;
import com.skytech.crm.repository.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DealLogServiceTest {
  @Mock DealRepository deals;
  @Mock DealLogRepository logs;
  @Mock DealLogCommentRepository comments;
  @Mock AutomationJobService automationJobs;
  @Mock CurrentUserService current;
  @Mock ActivityService activity;
  @Mock CrmMapper mapper;
  @Mock CalendarSyncService calendar;
  @Mock InAppNotificationService inAppNotifications;
  @InjectMocks DealLogService service;

  @Test
  void paymentCreateUpdateAndDeleteReconcileDealTotals() {
    UUID dealId = UUID.randomUUID(), logId = UUID.randomUUID();
    User admin = new User();
    admin.setId(UUID.randomUUID());
    admin.setRole(Role.ADMIN);
    Deal deal = new Deal();
    deal.setId(dealId);
    deal.setTitle("CRM");
    deal.setContractValue(new BigDecimal("1000"));
    deal.setTotalPaid(new BigDecimal("100"));
    when(current.get()).thenReturn(admin);
    when(current.id()).thenReturn(admin.getId());
    when(deals.findById(dealId)).thenReturn(Optional.of(deal));
    when(logs.save(any(DealLog.class)))
        .thenAnswer(
            invocation -> {
              DealLog value = invocation.getArgument(0);
              if (value.getId() == null) value.setId(logId);
              return value;
            });
    service.create(dealId, payment("250"));
    assertThat(deal.getTotalPaid()).isEqualByComparingTo("350");
    assertThat(deal.getArrears()).isEqualByComparingTo("650");

    DealLog stored = new DealLog();
    stored.setId(logId);
    stored.setDeal(deal);
    stored.setCreatedBy(admin);
    stored.setAmountPaid(new BigDecimal("250"));
    when(logs.findById(logId)).thenReturn(Optional.of(stored));
    service.update(dealId, logId, payment("400"));
    assertThat(deal.getTotalPaid()).isEqualByComparingTo("500");
    assertThat(deal.getArrears()).isEqualByComparingTo("500");

    service.delete(dealId, logId);
    assertThat(deal.getTotalPaid()).isEqualByComparingTo("100");
    assertThat(deal.getArrears()).isEqualByComparingTo("900");
    verify(logs).delete(stored);
  }

  @Test
  void rejectsPaymentLogWithoutPaymentMode() {
    UUID dealId = UUID.randomUUID();
    User admin = new User();
    admin.setId(UUID.randomUUID());
    admin.setRole(Role.ADMIN);
    Deal deal = new Deal();
    deal.setId(dealId);
    when(current.get()).thenReturn(admin);
    when(deals.findById(dealId)).thenReturn(Optional.of(deal));
    DealLogRequest invalid =
        new CreateDealLogRequest().setLogType("PAYMENT").setAmountPaid(BigDecimal.TEN);

    assertThatThrownBy(() -> service.create(dealId, invalid))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("paymentMode");
  }

  @Test
  void unrelatedTypeSpecificFieldsCannotAlterPaymentTotals() {
    UUID dealId = UUID.randomUUID();
    User admin = new User();
    admin.setId(UUID.randomUUID());
    admin.setRole(Role.ADMIN);
    Deal deal = new Deal();
    deal.setId(dealId);
    deal.setContractValue(new BigDecimal("1000"));
    deal.setTotalPaid(new BigDecimal("100"));
    when(current.get()).thenReturn(admin);
    when(current.id()).thenReturn(admin.getId());
    when(deals.findById(dealId)).thenReturn(Optional.of(deal));
    when(logs.save(any(DealLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

    DealLogRequest negotiation =
        new CreateDealLogRequest()
            .setLogType("NEGOTIATION")
            .setContactMode("PHONE_CALL")
            .setResponseType("POSITIVE")
            .setAmountPaid(new BigDecimal("900"));

    service.create(dealId, negotiation);

    ArgumentCaptor<DealLog> saved = ArgumentCaptor.forClass(DealLog.class);
    verify(logs).save(saved.capture());
    assertThat(saved.getValue().getAmountPaid()).isNull();
    assertThat(deal.getTotalPaid()).isEqualByComparingTo("100");
    verify(deals, never()).save(deal);
  }

  private DealLogRequest payment(String amount) {
    return new CreateDealLogRequest()
        .setLogType("PAYMENT")
        .setResponseType("POSITIVE")
        .setAmountPaid(new BigDecimal(amount))
        .setPaymentMode("BANK_TRANSFER")
        .setInvoiceNumber("INV-1")
        .setReceiptNumber("REC-1")
        .setInvoiceIssued(true)
        .setBody("Payment received");
  }
}
