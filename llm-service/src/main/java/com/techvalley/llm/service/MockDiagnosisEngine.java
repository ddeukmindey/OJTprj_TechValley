package com.techvalley.llm.service;

import com.techvalley.llm.dto.external.AlertDto;
import com.techvalley.llm.dto.external.InstanceDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MockDiagnosisEngine {

    public String[] diagnose(InstanceDto instance, List<AlertDto> alertHistory) {
        long unresolvedCount = alertHistory.stream().filter(a -> a.getIsResolved() == 0).count();

        String cause;
        String action;
        String severity;

        if ("ERROR".equals(instance.getStatus())) {
            if (unresolvedCount >= 3) {
                cause = "Instance gặp lỗi lặp lại nhiều lần, khả năng cao do cấu hình sai hoặc tài nguyên không đủ ổn định lâu dài.";
                action = "Kiểm tra log hệ thống chi tiết, cân nhắc khởi động lại instance hoặc nâng cấp cấu hình (instanceType).";
                severity = "CRITICAL";
            } else {
                cause = "Instance đang ở trạng thái lỗi, có thể do sự cố tạm thời của dịch vụ hoặc mất kết nối.";
                action = "Kiểm tra log gần nhất và thử khởi động lại instance. Theo dõi thêm nếu lỗi tái diễn.";
                severity = "HIGH";
            }
        } else if (instance.getCpuUsage() != null && instance.getCpuUsage() >= 80) {
            cause = "CPU đang ở mức cao, có nguy cơ ảnh hưởng hiệu năng nếu kéo dài.";
            action = "Theo dõi thêm workload hiện tại, cân nhắc nâng cấp lên instanceType lớn hơn nếu tình trạng kéo dài.";
            severity = "MEDIUM";
        } else {
            cause = "Instance đang hoạt động ổn định, không phát hiện dấu hiệu bất thường.";
            action = "Không cần hành động khẩn cấp. Tiếp tục theo dõi định kỳ.";
            severity = "LOW";
        }

        return new String[]{cause, action, severity};
    }
}