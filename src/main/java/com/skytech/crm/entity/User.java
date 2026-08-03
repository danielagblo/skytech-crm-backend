package com.skytech.crm.entity;

import com.skytech.crm.enums.*;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "users")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {
  @Column(nullable = false, length = 100)
  private String firstName;

  @Column(nullable = false, length = 100)
  private String lastName;

  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(length = 30)
  private String phone;

  @Column(unique = true, length = 100)
  private String username;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private Role role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private PlanTier planTier = PlanTier.FREE;

  @Column(columnDefinition = "text")
  private String profilePhotoUrl;

  private boolean isActive = true;
  private OffsetDateTime lastLogin;

  @Column(length = 10)
  private String otpCode;

  private OffsetDateTime otpExpiresAt;

  @Column(length = 255)
  private String refreshTokenHash;

  private OffsetDateTime deletedAt;

  public String fullName() {
    return firstName + " " + lastName;
  }
}
