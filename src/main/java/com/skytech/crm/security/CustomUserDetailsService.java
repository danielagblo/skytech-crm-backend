package com.skytech.crm.security;

import com.skytech.crm.entity.User;
import com.skytech.crm.repository.UserRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
  private final UserRepository users;

  public UserDetails loadUserByUsername(String value) {
    User u;
    try {
      u = users.findById(UUID.fromString(value)).orElseThrow();
    } catch (Exception e) {
      u =
          users
              .findByEmailIgnoreCase(value)
              .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
    return org.springframework.security.core.userdetails.User.withUsername(u.getId().toString())
        .password(u.getPasswordHash())
        .roles(u.getRole().name())
        .disabled(!u.isActive())
        .build();
  }
}
