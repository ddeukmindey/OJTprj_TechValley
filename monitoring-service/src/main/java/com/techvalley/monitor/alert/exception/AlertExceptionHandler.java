package com.techvalley.monitor.alert.exception;

import com.techvalley.monitor.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Chỉ bắt exception phát sinh từ controller trong package alert,
 * tránh xung đột với Global Exception Handler chung mà các module khác
 * (instance, client, cost, ...) có thể tự khai báo sau này.
 */
@RestControllerAdvice(basePackages = "com.techvalley.monitor.alert.controller")
public class AlertExceptionHandler {

    @ExceptionHandler(AlertNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(AlertNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(AlertAlreadyResolvedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAlreadyResolved(AlertAlreadyResolvedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String field = ex.getName();
        String message = "Tham số '" + field + "' không hợp lệ.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        HttpStatus.BAD_REQUEST.value(),
                        "Thông tin đầu vào không hợp lệ",
                        List.of(new ApiResponse.ErrorDetail(field, message))
                ));
    }
}
