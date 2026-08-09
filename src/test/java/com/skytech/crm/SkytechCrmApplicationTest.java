package com.skytech.crm;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skytech.crm.dto.request.CreateUserRequest;
import com.skytech.crm.dto.response.AuthResponse;
import com.skytech.crm.entity.User;
import com.skytech.crm.enums.Role;
import com.skytech.crm.security.CustomUserDetailsService;
import com.skytech.crm.security.JwtTokenProvider;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:crm;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.flyway.enabled=false",
      "spring.task.scheduling.enabled=false",
      "app.backfill-on-startup=false",
      "jwt.secret=01234567890123456789012345678901"
    })
@AutoConfigureMockMvc
class SkytechCrmApplicationTest {
  @Autowired ObjectMapper objectMapper;

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  RequestMappingHandlerMapping mappings;

  @Autowired MockMvc mockMvc;
  @Autowired JwtTokenProvider tokens;
  @MockitoBean CustomUserDetailsService userDetails;

  @Test
  void contextLoads() {}

  @Test
  void jsonContractUsesSnakeCase() throws Exception {
    String json = objectMapper.writeValueAsString(new AuthResponse.Login(true, UUID.randomUUID()));
    assertThat(json).contains("requires_otp", "user_id").doesNotContain("requiresOtp", "userId");
  }

  @Test
  void operationSpecificRequestDtosKeepTheFlatJsonContract() throws Exception {
    CreateUserRequest request =
        objectMapper.readValue(
            """
            {"first_name":"Jane","last_name":"Doe","email":"jane@example.com",
             "password":"secure-pass","role":"AGENT"}
            """,
            CreateUserRequest.class);
    assertThat(request.getFirstName()).isEqualTo("Jane");
    assertThat(request.getEmail()).isEqualTo("jane@example.com");
  }

  @Test
  void openApiDocumentsOperationSpecificRequestFields() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.components.schemas.CreateUserRequest.properties.first_name").exists())
        .andExpect(jsonPath("$.components.schemas.UpdateDealRequest.properties.version").exists());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void validationErrorsUseFieldLevelSnakeCaseEnvelope() throws Exception {
    mockMvc
        .perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.details.first_name").exists())
        .andExpect(jsonPath("$.details.last_name").exists());
  }

