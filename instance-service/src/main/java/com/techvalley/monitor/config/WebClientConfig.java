package com.techvalley.monitor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient clientServiceClient(WebClient.Builder builder,
            @Value("${client-service.base-url}") String baseUrl,
            @Value("${internal.api-key}") String internalApiKey) {
        return builder.baseUrl(baseUrl)
        .defaultHeader("X-Internal-Key", internalApiKey)
        .build();
    }
}