package com.techvalley.llm.service.impl;

import com.techvalley.llm.service.LlmProviderService;
import org.springframework.stereotype.Service;

@Service("mockLlmService")
public class MockLlmServiceImpl implements LlmProviderService {
    @Override
    public String analyze(String prompt) {
        return """
        {
          "healthScore": 45,
          "diagnosis": "Máy chủ đang gặp tình trạng nghẽn CPU kéo dài.",
          "rootCause": "Phát hiện Memory Leak trong ứng dụng.",
          "actionableSteps": [
            "1. Kiểm tra log ứng dụng để phát hiện thread bị treo.",
            "2. Khởi động lại container."
          ]
        }
        """;
    }
}
