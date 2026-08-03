package com.skytech.crm.config;

import java.nio.file.Paths;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String location = Paths.get("uploads").toAbsolutePath().normalize().toUri().toString();
    if (!location.endsWith("/")) {
      location += "/";
    }
    registry.addResourceHandler("/uploads/**").addResourceLocations(location);
  }
}
