package com.techvalley.llm.service.impl;

import com.techvalley.llm.dto.client.AlertDto;
import com.techvalley.llm.dto.client.InstanceDto;
import com.techvalley.llm.dto.response.InstanceDiagnosisResponse;
import com.techvalley.llm.service.LlmProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("mockLlmService")
public class MockLlmServiceImpl implements LlmProviderService {

    private static final Logger log = LoggerFactory.getLogger(MockLlmServiceImpl.class);

    @Override
    public InstanceDiagnosisResponse diagnoseInstance(InstanceDto instance, List<AlertDto> alerts) {
        log.info("Thực thi MockLlmServiceImpl (Phân tích chẩn đoán thực tế) cho instanceId={}, status={}", instance.getId(), instance.getStatus());

        String status = instance.getStatus() != null ? instance.getStatus() : "RUNNING";
        double cpu = instance.getCpuUsage() != null ? instance.getCpuUsage() : 0.0;
        int alertCount = alerts != null ? alerts.size() : 0;

        String issueType;
        int healthScore;
        String diagnosis;
        String rootCause;
        List<String> actionableSteps = new ArrayList<>();

        if ("STOPPED".equalsIgnoreCase(status)) {
            issueType = "SYSTEM_ERROR";
            healthScore = 0;
            diagnosis = String.format("[Cảnh báo Ngừng hoạt động] Máy chủ '%s' (ID: %d) đang ở trạng thái STOPPED. Dịch vụ không thể tiếp nhận yêu cầu.", instance.getInstanceName(), instance.getId());
            rootCause = "Tiến trình container bị ngừng đột ngột do lỗi gạt cầu chì (OOMKilled) hoặc lệnh dừng chủ động từ quản trị viên.";
            actionableSteps.add("1. Kiểm tra log hệ thống (`docker logs` / `journalctl`) để tìm nguyên nhân dừng đột ngột.");
            actionableSteps.add("2. Thực hiện khởi động lại máy chủ (Start Instance) từ màn hình quản trị Dashboard.");
            actionableSteps.add("3. Kiểm tra hạn mức bộ nhớ RAM allocation cho tiến trình ứng dụng.");
        } else if ("ERROR".equalsIgnoreCase(status) || cpu >= 90.0) {
            issueType = cpu >= 90.0 ? "HIGH_CPU" : "CRITICAL";
            healthScore = 25;
            diagnosis = String.format("[Sự cố Nghiêm trọng] Máy chủ '%s' (ID: %d) gặp lỗi nguy hiểm với CPU %.1f%% và %d cảnh báo chưa xử lý.", instance.getInstanceName(), instance.getId(), cpu, alertCount);
            rootCause = "Ứng dụng quá tải ngưng trệ thread (Thread Lockup) hoặc gặp hiện tượng HTTP 504 Gateway Timeout do lưu lượng truy cập đột biến.";
            actionableSteps.add("1. Kích hoạt tính năng Auto-scaling hoặc Scale UP cấu hình máy chủ từ " + instance.getInstanceType() + " lên dòng cao hơn.");
            actionableSteps.add("2. Giới hạn lưu lượng truy cập đầu vào (Rate Limiting / Throttling) để giảm tải cho CPU.");
            actionableSteps.add("3. Khởi động lại các worker node bị treo thread.");
        } else if (cpu >= 75.0) {
            issueType = "HIGH_CPU";
            healthScore = 60;
            diagnosis = String.format("[Cảnh báo Tải cao] Máy chủ '%s' (ID: %d) có tỷ lệ CPU %.1f%% tiệm cận ngưỡng quá tải.", instance.getInstanceName(), instance.getId(), cpu);
            rootCause = "Có các truy vấn CSDL chạy chậm (Slow Queries) thiếu index hoặc tiến trình xử lý nền (background worker) chưa tối ưu.";
            actionableSteps.add("1. Phân tích slow query log trong PostgreSQL để tối ưu chỉ mục Index.");
            actionableSteps.add("2. Mở rộng kích thước connection pool cho CSDL.");
            actionableSteps.add("3. Theo dõi sát biểu đồ CPU trong 30 phút tới.");
        } else {
            issueType = "NONE";
            healthScore = 95;
            diagnosis = String.format("[Hoạt động Tốt] Máy chủ '%s' (ID: %d) vận hành ổn định. Tỷ lệ CPU %.1f%%, không có cảnh báo nào chưa xử lý.", instance.getInstanceName(), instance.getId(), cpu);
            rootCause = "Tất cả chỉ số telemetry hạ tầng và ứng dụng đều nằm trong khoảng an toàn cho phép.";
            actionableSteps.add("1. Duy trì cơ chế kiểm tra sức khỏe (Healthcheck) định kỳ.");
            actionableSteps.add("2. Thực hiện sao lưu dữ liệu (Backup) định kỳ theo SLA.");
        }

        return new InstanceDiagnosisResponse(
                instance.getId(),
                instance.getInstanceName(),
                status,
                issueType,
                healthScore,
                diagnosis,
                rootCause,
                actionableSteps,
                true
        );
    }
}
