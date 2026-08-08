package com.techvalley.llm.common.exception;

import com.techvalley.llm.common.dto.ApiResponse;
import com.techvalley.llm.diagnosis.exception.InstanceAccessDeniedException;
import com.techvalley.llm.diagnosis.exception.InstanceNotFoundException;
import com.techvalley.llm.diagnosis.exception.LlmException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InstanceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(InstanceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, ex.getMessage()));
    }

    @ExceptionHandler(InstanceAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(InstanceAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(403, ex.getMessage()));
    }

    @ExceptionHandler(LlmException.class)
    public ResponseEntity<ApiResponse<Void>> handleLlmException(LlmException ex) {
        // Về nguyên tắc DiagnosisServiceImpl luôn tự fallback sang Mock trước khi lỗi này lộ ra ngoài,
        // nên nếu người dùng thấy lỗi 502 này nghĩa là cả provider thật lẫn Mock đều hỏng.
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error(502, "Không thể tạo chẩn đoán AI: " + ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            errors.put(fieldName, error.getDefaultMessage());
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .code(400)
                        .message("Dữ liệu đầu vào không hợp lệ")
                        .data(errors)
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Đã xảy ra lỗi hệ thống: " + ex.getMessage()));
    }
}
