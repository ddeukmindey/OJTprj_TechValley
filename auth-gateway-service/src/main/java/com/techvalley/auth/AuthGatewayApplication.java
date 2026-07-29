package com.techvalley.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.techvalley.auth.entity")
@EnableJpaRepositories(basePackages = "com.techvalley.auth")
public class AuthGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthGatewayApplication.class, args);
    }
}