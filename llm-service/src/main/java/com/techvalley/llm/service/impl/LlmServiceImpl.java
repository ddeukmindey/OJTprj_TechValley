package com.techvalley.llm.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techvalley.llm.client.AlertServiceClient;
import com.techvalley.llm.client.InstanceServiceClient;
import com.techvalley.llm.dto.InstanceDiagnosisResponse;
import com.techvalley.llm.service.LlmProviderService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LlmServiceImpl {

    private final InstanceServiceClient instanceServiceClient;
    private final AlertServiceClient alertServiceClient;
    private final LlmProviderService llmProvider;
    private final ObjectMapper objectMapper;

    public LlmServiceImpl(InstanceServiceClient instanceServiceClient,
                          AlertServiceClient alertServiceClient,
                          @Qualifier("geminiLlmService") LlmProviderService llmProvider) {
        this.instanceServiceClient = instanceServiceClient;
        this.alertServiceClient = alertServiceClient;
        this.llmProvider = llmProvider;
        this.objectMapper = new ObjectMapper();
    }

    public InstanceDiagnosisResponse diagnoseInstance(Long id) {
        String instanceData = instanceServiceClient.getInstanceInfo(id);
        String alertData = alertServiceClient.getAlertsByInstance(id);

        String prompt = "Dữ liệu Instance: " + instanceData + "\\nLịch sử Alert: " + alertData + "\\nHãy chẩn đoán và trả về JSON chuẩn.";
        String aiResultJson = llmProvider.analyze(prompt);

        try {
            InstanceDiagnosisResponse response = objectMapper.readValue(aiResultJson, InstanceDiagnosisResponse.class);
            response.instanceId = id;
            
            // Try to extract instance name if instanceData contains it
            try {
                 if(instanceData.contains("\"name\"") || instanceData.contains("\"instanceName\"")) {
                     var rootNode = objectMapper.readTree(instanceData);
                     if(rootNode.has("data") && rootNode.get("data").has("name")) {
                          response.instanceName = rootNode.get("data").get("name").asText();
                     } else if(rootNode.has("name")) {
                          response.instanceName = rootNode.get("name").asText();
                     }
                 }
            } catch (Exception ignore) {}
            
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi phân tích JSON từ AI");
        }
    }
}
