package com.techvalley.instance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.techvalley.instance", "com.techvalley.monitor.instance", "com.techvalley.monitor.common"})
public class InstanceServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InstanceServiceApplication.class, args);
    }
}
