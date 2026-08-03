package com.skytech.crm.exception;

import com.skytech.crm.enums.Feature;
import lombok.Getter;

@Getter
public class FeatureGatedException extends RuntimeException {
  private final Feature feature;

  public FeatureGatedException(Feature f) {
    super("Upgrade required for " + f);
    feature = f;
  }
}
