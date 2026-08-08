package com.techvalley.llm.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AlertServiceClient {
    private final WebClient webClient;

    public AlertServiceClient(@Value("${services.alert.url}") String alertUrl) {
        this.webClient = WebClient.builder().baseUrl(alertUrl).build();
    }

    public String getAlertsByInstance(Long instanceId) {
        try {
            return webClient.get()
                    .uri("/api/alerts?instanceId=" + instanceId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            return "[]"; // Fallback empty list if alert-service is unreachable
        }
    }
}
