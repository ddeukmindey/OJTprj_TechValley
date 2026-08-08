package com.techvalley.llm.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.techvalley.llm.service.LlmProviderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service("geminiLlmService")
public class GeminiLlmServiceImpl implements LlmProviderService {

    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;

    public GeminiLlmServiceImpl(
            @Value("${llm.api-key}") String apiKey,
            @Value("${llm.model:gemini-1.5-flash}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String analyze(String prompt) {
        try {
            // Chuẩn bị payload gửi cho Gemini
            ObjectNode requestBody = objectMapper.createObjectNode();
            ObjectNode contents = requestBody.putArray("contents").addObject();
            ObjectNode parts = contents.putArray("parts").addObject();
            
            // Ép Gemini trả về đúng cấu trúc JSON
            String systemInstruction = "Bạn là AI phân tích máy chủ. BẮT BUỘC TRẢ VỀ CHUẨN JSON như sau, không kèm bất kỳ markdown (```json) hay text nào khác: {\"healthScore\": <số>, \"diagnosis\": \"<text>\", \"rootCause\": \"<text>\", \"actionableSteps\": [\"<step1>\"]}. Thông tin cần phân tích: ";
            parts.put("text", systemInstruction + prompt);

            // Cấu hình để Gemini trả về JSON format
            ObjectNode generationConfig = requestBody.putObject("generationConfig");
            generationConfig.put("responseMimeType", "application/json");

            java.net.URI uri = java.net.URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey);

            String response = webClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Bóc tách JSON response từ Gemini
            JsonNode rootNode = objectMapper.readTree(response);
            String aiText = rootNode.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
                    
            return aiText;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi gọi API Gemini: " + e.getMessage());
        }
    }
}
