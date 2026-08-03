package com.skytech.crm.security;

import static org.assertj.core.api.Assertions.*;

import com.skytech.crm.config.JwtConfig;
import com.skytech.crm.entity.User;
import com.skytech.crm.enums.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {
  @Test
  void createsAndValidatesTypedTokens() {
    JwtTokenProvider provider =
        new JwtTokenProvider(new JwtConfig("01234567890123456789012345678901", 60_000, 120_000));
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail("agent@example.com");
    user.setRole(Role.AGENT);
    String access = provider.access(user), refresh = provider.refresh(user);
    assertThat(provider.valid(access, "access")).isTrue();
    assertThat(provider.valid(access, "refresh")).isFalse();
    assertThat(provider.valid(refresh, "refresh")).isTrue();
    assertThat(provider.userId(access)).isEqualTo(user.getId());
  }
}
