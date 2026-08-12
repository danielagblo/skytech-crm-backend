package com.skytech.crm.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.validation.ConstraintViolationException;
import java.time.*;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  @JsonInclude(JsonInclude.Include.NON_NULL)
  record ErrorBody(
      boolean success, String error, String message, Object details, OffsetDateTime timestamp) {}

  private static final PropertyNamingStrategies.SnakeCaseStrategy SNAKE_CASE =
      new PropertyNamingStrategies.SnakeCaseStrategy();

  private ResponseEntity<ErrorBody> error(
      HttpStatus status, String code, String message, Object details) {
    return ResponseEntity.status(status)
        .body(new ErrorBody(false, code, message, details, OffsetDateTime.now()));
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  ResponseEntity<ErrorBody> notFound(ResourceNotFoundException e) {
    return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", e.getMessage(), null);
  }

  @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
  ResponseEntity<ErrorBody> forbidden(Exception e) {
    return error(HttpStatus.FORBIDDEN, "FORBIDDEN", e.getMessage(), null);
  }

  @ExceptionHandler(FeatureGatedException.class)
  ResponseEntity<?> gated(FeatureGatedException e) {
    return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
        .body(
            Map.of(
                "success",
                false,
                "error",
                "FEATURE_GATED",
                "message",
                e.getMessage(),
                "upgrade_required",
                true,
                "feature",
                e.getFeature(),
                "timestamp",
                OffsetDateTime.now()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErrorBody> invalid(MethodArgumentNotValidException e) {
    Map<String, String> fields = new LinkedHashMap<>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(x -> fields.put(SNAKE_CASE.translate(x.getField()), x.getDefaultMessage()));
    return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", fields);
  }

  @ExceptionHandler(BindException.class)
  ResponseEntity<ErrorBody> binding(BindException e) {
    Map<String, String> fields = new LinkedHashMap<>();
    e.getFieldErrors()
        .forEach(x -> fields.put(SNAKE_CASE.translate(x.getField()), x.getDefaultMessage()));
    return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", fields);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ErrorBody> constraints(ConstraintViolationException e) {
    Map<String, String> fields = new LinkedHashMap<>();
    e.getConstraintViolations()
        .forEach(
            violation ->
                fields.put(
                    SNAKE_CASE.translate(violation.getPropertyPath().toString()),
                    violation.getMessage()));
    return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", fields);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  ResponseEntity<ErrorBody> methodValidation(HandlerMethodValidationException e) {
    return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", null);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ErrorBody> conflict(DataIntegrityViolationException e) {
    log.warn("Database constraint conflict: {}", rootMessage(e));
    return error(
        HttpStatus.CONFLICT, "DATA_CONFLICT", "The operation conflicts with existing data", null);
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  ResponseEntity<ErrorBody> optimistic(ObjectOptimisticLockingFailureException e) {
    return error(
        HttpStatus.CONFLICT,
        "CONCURRENT_MODIFICATION",
        "The resource was changed by another request; reload and try again",
        null);
  }

  @ExceptionHandler({
    MethodArgumentTypeMismatchException.class,
    HttpMessageNotReadableException.class,
    MissingServletRequestParameterException.class,
    MultipartException.class
  })
  ResponseEntity<ErrorBody> malformed(Exception e) {
    return error(
        HttpStatus.BAD_REQUEST,
        "MALFORMED_REQUEST",
        "A path, query, or body value has an invalid format",
        null);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ErrorBody> methodNotAllowed(HttpRequestMethodNotSupportedException e) {
    return error(
        HttpStatus.METHOD_NOT_ALLOWED,
        "METHOD_NOT_ALLOWED",
        "The HTTP method is not supported for this endpoint",
        null);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<ErrorBody> mediaType(HttpMediaTypeNotSupportedException e) {
    return error(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        "UNSUPPORTED_MEDIA_TYPE",
        "The request content type is not supported",
        null);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ErrorBody> routeNotFound(NoResourceFoundException e) {
    return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Endpoint not found", null);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<ErrorBody> upload(MaxUploadSizeExceededException e) {
    return error(
        HttpStatus.PAYLOAD_TOO_LARGE,
        "FILE_TOO_LARGE",
        "The uploaded file exceeds the 10 MB limit",
        null);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ErrorBody> bad(IllegalArgumentException e) {
    return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage(), null);
  }

  private String rootMessage(Throwable error) {
    Throwable root = error;
    while (root.getCause() != null && root.getCause() != root) root = root.getCause();
    return Optional.ofNullable(root.getMessage()).orElse(root.getClass().getSimpleName());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorBody> other(Exception e) {
    log.error("Unhandled request error", e);
    return error(
        HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", null);
  }
}
