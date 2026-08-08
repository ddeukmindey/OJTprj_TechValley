package com.techvalley.llm.diagnosis.service;

import com.techvalley.llm.diagnosis.dto.response.InstanceDiagnosisResponse;

public interface DiagnosisService {
    /** Chẩn đoán 1 instance: fetch context -> gọi AI -> parse -> trả kết quả. */
    InstanceDiagnosisResponse diagnose(Long instanceId);
}
