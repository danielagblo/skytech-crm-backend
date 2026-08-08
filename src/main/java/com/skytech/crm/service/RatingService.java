package com.skytech.crm.service;

import com.skytech.crm.dto.request.*;
import com.skytech.crm.dto.response.*;
import com.skytech.crm.entity.*;
import com.skytech.crm.enums.ActivityType;
import com.skytech.crm.enums.Role;
import com.skytech.crm.exception.*;
import com.skytech.crm.repository.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RatingService {
  private final DealRepository deals;
  private final DealLogRepository dealLogs;
  private final UserRepository users;
  private final RatingRepository ratings;
  private final CurrentUserService current;
  private final ActivityService activity;
  private final ApplicationEventPublisher publisher;

  @Value("${app.rating-base-url:http://localhost:3000}")
  private String baseUrl;

  @Transactional
  public RatingLinkResponse request(RatingRequest request) {
    Deal deal = deal(request.dealId());
    User agent =
        users.findById(current.id()).orElseThrow(() -> new ResourceNotFoundException("User"));

    Optional<Rating> pending =
        ratings.findFirstByDealIdAndAgentIdAndRatedFalseOrderByCreatedAtDesc(
            deal.getId(), agent.getId());
    if (pending.isPresent()) {
      Rating existing = pending.get();
      return link(existing, "ALREADY_SENT", "A rating link is already pending for this client");
    }

    Rating rating = new Rating();
    rating.setDeal(deal);
    rating.setAgent(agent);
    rating.setCompanyId(deal.getCompanyId());
    rating.setContactChannel("EMAIL");
    if (deal.getLead() != null) {
      rating.setClientEmail(deal.getLead().getEmail());
      rating.setClientName(clientName(deal.getLead()));
    }
    rating.setToken(UUID.randomUUID().toString().replace("-", ""));
    rating.setRated(false);
    rating = ratings.save(rating);

    if (rating.getClientEmail() == null || rating.getClientEmail().isBlank()) {
      return link(rating, "NO_EMAIL", "No client email on record to send a rating link");
    }

    activity.log(
        current.id(), ActivityType.LEAD_LOG_CALL, "DEAL", deal.getId(), "Requested client rating");
    publisher.publishEvent(
        new RatingEmailRequested(
            rating.getId(),
            rating.getClientEmail(),
            agent.fullName(),
            deal.getTitle(),
            baseUrl.replaceAll("/+$", "") + "/rate-me/" + rating.getToken()));
    return link(rating, "SENT", "Rating link sent to " + rating.getClientEmail());
  }

  @Transactional(readOnly = true)
  public RatingInfoResponse info(String token) {
    Rating rating = rating(token);
    return new RatingInfoResponse(
        rating.getId(),
        rating.getAgent().getId(),
        rating.getAgent().fullName(),
        rating.getDeal().getTitle(),
        rating.getRated(),
        rating.getRating(),
        rating.getFeedback());
  }

  @Transactional
  public RatingSubmissionResponse submit(String token, RatingSubmitRequest request) {
    Rating rating = rating(token);
    if (Boolean.TRUE.equals(rating.getRated()))
      throw new IllegalArgumentException("This rating link has already been used");
    rating.setRating(request.rating());
    rating.setFeedback(request.feedback());
    rating.setClientName(
        request.clientName() == null || request.clientName().isBlank()
            ? rating.getClientName()
            : request.clientName());
    rating.setRated(true);
    rating.setRatedAt(OffsetDateTime.now());
    rating = ratings.save(rating);
    dealLogs
        .findFirstByDealIdOrderByCreatedAtDesc(rating.getDeal().getId())
        .ifPresent(
            log -> {
              log.setAutoReviewScore(request.rating());
              dealLogs.save(log);
            });
    return new RatingSubmissionResponse(
        rating.getId(),
        rating.getAgent().getId(),
        rating.getDeal().getId(),
        rating.getRating(),
        rating.getFeedback(),
        rating.getClientName(),
        rating.getRatedAt());
  }

  private Rating rating(String token) {
    return ratings
        .findByToken(token)
        .orElseThrow(() -> new ResourceNotFoundException("Rating link"));
  }

  private RatingLinkResponse link(Rating rating, String status, String message) {
    return new RatingLinkResponse(
        rating.getId(),
        rating.getAgent().getId(),
        rating.getDeal().getId(),
        rating.getClientEmail(),
        status,
        message);
  }

  private Deal deal(UUID id) {
    Deal deal = deals.findById(id).orElseThrow(() -> new ResourceNotFoundException("Deal"));
    User me = current.get();
    if (me.getRole() == Role.AGENT
        && (deal.getAssignedTo() == null || !deal.getAssignedTo().getId().equals(me.getId())))
      throw new ForbiddenException("Deal is not assigned to you");
    return deal;
  }

  private String clientName(Lead lead) {
    String name =
        (lead.getFirstName() == null ? "" : lead.getFirstName().trim())
            + " "
            + (lead.getLastName() == null ? "" : lead.getLastName().trim());
    return name.isBlank() ? null : name.trim();
  }
}