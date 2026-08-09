package com.skytech.crm.dto.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class InvoiceRequest {
  @NotNull private UUID dealId;

  @Size(max = 255)
  private String recipientName;

  @Size(max = 255)
  private String recipientCompany;

  @Email
  @Size(max = 255)
  private String recipientEmail;

  private String recipientAddress;
  private LocalDate issueDate;

  @NotBlank
  @Pattern(regexp = "[A-Z]{3}")
  private String currency = "GHS";

  @NotNull
  @DecimalMin("0.00")
  @DecimalMax("100.00")
  @Digits(integer = 3, fraction = 4)
  private BigDecimal taxRate = BigDecimal.ZERO;

  @NotNull
  @PositiveOrZero
  @Digits(integer = 13, fraction = 2)
  private BigDecimal discountAmount = BigDecimal.ZERO;

  private String notes;
  private String terms;

  @NotEmpty
  @Size(max = 100)
  private List<@Valid InvoiceItemRequest> items;

  private Long version;

  public record InvoiceItemRequest(
      @NotBlank @Size(max = 500) String description,
      @NotNull @DecimalMin(value = "0.0001") @Digits(integer = 11, fraction = 4)
          BigDecimal quantity,
      @NotNull @PositiveOrZero @Digits(integer = 13, fraction = 2) BigDecimal unitPrice,
      List<@Size(max = 500) String> subLines) {}
}
