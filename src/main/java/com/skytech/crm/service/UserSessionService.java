package com.skytech.crm.service;

import com.skytech.crm.entity.*;
import com.skytech.crm.repository.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSessionService {
  private final UserSessionRepository sessions;

  @Value("${presence.inactivity-timeout-minutes:5}")
  private long inactivityTimeoutMinutes;

  @Transactional
  public void start(User user) {
    OffsetDateTime now = OffsetDateTime.now();
    sessions.findOpen(user.getId(), user.getCompanyId()).stream().findFirst()
        .ifPresent(existing -> existing.setEndedAt(existing.getLastActivityAt()));
    UserSession session = new UserSession();
    session.setCompanyId(user.getCompanyId());
    session.setUser(user);
    session.setStartedAt(now);
    session.setLastActivityAt(now);
    sessions.save(session);
  }

  @Transactional
  public void heartbeat(User user) {
    OffsetDateTime now = OffsetDateTime.now();
    UserSession session =
        sessions.findOpen(user.getId(), user.getCompanyId()).stream().findFirst().orElseGet(() -> {
          UserSession created = new UserSession();
          created.setCompanyId(user.getCompanyId());
          created.setUser(user);
          created.setStartedAt(now);
          return created;
        });
    session.setLastActivityAt(now);
    sessions.save(session);
  }

  @Transactional
  public void end(User user) {
    sessions.findOpen(user.getId(), user.getCompanyId()).stream().findFirst().ifPresent(session -> {
      session.setEndedAt(OffsetDateTime.now());
      sessions.save(session);
    });
  }

  @Transactional(readOnly = true)
  public long activeSeconds(UUID companyId, UUID userId) {
    OffsetDateTime now = OffsetDateTime.now();
    return sessions.findTenantSessions(companyId, userId).stream()
        .mapToLong(session -> {
          OffsetDateTime end = session.getEndedAt() != null ? session.getEndedAt() : session.getLastActivityAt();
          if (end == null || session.getStartedAt() == null || end.isBefore(session.getStartedAt())) return 0;
          return Duration.between(session.getStartedAt(), end).getSeconds();
        })
        .sum();
  }

  @Scheduled(
      fixedDelayString = "${presence.session-sweep-ms:60000}",
      initialDelayString = "${presence.session-sweep-ms:60000}")
  @Transactional
  public void closeInactiveSessions() {
    OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(inactivityTimeoutMinutes);
    for (UserSession session : sessions.findByEndedAtIsNullAndLastActivityAtBefore(cutoff)) {
      session.setEndedAt(session.getLastActivityAt());
      sessions.save(session);
    }
  }
}
