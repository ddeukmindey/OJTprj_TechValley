package com.techvalley.monitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.techvalley.monitoring", "com.techvalley.monitor.monitoring", "com.techvalley.monitor.instance", "com.techvalley.monitor.alert", "com.techvalley.monitor.client", "com.techvalley.monitor.common"})
public class MonitoringServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MonitoringServiceApplication.class, args);
    }
}
