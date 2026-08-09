package com.techvalley.alert.exception;

public class AlertNotFoundException extends RuntimeException {

    public AlertNotFoundException(Long id) {
        super("Không tìm thấy cảnh báo với ID: " + id);
    }

    public AlertNotFoundException(String message) {
        super(message);
    }
}
