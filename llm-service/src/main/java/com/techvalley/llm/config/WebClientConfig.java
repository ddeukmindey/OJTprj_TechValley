package com.techvalley.llm.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    private WebClient.Builder timeoutBuilder(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .responseTimeout(java.time.Duration.ofSeconds(5))
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS)));
        return builder.clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    @Bean
    public WebClient instanceServiceClient(WebClient.Builder builder,
            @Value("${instance-service.base-url}") String baseUrl) {
        return timeoutBuilder(builder).baseUrl(baseUrl).build();
    }

    @Bean
    public WebClient alertServiceClient(WebClient.Builder builder,
            @Value("${alert-service.base-url}") String baseUrl) {
        return timeoutBuilder(builder).baseUrl(baseUrl).build();
    }

    @Bean
    public WebClient geminiClient(WebClient.Builder builder) {
        return timeoutBuilder(builder).build();
    }
}