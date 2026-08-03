package com.skytech.crm.service;

import com.skytech.crm.entity.User;
import com.skytech.crm.exception.ResourceNotFoundException;
import com.skytech.crm.repository.UserRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
  private final UserRepository users;

  public User get() {
    String id = SecurityContextHolder.getContext().getAuthentication().getName();
    return users
        .findById(UUID.fromString(id))
        .orElseThrow(() -> new ResourceNotFoundException("User"));
  }

  public UUID id() {
    return get().getId();
  }
}
