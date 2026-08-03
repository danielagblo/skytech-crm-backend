package com.skytech.crm.config;

import com.stripe.StripeClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;

@Configuration
@RequiredArgsConstructor
public class StripeClientConfig {
  private final StripeConfig stripe;

  /** Wires the SDK for future billing work; no service invokes it while billing is inactive. */
  @Bean
  StripeClient stripeClient() {
    return new StripeClient(stripe.secretKey() == null ? "" : stripe.secretKey());
  }
}
