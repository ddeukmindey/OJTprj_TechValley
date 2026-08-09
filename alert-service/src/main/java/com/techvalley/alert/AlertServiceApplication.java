package com.techvalley.alert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EntityScan("com.techvalley.alert.entity")
@EnableJpaRepositories("com.techvalley.alert.repository")
public class AlertServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlertServiceApplication.runClass(), args);
    }

    private static Class<?> runClass() {
        return AlertServiceApplication.class;
    }
}
