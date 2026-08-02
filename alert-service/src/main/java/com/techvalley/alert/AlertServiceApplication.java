package com.techvalley.alert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.techvalley.alert", "com.techvalley.monitor.alert", "com.techvalley.monitor.common"})
public class AlertServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlertServiceApplication.class, args);
    }
}
