package com.skytech.crm.controller;

import com.skytech.crm.dto.request.*;
import com.skytech.crm.dto.response.ApiResponse;
import com.skytech.crm.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ratings")
@RequiredArgsConstructor
public class RatingController extends BaseController {
  private final RatingService ratings;

  @PostMapping
  ApiResponse<?> request(@Valid @RequestBody RatingRequest request) {
    return ok(ratings.request(request));
  }

  @GetMapping("/public/{token}")
  ApiResponse<?> info(@PathVariable String token) {
    return ok(ratings.info(token));
  }

  @PostMapping("/public/{token}")
  ApiResponse<?> submit(@PathVariable String token, @Valid @RequestBody RatingSubmitRequest request) {
    return ok(ratings.submit(token, request));
  }
}