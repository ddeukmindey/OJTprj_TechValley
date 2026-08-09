package com.techvalley.alert.exception;

public class AlertAlreadyResolvedException extends RuntimeException {

    public AlertAlreadyResolvedException(Long id) {
        super("Cảnh báo với ID: " + id + " đã được xử lý trước đó!");
    }

    public AlertAlreadyResolvedException(String message) {
        super(message);
    }
}
