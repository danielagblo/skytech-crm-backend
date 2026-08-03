package com.skytech.crm.service;

import static org.assertj.core.api.Assertions.*;

import com.skytech.crm.entity.User;
import com.skytech.crm.enums.Feature;
import org.junit.jupiter.api.Test;

class FeatureGateServiceTest {
  @Test
  void allFeaturesAreTemporarilyAvailable() {
    FeatureGateService service = new FeatureGateService();
    for (Feature feature : Feature.values()) {
      assertThat(service.canAccess(new User(), feature)).isTrue();
      assertThatCode(() -> service.require(new User(), feature)).doesNotThrowAnyException();
    }
  }
}
