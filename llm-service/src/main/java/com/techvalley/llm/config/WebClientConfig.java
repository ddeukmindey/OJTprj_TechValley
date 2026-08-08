package com.techvalley.llm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * llm-service KHÔNG có DB riêng cho nghiệp vụ Instance/Alert/Client -
 * nó chỉ tổng hợp dữ liệu (Context) từ 3 service kia qua REST, giống hệt cách
 * monitoring-service đã làm (xem monitoring-service/config/WebClientConfig.java).
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient instanceServiceClient(WebClient.Builder builder,
            @Value("${instance-service.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    public WebClient alertServiceClient(WebClient.Builder builder,
            @Value("${alert-service.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    public WebClient clientServiceClient(WebClient.Builder builder,
            @Value("${client-service.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    /** WebClient riêng, KHÔNG có baseUrl cố định, dùng để gọi ra Gemini/OpenAI API bên ngoài. */
    @Bean
    public WebClient externalAiClient(WebClient.Builder builder) {
        return builder.build();
    }
}
