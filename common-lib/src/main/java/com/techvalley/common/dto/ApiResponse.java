package com.techvalley.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private int code;
    private String message;
    private T data;
    private List<ErrorDetail> errors;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true).code(200).message("Thành công").data(data)
                .timestamp(LocalDateTime.now()).build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true).code(200).message(message).data(data)
                .timestamp(LocalDateTime.now()).build();
    }

    public static <T> ApiResponse<T> success(int code, String message, T data) {
        return ApiResponse.<T>builder()
                .success(true).code(code).message(message).data(data)
                .timestamp(LocalDateTime.now()).build();
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .success(false).code(code).message(message).data(null)
                .timestamp(LocalDateTime.now()).build();
    }

    public static <T> ApiResponse<T> error(int code, String message, List<ErrorDetail> errors) {
        return ApiResponse.<T>builder()
                .success(false).code(code).message(message).errors(errors)
                .timestamp(LocalDateTime.now()).build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail {
        private String field;
        private String message;
    }
}