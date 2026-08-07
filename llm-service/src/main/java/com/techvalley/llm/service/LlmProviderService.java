package com.techvalley.llm.service;

import com.techvalley.llm.dto.client.AlertDto;
import com.techvalley.llm.dto.client.InstanceDto;
import com.techvalley.llm.dto.response.InstanceDiagnosisResponse;

import java.util.List;

public interface LlmProviderService {
    InstanceDiagnosisResponse diagnoseInstance(InstanceDto instance, List<AlertDto> alerts);
}
