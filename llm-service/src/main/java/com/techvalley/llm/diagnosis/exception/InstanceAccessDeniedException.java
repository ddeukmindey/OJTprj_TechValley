package com.techvalley.llm.diagnosis.exception;

/** Ném ra khi CLIENT_MANAGER cố chẩn đoán 1 instance KHÔNG thuộc client mà họ quản lý. */
public class InstanceAccessDeniedException extends RuntimeException {
    public InstanceAccessDeniedException(String message) {
        super(message);
    }
}
