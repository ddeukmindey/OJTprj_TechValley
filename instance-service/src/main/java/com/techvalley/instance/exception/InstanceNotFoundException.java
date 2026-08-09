package com.techvalley.instance.exception;

public class InstanceNotFoundException extends RuntimeException {

    public InstanceNotFoundException(Long id) {
        super("Không tìm thấy Máy chủ ảo với ID: " + id);
    }

    public InstanceNotFoundException(String message) {
        super(message);
    }
}
