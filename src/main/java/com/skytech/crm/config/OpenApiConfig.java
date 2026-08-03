package com.skytech.crm.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.*;
import java.util.*;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {
  @Bean
  OpenAPI api() {
    return new OpenAPI()
        .info(new Info().title("Skytech CRM API").version("v1").description("B2B CRM REST API"))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }

  @Bean
  OpenApiCustomizer snakeCaseSchemas() {
    return openApi -> {
      if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) return;
      openApi.getComponents().getSchemas().values().forEach(this::snakeCase);
    };
  }

  private void snakeCase(Schema<?> schema) {
    if (schema.getProperties() != null) {
      Map<String, Schema> translated = new LinkedHashMap<>();
      schema
          .getProperties()
          .forEach(
              (name, property) ->
                  translated.put(
                      new PropertyNamingStrategies.SnakeCaseStrategy().translate(name),
                      (Schema) property));
      schema.setProperties(translated);
    }
    if (schema.getRequired() != null)
      schema.setRequired(
          schema.getRequired().stream()
              .map(new PropertyNamingStrategies.SnakeCaseStrategy()::translate)
              .toList());
  }
}
