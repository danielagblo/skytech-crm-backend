package com.skytech.crm.service;

import com.skytech.crm.dto.response.DealResponse;
import com.skytech.crm.enums.DealStage;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PipelineService {
  private final DealService deals;

  public Map<DealStage, List<DealResponse>> get() {
    return deals.pipeline();
  }
}
