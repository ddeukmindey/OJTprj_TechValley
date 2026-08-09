package com.techvalley.auth.exception;

import com.techvalley.auth.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ApiResponse<Object>> handleUnauthorized(
      UnauthorizedException ex) {

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.builder()
            .success(false)
            .message(ex.getMessage())
            .data(null)
            .build());
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiResponse<Object>> handleNotFound(
      ResourceNotFoundException ex) {

    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.builder()
            .success(false)
            .message(ex.getMessage())
            .data(null)
            .build());
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ApiResponse<Object>> handleBadRequest(
      BadRequestException ex) {

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.builder()
            .success(false)
            .message(ex.getMessage())
            .data(null)
            .build());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @SuppressWarnings("null")
  public ResponseEntity<ApiResponse<Object>> handleValidation(
      MethodArgumentNotValidException ex) {

    String message = ex.getBindingResult()
        .getFieldError()
        .getDefaultMessage();

    return ResponseEntity.badRequest()
        .body(ApiResponse.builder()
            .success(false)
            .message(message)
            .data(null)
            .build());
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Object>> handleAccessDenied(
      AccessDeniedException ex) {

    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponse.builder()
            .success(false)
            .message("Access Denied")
            .data(null)
            .build());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Object>> handleException(
      Exception ex) {
    ex.printStackTrace();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.builder()
            .success(false)
            .message("Internal Server Error")
            .data(null)
            .build());
  }
}