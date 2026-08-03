package com.skytech.crm.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.skytech.crm.enums.Feature;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {
  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void requiredExceptionMappingsUseTheSpecifiedStatusesAndCodes() {
    var missing = handler.notFound(new ResourceNotFoundException("Lead"));
    assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(missing.getBody().error()).isEqualTo("RESOURCE_NOT_FOUND");
    assertThat(missing.getBody().message()).isEqualTo("Lead not found");

    var forbidden = handler.forbidden(new ForbiddenException("Denied"));
    assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(forbidden.getBody().error()).isEqualTo("FORBIDDEN");

    var conflict = handler.conflict(new DataIntegrityViolationException("duplicate"));
    assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(conflict.getBody().error()).isEqualTo("DATA_CONFLICT");
  }

  @Test
  void featureGateResponseContainsUpgradeFields() {
    var response = handler.gated(new FeatureGatedException(Feature.BULK_BROADCAST));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
    assertThat(response.getBody()).isInstanceOf(Map.class);
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertThat(body.get("upgrade_required")).isEqualTo(true);
    assertThat(body.get("feature")).isEqualTo(Feature.BULK_BROADCAST);
  }

  @Test
  void unknownExceptionsReturnGenericInternalError() {
    var response = handler.other(new RuntimeException("sensitive database detail"));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody().error()).isEqualTo("INTERNAL_ERROR");
    assertThat(response.getBody().message()).doesNotContain("sensitive");
  }
}
