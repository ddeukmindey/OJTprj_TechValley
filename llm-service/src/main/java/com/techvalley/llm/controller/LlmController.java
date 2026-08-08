package com.techvalley.llm.controller;

import com.techvalley.llm.dto.ApiResponse;
import com.techvalley.llm.dto.InstanceDiagnosisResponse;
import com.techvalley.llm.service.impl.LlmServiceImpl;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/instances")
public class LlmController {

    private final LlmServiceImpl llmService;

    public LlmController(LlmServiceImpl llmService) {
        this.llmService = llmService;
    }

    @GetMapping("/{id}/diagnosis")
    public ApiResponse<InstanceDiagnosisResponse> getDiagnosis(@PathVariable Long id) {
        InstanceDiagnosisResponse result = llmService.diagnoseInstance(id);
        return ApiResponse.success(result, "Phân tích chẩn đoán từ AI hoàn tất");
    }
}
