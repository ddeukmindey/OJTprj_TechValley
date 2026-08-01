package com.techvalley.monitor.instance.exception;

public class InstanceNotFoundException extends RuntimeException {
    public InstanceNotFoundException(Long id) {
        super("Không tìm thấy Instance với ID: " + id);
    }
}
