package com.skytech.crm.controller;

import com.skytech.crm.dto.response.ApiResponse;
import com.skytech.crm.service.PipelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pipeline")
@RequiredArgsConstructor
public class PipelineController extends BaseController {
  private final PipelineService pipeline;

  @GetMapping
  ApiResponse<?> get() {
    return ok(pipeline.get());
  }
}
