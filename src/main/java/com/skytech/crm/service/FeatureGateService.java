package com.skytech.crm.service;

import com.skytech.crm.entity.User;
import com.skytech.crm.enums.Feature;
import org.springframework.stereotype.Service;

@Service
public class FeatureGateService {
  public void require(User user, Feature feature) {
    // STUB: everything is free tier for now.
    // When billing goes live, replace this body with plan-tier checks.
    // throw new FeatureGatedException(feature) if not allowed.
  }

  public boolean canAccess(User user, Feature feature) {
    return true; // stub
  }
}
