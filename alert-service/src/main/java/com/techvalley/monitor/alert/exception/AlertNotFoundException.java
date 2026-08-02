package com.techvalley.monitor.alert.exception;

public class AlertNotFoundException extends RuntimeException {

    public AlertNotFoundException(Long id) {
        super("Không tìm thấy cảnh báo với ID: " + id);
    }
}
