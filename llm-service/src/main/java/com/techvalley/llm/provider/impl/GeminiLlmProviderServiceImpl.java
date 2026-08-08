package com.techvalley.llm.provider.impl;

import com.techvalley.llm.config.LlmProperties;
import com.techvalley.llm.diagnosis.exception.LlmException;
import com.techvalley.llm.provider.LlmProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Gọi Gemini API (Google AI Studio - có free tier) theo REST endpoint chuẩn:
 * POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key=API_KEY
 * Cách lấy API Key miễn phí: xem hướng dẫn trong README / phần trả lời kèm theo.
 */
@Service("gemini")
@RequiredArgsConstructor
public class GeminiLlmProviderServiceImpl implements LlmProviderService {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";

    @Qualifier("externalAiClient")
    private final WebClient externalAiClient;
    private final LlmProperties llmProperties;

    @Override
    public String getProviderName() {
        return "GEMINI";
    }

    @Override
    @SuppressWarnings("unchecked")
    public String generate(String prompt) {
        if (llmProperties.getApiKey() == null || llmProperties.getApiKey().isBlank()
                || "dummy_key".equals(llmProperties.getApiKey())) {
            throw new LlmException("Chưa cấu hình LLM_API_KEY hợp lệ cho Gemini");
        }

        String url = BASE_URL + "/" + llmProperties.getModel() + ":generateContent?key=" + llmProperties.getApiKey();

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "responseMimeType", "application/json"
                )
        );

        try {
            Map<String, Object> response = externalAiClient.post()
                    .uri(url)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMillis(llmProperties.getTimeoutMs()))
                    .block();

            return extractText(response);
        } catch (LlmException e) {
            throw e;
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            // In kèm response body thật từ Google (thường chứa lý do cụ thể: model bị deprecate,
            // sai định dạng key, hết quota...) thay vì chỉ có mã lỗi HTTP, để dễ debug hơn.
            throw new LlmException("Lỗi khi gọi Gemini API (" + e.getStatusCode() + "): " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new LlmException("Lỗi khi gọi Gemini API: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            throw new LlmException("Không đọc được nội dung phản hồi từ Gemini (định dạng bất ngờ)", e);
        }
    }
}
