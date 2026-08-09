package com.techvalley.llm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiClientService {

    @Qualifier("geminiClient")
    private final WebClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${llm.gemini.api-key}")
    private String apiKey;
    @Value("${llm.gemini.api-url}")
    private String apiUrl;

    public String[] callGemini(String prompt) throws Exception {
        Map<String, Object> body = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{Map.of("text", prompt)})
                }
        );

        String rawResponse = geminiClient.post()
                .uri(apiUrl + "?key=" + apiKey)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonNode root = objectMapper.readTree(rawResponse);
        String text = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

        // Loại bỏ markdown code fence nếu Gemini trả kèm ```json ... ```
        String cleanJson = text.replaceAll("```json|```", "").trim();
        JsonNode diagnosis = objectMapper.readTree(cleanJson);

        return new String[]{
                diagnosis.path("cause").asText(),
                diagnosis.path("recommendedAction").asText(),
                diagnosis.path("severity").asText()
        };
    }
}