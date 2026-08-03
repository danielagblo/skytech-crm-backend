package com.skytech.crm.service;

import com.skytech.crm.dto.request.UserRequest;
import com.skytech.crm.dto.response.*;
import com.skytech.crm.entity.User;
import com.skytech.crm.enums.*;
import com.skytech.crm.exception.*;
import com.skytech.crm.mapper.CrmMapper;
import com.skytech.crm.repository.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository users;
  private final DealRepository deals;
  private final DealLogRepository dealLogs;
  private final PasswordEncoder passwords;
  private final CrmMapper mapper;
  private final CurrentUserService current;
  private final FeatureGateService gates;
  private final ActivityService activity;

  @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
  @Transactional(readOnly = true)
  public Page<UserResponse> list(String search, Pageable p) {
    Specification<User> s =
        (r, q, b) ->
            search == null || search.isBlank()
                ? b.conjunction()
                : b.or(
                    b.like(b.lower(r.get("firstName")), "%" + search.toLowerCase() + "%"),
                    b.like(b.lower(r.get("lastName")), "%" + search.toLowerCase() + "%"),
                    b.like(b.lower(r.get("email")), "%" + search.toLowerCase() + "%"));
    return users.findAll(s, p).map(mapper::user);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public UserResponse create(UserRequest r) {
    gates.require(current.get(), Feature.UNLIMITED_AGENTS);
    if (users.existsByEmailIgnoreCase(r.getEmail()))
      throw new org.springframework.dao.DataIntegrityViolationException("Email already exists");
    User u = new User();
    apply(u, r, true);
    u = users.save(u);
    activity.log(
        current.id(),
        ActivityType.LEAD_STAGE_CHANGED,
        "SYSTEM",
        u.getId(),
        "Created user " + u.getEmail());
    return mapper.user(u);
  }

  @Transactional(readOnly = true)
  public UserResponse get(UUID id) {
    User me = current.get();
    if (me.getRole() == Role.AGENT && !me.getId().equals(id))
      throw new ForbiddenException("Agents may only view their own profile");
    return mapper.user(find(id));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public UserResponse update(UUID id, UserRequest r) {
    User u = find(id);
    apply(u, r, false);
    u = users.save(u);
    activity.log(current.id(), ActivityType.LEAD_STAGE_CHANGED, "SYSTEM", id, "Updated user");
    return mapper.user(u);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public void delete(UUID id) {
    User u = find(id);
    if (current.id().equals(id))
      throw new IllegalArgumentException("You cannot delete your own account");
    u.setDeletedAt(java.time.OffsetDateTime.now());
    u.setActive(false);
    users.save(u);
    activity.log(current.id(), ActivityType.LEAD_STAGE_CHANGED, "SYSTEM", id, "Deleted user");
  }

  @Transactional
  public UserResponse photo(UUID id, MultipartFile file) {
    User me = current.get();
    if (me.getRole() != Role.ADMIN && !me.getId().equals(id))
      throw new ForbiddenException("Not allowed");
    if (file.isEmpty() || file.getContentType() == null)
      throw new IllegalArgumentException("An image file is required");
    String ext =
        switch (file.getContentType().toLowerCase()) {
          case "image/jpeg" -> ".jpg";
          case "image/png" -> ".png";
          case "image/webp" -> ".webp";
          case "image/gif" -> ".gif";
          default ->
              throw new IllegalArgumentException(
                  "Only JPEG, PNG, WebP, and GIF images are supported");
        };
    try {
      Path dir = Paths.get("uploads", "profiles").toAbsolutePath().normalize();
      Files.createDirectories(dir);
      Path target = dir.resolve(id + ext).normalize();
      if (!target.startsWith(dir)) throw new IllegalArgumentException("Invalid file name");
      file.transferTo(target);
      User u = find(id);
      u.setProfilePhotoUrl("/uploads/profiles/" + target.getFileName());
      users.save(u);
      activity.log(
          me.getId(), ActivityType.LEAD_STAGE_CHANGED, "SYSTEM", id, "Updated profile photo");
      return mapper.user(u);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to store profile photo", e);
    }
  }

  @Transactional(readOnly = true)
  public UserPerformanceResponse performance(UUID id) {
    User me = current.get();
    if (me.getRole() == Role.AGENT && !me.getId().equals(id))
      throw new ForbiddenException("Agents may only view their own performance");
    find(id);
    var owned = deals.findByAssignedToId(id);
    var closed = owned.stream().filter(d -> d.getStage() == DealStage.CLIENT_RETENTION).toList();
    var revenue =
        closed.stream()
            .map(d -> d.getTotalPaid() == null ? java.math.BigDecimal.ZERO : d.getTotalPaid())
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    Map<String, java.math.BigDecimal> byMonth = new TreeMap<>();
    closed.stream()
        .filter(d -> d.getCreatedAt() != null)
        .forEach(
            d ->
                byMonth.merge(
                    java.time.YearMonth.from(d.getCreatedAt()).toString(),
                    Optional.ofNullable(d.getTotalPaid()).orElse(java.math.BigDecimal.ZERO),
                    java.math.BigDecimal::add));
    List<java.math.BigDecimal> rankings =
        users.findAll().stream()
            .filter(u -> u.getRole() == Role.AGENT)
            .map(
                u ->
                    deals.findByAssignedToId(u.getId()).stream()
                        .filter(d -> d.getStage() == DealStage.CLIENT_RETENTION)
                        .map(
                            d ->
                                Optional.ofNullable(d.getTotalPaid())
                                    .orElse(java.math.BigDecimal.ZERO))
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add))
            .sorted(Comparator.reverseOrder())
            .toList();
    int rank = Math.max(1, rankings.indexOf(revenue) + 1);
    Set<UUID> dealIds =
        owned.stream()
            .map(com.skytech.crm.entity.Deal::getId)
            .collect(java.util.stream.Collectors.toSet());
    long callSeconds =
        dealIds.isEmpty()
            ? 0
            : dealLogs.findByDealIdIn(dealIds).stream()
                .map(com.skytech.crm.entity.DealLog::getCallDurationSeconds)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();
    return new UserPerformanceResponse(rank, closed.size(), revenue, callSeconds / 3600, byMonth);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public UserResponse role(UUID id, Role role) {
    User u = find(id);
    u.setRole(role);
    users.save(u);
    activity.log(
        current.id(),
        ActivityType.LEAD_STAGE_CHANGED,
        "SYSTEM",
        id,
        "Changed user role to " + role);
    return mapper.user(u);
  }

  private User find(UUID id) {
    return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User"));
  }

  private void apply(User u, UserRequest r, boolean creating) {
    u.setFirstName(r.getFirstName());
    u.setLastName(r.getLastName());
    u.setEmail(r.getEmail().toLowerCase());
    u.setPhone(r.getPhone());
    u.setUsername(r.getUsername());
    u.setRole(r.getRole());
    u.setPlanTier(r.getPlanTier() == null ? PlanTier.FREE : r.getPlanTier());
    if (r.getActive() != null) u.setActive(r.getActive());
    if (r.getPassword() != null && !r.getPassword().isBlank())
      u.setPasswordHash(passwords.encode(r.getPassword()));
    else if (creating) throw new IllegalArgumentException("Password is required");
  }
}
