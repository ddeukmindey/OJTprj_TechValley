package com.techvalley.llm.service;

import com.techvalley.llm.dto.external.AlertDto;
import com.techvalley.llm.dto.external.InstanceDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptBuilder {

    public String buildDiagnosisPrompt(InstanceDto instance, List<AlertDto> alertHistory) {
        long unresolvedCount = alertHistory.stream().filter(a -> a.getIsResolved() == 0).count();

        return """
                Bạn là chuyên gia vận hành hệ thống Cloud (SRE) cho công ty TechValley.
                Hãy phân tích tình trạng của một máy chủ ảo (instance) dựa trên dữ liệu sau và trả lời NGẮN GỌN, đúng cấu trúc JSON yêu cầu — không thêm giải thích ngoài JSON.

                Dữ liệu instance:
                - Tên: %s
                - Khu vực: %s
                - Loại: %s
                - Trạng thái hiện tại: %s
                - CPU usage: %s%%

                Lịch sử cảnh báo liên quan: %d alert, trong đó %d alert CHƯA xử lý.

                Trả về đúng định dạng JSON sau (không thêm markdown, không thêm text khác):
                {
                  "cause": "<nguyên nhân khả dĩ nhất, 1 câu ngắn gọn bằng tiếng Việt>",
                  "recommendedAction": "<hành động cụ thể nên làm, 1 câu ngắn gọn bằng tiếng Việt>",
                  "severity": "<một trong: LOW, MEDIUM, HIGH, CRITICAL>"
                }
                """.formatted(
                instance.getInstanceName(), instance.getRegion(), instance.getInstanceType(),
                instance.getStatus(), instance.getCpuUsage(),
                alertHistory.size(), unresolvedCount
        );
    }
}