  @Test
  void unauthenticatedErrorsUseTheApiEnvelope() throws Exception {
    mockMvc
        .perform(get("/api/v1/users"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
  }

  @Test
  void validTokenForMissingUserIsRejectedWithTheApiEnvelope() throws Exception {
    User missing = new User();
    missing.setId(UUID.randomUUID());
    missing.setEmail("missing@example.com");
    missing.setRole(Role.AGENT);
    when(userDetails.loadUserByUsername(missing.getId().toString()))
        .thenThrow(new UsernameNotFoundException("User not found"));
    mockMvc
        .perform(
            get("/api/v1/users/" + missing.getId())
                .header("Authorization", "Bearer " + tokens.access(missing)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
  }

  @Test
  void exposesEveryRequiredApiRoute() {
    Set<String> actual = new HashSet<>();
    mappings
        .getHandlerMethods()
        .forEach(
            (info, handler) -> {
              if (!handler.getBeanType().getPackageName().startsWith("com.skytech.crm.controller"))
                return;
              for (String path : info.getPatternValues()) {
                for (RequestMethod method : info.getMethodsCondition().getMethods())
                  actual.add(method + " " + path);
              }
            });
    assertThat(actual)
        .contains(
            "POST /api/v1/auth/login",
            "POST /api/v1/auth/verify-otp",
            "POST /api/v1/auth/refresh",
            "POST /api/v1/auth/logout",
            "GET /api/v1/auth/me",
            "GET /api/v1/users",
            "POST /api/v1/users",
            "GET /api/v1/users/{id}",
            "PUT /api/v1/users/{id}",
            "DELETE /api/v1/users/{id}",
            "PUT /api/v1/users/{id}/photo",
            "GET /api/v1/users/{id}/performance",
            "PUT /api/v1/users/{id}/role",
            "GET /api/v1/leads",
            "POST /api/v1/leads",
            "GET /api/v1/leads/{id}",
            "PUT /api/v1/leads/{id}",
            "DELETE /api/v1/leads/{id}",
            "GET /api/v1/leads/stats",
            "POST /api/v1/leads/{id}/convert",
            "PUT /api/v1/leads/{id}/assign",
            "GET /api/v1/leads/auto-assign/config",
            "PUT /api/v1/leads/auto-assign/config",
            "GET /api/v1/pipeline",
            "GET /api/v1/deals",
            "POST /api/v1/deals",
            "GET /api/v1/deals/{id}",
            "PUT /api/v1/deals/{id}",
            "DELETE /api/v1/deals/{id}",
            "PUT /api/v1/deals/{id}/stage",
            "GET /api/v1/deals/{dealId}/logs",
            "POST /api/v1/deals/{dealId}/logs",
            "GET /api/v1/deals/{dealId}/logs/{logId}",
            "PUT /api/v1/deals/{dealId}/logs/{logId}",
            "DELETE /api/v1/deals/{dealId}/logs/{logId}",
            "GET /api/v1/deals/{dealId}/logs/{logId}/comments",
            "POST /api/v1/deals/{dealId}/logs/{logId}/comments",
            "PUT /api/v1/deals/{dealId}/logs/{logId}/comments/{commentId}",
            "DELETE /api/v1/deals/{dealId}/logs/{logId}/comments/{commentId}",
            "POST /api/v1/deals/{dealId}/logs/{logId}/comments/{commentId}/reply",
            "GET /api/v1/tasks",
            "POST /api/v1/tasks",
            "GET /api/v1/tasks/{id}",
            "PUT /api/v1/tasks/{id}",
            "DELETE /api/v1/tasks/{id}",
            "PUT /api/v1/tasks/{id}/status",
            "GET /api/v1/tasks/stats",
            "GET /api/v1/tasks/{taskId}/subtasks",
            "POST /api/v1/tasks/{taskId}/subtasks",
            "PUT /api/v1/tasks/{taskId}/subtasks/{subTaskId}",
            "DELETE /api/v1/tasks/{taskId}/subtasks/{subTaskId}",
            "GET /api/v1/tasks/{taskId}/comments",
            "POST /api/v1/tasks/{taskId}/comments",
            "PUT /api/v1/tasks/{taskId}/comments/{commentId}",
            "DELETE /api/v1/tasks/{taskId}/comments/{commentId}",
            "POST /api/v1/tasks/{taskId}/comments/{commentId}/reply",
            "GET /api/v1/calendar/events",
            "POST /api/v1/calendar/events",
            "GET /api/v1/calendar/events/{id}",
            "PUT /api/v1/calendar/events/{id}",
            "DELETE /api/v1/calendar/events/{id}",
            "GET /api/v1/dashboard/overview",
            "GET /api/v1/dashboard/top-deals",
            "GET /api/v1/dashboard/agent-stats/{userId}",
            "GET /api/v1/activities",
            "POST /api/v1/activities",
            "GET /api/v1/automations",
            "POST /api/v1/automations",
            "GET /api/v1/automations/{id}",
            "PUT /api/v1/automations/{id}",
            "DELETE /api/v1/automations/{id}",
            "PUT /api/v1/automations/{id}/toggle",
            "GET /api/v1/automations/birthday-configs",
            "GET /api/v1/automations/holiday-configs",
            "GET /api/v1/automations/payment-workflows",
            "GET /api/v1/broadcasts",
            "POST /api/v1/broadcasts",
            "GET /api/v1/broadcasts/{id}",
            "PUT /api/v1/broadcasts/{id}",
            "DELETE /api/v1/broadcasts/{id}",
            "POST /api/v1/broadcasts/{id}/send",
            "POST /api/v1/broadcasts/{id}/schedule",
            "GET /api/v1/broadcasts/recent-activity",
            "GET /api/v1/contacts/segments",
            "GET /api/v1/settings",
            "PUT /api/v1/settings",
            "GET /api/v1/settings/lead-assignment",
            "PUT /api/v1/settings/lead-assignment",
            "GET /api/v1/automations/options",
            "GET /api/v1/invoices",
            "POST /api/v1/invoices",
            "GET /api/v1/invoices/{id}",
            "PUT /api/v1/invoices/{id}",
            "DELETE /api/v1/invoices/{id}",
            "POST /api/v1/invoices/{id}/issue",
            "GET /api/v1/invoices/{id}/pdf",
            "POST /api/v1/invoices/{id}/send",
            "POST /api/v1/invoices/{id}/payments",
            "POST /api/v1/invoices/{id}/void");
  }

  @Test
  void collectionEndpointsUsePageableAndFigmaTablesExposeRequiredFilters() {
    Set<String> pageablePaths =
        Set.of(
            "/api/v1/users",
            "/api/v1/leads",
            "/api/v1/deals",
            "/api/v1/deals/{dealId}/logs",
            "/api/v1/deals/{dealId}/logs/{logId}/comments",
            "/api/v1/tasks",
            "/api/v1/tasks/{taskId}/subtasks",
            "/api/v1/tasks/{taskId}/comments",
            "/api/v1/calendar/events",
            "/api/v1/activities",
            "/api/v1/automations",
            "/api/v1/automations/birthday-configs",
            "/api/v1/automations/holiday-configs",
            "/api/v1/automations/payment-workflows",
            "/api/v1/broadcasts",
            "/api/v1/broadcasts/recent-activity",
            "/api/v1/dashboard/top-deals",
            "/api/v1/invoices");
    Map<String, java.lang.reflect.Method> getMethods = new HashMap<>();
    mappings
        .getHandlerMethods()
        .forEach(
            (info, handler) -> {
              if (!info.getMethodsCondition().getMethods().contains(RequestMethod.GET)) return;
              info.getPatternValues().forEach(path -> getMethods.put(path, handler.getMethod()));
            });
    pageablePaths.forEach(
        path ->
            assertThat(getMethods.get(path).getParameterTypes()).as(path).contains(Pageable.class));

    for (String path : List.of("/api/v1/leads", "/api/v1/deals", "/api/v1/tasks")) {
      Set<String> parameters =
          Arrays.stream(getMethods.get(path).getParameters())
              .map(java.lang.reflect.Parameter::getName)
              .collect(java.util.stream.Collectors.toSet());
      assertThat(parameters).as(path).contains("search", "priority", "assignee", "p");
    }
  }
}
