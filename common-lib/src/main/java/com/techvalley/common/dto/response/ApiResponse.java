package com.techvalley.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Chuẩn API Envelope dùng chung cho toàn bộ 5 microservices nghiệp vụ:
 * instance-service, client-service, monitoring-service, alert-service, llm-service.
 *
 * Cấu trúc JSON trả về:
 * {
 *   "success": true/false,
 *   "code": 200/400/401/403/404/500,
 *   "message": "...",
 *   "data": {...},
 *   "timestamp": "2026-08-09T22:00:00"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private int code;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(200)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
