package com.skytech.crm.dto.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.skytech.crm.enums.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;
import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class LeadRequest {
  private UUID[] assignedTo;

  @Size(max = 100)
  private String firstName;

  @Size(max = 100)
  private String lastName;

  @Email
  @Size(max = 255)
  private String email;

  @Size(max = 30)
  private String phone1;

  @Size(max = 30)
  private String phone2;

  @Size(max = 30)
  private String whatsapp;

  @Size(max = 255)
  private String companyName;

  @Size(max = 100)
  private String role;

  private String address;

  @Size(max = 100)
  private String industry;

  @Size(max = 100)
  private String category;

  @Pattern(regexp = "SMS|EMAIL|FACEBOOK|GOOGLE|BANNER|META_ADS")
  private String leadSource;

  private Priority priority;
  private LeadStatus status;

  @Pattern(regexp = "IN_1_WEEK|ONE_TO_TWO_MONTHS|THREE_PLUS_MONTHS")
  private String launchTimeline;

  private Boolean hasPublicOffice;
  private Boolean meetingArranged;
  private LocalDate birthday;
  private Boolean smsOptIn;
  private Boolean emailOptIn;
  private Boolean newsletterOptIn;
  private String description;

  @Min(0)
  @Max(100)
  private Integer conversionScore;
}
