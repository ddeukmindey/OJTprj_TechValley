package com.techvalley.llm.dto;

import java.time.Instant;

public class ApiResponse<T> {
    public boolean success;
    public int code;
    public String message;
    public T data;
    public Instant timestamp = Instant.now();

    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> res = new ApiResponse<>();
        res.success = true;
        res.code = 200;
        res.message = message;
        res.data = data;
        return res;
    }
}
