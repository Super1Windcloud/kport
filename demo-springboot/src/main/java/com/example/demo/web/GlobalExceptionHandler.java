package com.example.demo.web;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.superwindcloud.fkill.FkillException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidation(
      MethodArgumentNotValidException exception) {
    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getDefaultMessage())
            .orElse("Validation failed");

    return ResponseEntity.badRequest()
        .body(ApiResponse.failure(message, Map.of("errors", exception.getMessage())));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Map<String, Object>>> handleIllegalArgument(
      IllegalArgumentException exception) {
    HttpStatus status =
        "Invalid admin token".equals(exception.getMessage())
            ? HttpStatus.UNAUTHORIZED
            : HttpStatus.BAD_REQUEST;

    return ResponseEntity.status(status)
        .body(ApiResponse.failure(exception.getMessage(), Map.of()));
  }

  @ExceptionHandler(FkillException.class)
  public ResponseEntity<ApiResponse<Map<String, Object>>> handleFkill(FkillException exception) {
    return ResponseEntity.badRequest()
        .body(ApiResponse.failure(exception.getMessage(), Map.of("errors", exception.errors())));
  }
}
