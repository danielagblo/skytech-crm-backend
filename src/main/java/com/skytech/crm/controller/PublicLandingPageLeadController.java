package com.skytech.crm.controller;

import com.skytech.crm.dto.request.PublicLandingPageLeadRequest;
import com.skytech.crm.dto.response.*;
import com.skytech.crm.service.PublicLandingPageLeadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/landing-page-leads")
public class PublicLandingPageLeadController {
  private final PublicLandingPageLeadService submissions;

  @PostMapping
  ResponseEntity<ApiResponse<PublicLandingPageLeadResponse>> create(
      @Valid @RequestBody PublicLandingPageLeadRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(submissions.create(request), "Lead submitted"));
  }
}
