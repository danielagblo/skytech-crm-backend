package com.skytech.crm.config;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.web.cors.*;

@Configuration
public class CorsConfig {
  @Bean
  CorsConfigurationSource cors(@Value("${cors.allowed-origins}") String origins) {
    CorsConfiguration c = new CorsConfiguration();
    var allowed =
        Arrays.stream(origins.split(","))
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
}
