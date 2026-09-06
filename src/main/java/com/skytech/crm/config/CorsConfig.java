package com.skytech.crm.config;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.web.cors.*;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${cors.allowed-origins}") String origins) {
    CorsConfiguration c = new CorsConfiguration();
    var raw = System.getenv("CORS_ALLOWED_ORIGINS");
    String value = (raw != null && !raw.isBlank()) ? raw : origins;
    var allowed =
        Arrays.stream(value.split(","))
            .map(String::trim)
            .map(o -> o.replaceAll("^\"+|\"+$", ""))
            .filter(o -> !o.isEmpty())
            .toList();
    c.setAllowedOrigins(allowed);
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("*"));
    c.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
    s.registerCorsConfiguration("/**", c);
    return s;
  }

  @Bean
  FilterRegistrationBean<CorsFilter> corsFilterRegistration(CorsConfigurationSource source) {
    FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
    bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return bean;
  }
}
