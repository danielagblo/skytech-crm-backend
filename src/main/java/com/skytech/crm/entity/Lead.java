package com.skytech.crm.entity;

import com.skytech.crm.enums.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "leads")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class Lead extends BaseEntity {
  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(columnDefinition = "uuid[]")
  private UUID[] assignedTo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @Column(length = 100)
  private String firstName;

  @Column(length = 100)
  private String lastName;

  @Column(length = 255)
  private String email;

  @Column(name = "phone_1", length = 30)
  private String phone1;

  @Column(name = "phone_2", length = 30)
  private String phone2;

  @Column(length = 30)
  private String whatsapp;

  @Column(length = 255)
  private String companyName;

  @Column(length = 100)
  private String role;

  @Column(columnDefinition = "text")
  private String address;

  @Column(length = 100)
  private String industry;

  @Column(length = 100)
  private String category;

  @Column(length = 100)
  private String leadSource;

  @Column(length = 50)
  private String launchTimeline;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private Priority priority;

  @Enumerated(EnumType.STRING)
  @Column(length = 50)
  private LeadStatus status = LeadStatus.NEW;

  private Boolean hasPublicOffice, meetingArranged;
  private LocalDate birthday;
  private boolean smsOptIn, emailOptIn, newsletterOptIn;

  @Column(columnDefinition = "text")
  private String description;

  private int conversionScore;
  private OffsetDateTime deletedAt;
}
