package com.skytech.crm.dto.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.skytech.crm.enums.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class UserRequest {
  @NotBlank
  @Size(max = 100)
  private String firstName;

  @NotBlank
  @Size(max = 100)
  private String lastName;

  @Email
  @NotBlank
  @Size(max = 255)
  private String email;

  @Size(min = 8, max = 72)
  private String password;

  @Size(max = 30)
  private String phone;

  @Size(max = 100)
  private String username;

  @NotNull private Role role;
  private PlanTier planTier;
  private Boolean active;
}
