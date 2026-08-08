package com.techvalley.llm.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class InstanceServiceClient {
    private final WebClient webClient;

    public InstanceServiceClient(@Value("${services.instance.url}") String instanceUrl) {
        this.webClient = WebClient.builder().baseUrl(instanceUrl).build();
    }

    public String getInstanceInfo(Long id) {
        try {
            return webClient.get()
                    .uri("/api/instances/" + id)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            return "{}"; // Fallback empty json if instance-service is unreachable
        }
    }
}
