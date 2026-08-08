package com.techvalley.llm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Bind khối cấu hình:
 * llm:
 * provider: gemini | mock
 * api-key: ...
 * model: ...
 * timeout-ms: 8000
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {
    /**
     * "gemini" (mặc định, có fallback tự động) hoặc "mock" (ép luôn dùng
     * rule-based, không tốn API call).
     */
    private String provider = "mock";
    private String apiKey = "";
    private String model = "gemini-flash-latest";
    private long timeoutMs = 8000;
}
