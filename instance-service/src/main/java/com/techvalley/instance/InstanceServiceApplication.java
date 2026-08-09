package com.techvalley.instance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.techvalley.instance.entity")
@EnableJpaRepositories("com.techvalley.instance.repository")
public class InstanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InstanceServiceApplication.class, args);
    }
}
