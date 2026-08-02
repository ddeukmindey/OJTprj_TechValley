package com.techvalley.monitor.alert.exception;

public class AlertAlreadyResolvedException extends RuntimeException {

    public AlertAlreadyResolvedException(Long id) {
        super("Cảnh báo ID " + id + " đã được xử lý trước đó.");
    }
}
