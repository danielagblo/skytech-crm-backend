package com.skytech.crm.security;

import com.skytech.crm.config.JwtConfig;
import com.skytech.crm.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
  private final JwtConfig config;

  private SecretKey key() {
    return Keys.hmacShaKeyFor(config.secret().getBytes(StandardCharsets.UTF_8));
  }

  public String access(User u) {
    return build(u, "access", config.accessTokenExpiryMs());
  }

  public String refresh(User u) {
    return build(u, "refresh", config.refreshTokenExpiryMs());
  }

  private String build(User u, String type, long ttl) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(u.getId().toString())
        .claim("email", u.getEmail())
        .claim("role", u.getRole().name())
        .claim("type", type)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusMillis(ttl)))
        .signWith(key())
        .compact();
  }

  public Claims parse(String token) {
    return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
  }

  public UUID userId(String token) {
    return UUID.fromString(parse(token).getSubject());
  }

  public boolean valid(String token, String type) {
    try {
      return type.equals(parse(token).get("type", String.class));
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }
}
