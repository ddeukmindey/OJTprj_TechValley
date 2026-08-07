package com.techvalley.llm.service;

import com.techvalley.llm.dto.response.InstanceDiagnosisResponse;

public interface LlmService {
    InstanceDiagnosisResponse diagnoseInstance(Long instanceId);
}
