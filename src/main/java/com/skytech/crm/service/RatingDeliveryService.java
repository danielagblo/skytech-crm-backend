package com.skytech.crm.service;

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
  private final ActivityService activity;

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void deliver(RatingEmailRequested request) {
    Rating rating = ratings.findById(request.ratingId()).orElse(null);
    if (rating == null || Boolean.TRUE.equals(rating.getRated())) return;
    try {
      email.send(
          request.toEmail(),
          "How was your experience with " + request.agentName() + "?",
          body(request));
      rating.setEmailSentAt(OffsetDateTime.now());
      ratings.save(rating);
      activity.log(
          null,
          ActivityType.LEAD_LOG_CALL,
          "DEAL",
          rating.getDeal().getId(),
          "Sent client rating link to " + request.toEmail());
    } catch (Exception exception) {
      log.warn(
          "Rating {} email delivery failed: {}", request.ratingId(), exception.getMessage());
    }
  }

  private String body(RatingEmailRequested request) {
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
}
