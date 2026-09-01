package com.skytech.crm.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LeadRequestTest {
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

  @Test
  void acceptsBirthdayWithoutYear() throws Exception {
    LeadRequest request =
        json.readValue(
            """
            {"firstName":"Jane","phone1":"5551234567","birthday":"02-29"}
            """,
            CreateLeadRequest.class);

    assertThat(request.getBirthday()).isEqualTo(LocalDate.of(2000, 2, 29));
  }

  @Test
  void keepsAcceptingLegacyFullBirthday() throws Exception {
    LeadRequest request =
        json.readValue(
            """
            {"firstName":"Jane","phone1":"5551234567","birthday":"1992-02-23"}
            """,
            CreateLeadRequest.class);

    assertThat(request.getBirthday()).isEqualTo(LocalDate.of(1992, 2, 23));
  }

  @Test
  void rejectsInvalidMonthAndDay() {
    assertThatThrownBy(
            () ->
                json.readValue(
                    """
                    {"firstName":"Jane","phone1":"5551234567","birthday":"02-31"}
                    """,
                    CreateLeadRequest.class))
        .hasMessageContaining("Birthday must be a valid MM-DD or YYYY-MM-DD value");
  }

  @Test
  void onlyFirstNameContactAndBirthdayAreRequired() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var validator = factory.getValidator();
      var valid =
          new CreateLeadRequest()
              .setFirstName("Jane")
              .setPhone1("5551234567")
              .setBirthday(LocalDate.of(2000, 2, 23));

      assertThat(validator.validate(valid)).isEmpty();
      assertThat(validator.validate(new CreateLeadRequest()))
          .extracting(violation -> violation.getPropertyPath().toString())
          .containsExactlyInAnyOrder("firstName", "phone1", "birthday");
    }
  }
}
