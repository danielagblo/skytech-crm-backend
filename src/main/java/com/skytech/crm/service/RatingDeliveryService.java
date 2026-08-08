package com.skytech.crm.service;

import com.skytech.crm.entity.Lead;
import com.skytech.crm.entity.Rating;
import com.skytech.crm.enums.ActivityType;
import com.skytech.crm.repository.RatingRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatingDeliveryService {
  private final RatingRepository ratings;
  private final EmailService email;
  private final SmsService sms;
  private final ActivityService activity;

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void deliver(RatingEmailRequested request) {
    Rating rating = ratings.findById(request.ratingId()).orElse(null);
    if (rating == null || Boolean.TRUE.equals(rating.getRated())) return;
    if (rating.getEmailSentAt() != null) return;

    String link = request.link();
    log.info("========== CLIENT RATING LINK ==========");
    log.info("Deal: '{}' | To: {} | {}", request.dealTitle(), request.toEmail(), link);
    log.info("========================================");

    if (request.toEmail() == null || request.toEmail().isBlank()) {
      log.info("No email on record; rating email skipped");
    } else {
      try {
        email.send(
            request.toEmail(),
            "How was your experience with " + request.agentName() + "?",
            emailBody(request));
        log.info("Rating email sent to {}", request.toEmail());
      } catch (Exception exception) {
        log.warn(
            "Rating email to {} failed: {}", request.toEmail(), exception.getMessage());
      }
    }

    String phone = clientPhone(rating);
    if (phone != null && !phone.isBlank()) {
      try {
        sms.send(phone, smsBody(request));
        log.info("Rating SMS sent to {}", safeEnding(phone));
      } catch (Exception exception) {
        log.warn("Rating SMS to {} failed: {}", safeEnding(phone), exception.getMessage());
      }
    } else {
      log.info("No phone on record; rating SMS skipped for {}", request.toEmail());
    }

    rating.setEmailSentAt(OffsetDateTime.now());
    ratings.save(rating);
    activity.log(
        null,
        ActivityType.LEAD_LOG_CALL,
        "DEAL",
        rating.getDeal().getId(),
        "Sent client rating link to " + request.toEmail());
  }

  private String emailBody(RatingEmailRequested request) {
    return """
        Hi,

        Thank you for choosing Skytech. %s recently assisted you regarding "%s".

        We would love to know how we did. Please take a moment to rate your experience:

        %s

        Your feedback helps us serve you better.
        - Skytech Team
        """
        .formatted(request.agentName(), request.dealTitle(), request.link());
  }

  private String smsBody(RatingEmailRequested request) {
    return "Hi, Skytech. %s helped you with \"%s\". Rate your experience: %s"
        .formatted(request.agentName(), request.dealTitle(), request.link());
  }

  private String clientPhone(Rating rating) {
    Lead lead = rating.getDeal() == null ? null : rating.getDeal().getLead();
    if (lead == null) return null;
    if (lead.getPhone1() != null && !lead.getPhone1().isBlank()) return lead.getPhone1();
    return lead.getPhone2();
  }

  private String safeEnding(String phone) {
    if (phone == null || phone.isBlank()) return "<unknown>";
    return "…" + phone.substring(Math.max(0, phone.length() - 4));
  }
}