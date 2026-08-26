package com.skytech.crm.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;

public record PublicLandingPageLeadRequest(
    @NotBlank @Size(max = 200) String name,
    @Pattern(regexp = "^$|^\\+[1-9]\\d{6,14}$", message = "must be empty or a valid international number")
        String phone,
    @Email @Size(max = 255) String email,
    @Size(max = 255) String company,
    @NotBlank
        @Pattern(
            regexp =
                "Hospitality|Retail & E-commerce|Education|Tourism & Logistics|Real estate & construction|Healthcare|Tech|NGO|Religion|Other")
        String industry,
    @Size(max = 100) String building,
    @JsonAlias("projectType") @Size(max = 100) String projectType,
    @Size(max = 100) String budget,
    @Size(max = 100) String timeline,
    @Pattern(regexp = "^$|Very urgent|Urgent|Soon") String urgency,
    @Size(max = 100) String coupon,
    @JsonAlias("couponLabel") @Size(max = 255) String couponLabel,
    @Pattern(regexp = "^$|TikTok|Google|Facebook|Instagram|Friends|Other") String referral,
    @JsonAlias("packageName") @Size(max = 255) String packageName,
    @JsonAlias("packagePrice") @Size(max = 100) String packagePrice,
    @Size(max = 255) String source,
    @Size(max = 5000) String message) {}
