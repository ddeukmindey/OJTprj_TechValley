package com.techvalley.llm.provider.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techvalley.llm.provider.LlmProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fallback rule-based, KHÔNG gọi mạng, KHÔNG cần API Key - luôn sẵn sàng.
 * Đọc lại đúng prompt text (cùng 1 prompt đã gửi cho Gemini) và trích xuất các field
 * bằng regex đơn giản, để 2 provider dùng chung 1 định dạng đầu vào/đầu ra thống nhất
 * (DiagnosisServiceImpl không cần biết provider nào đang chạy).
 */
@Service("mock")
@RequiredArgsConstructor
public class MockLlmProviderServiceImpl implements LlmProviderService {

    private final ObjectMapper objectMapper;

    private static final Pattern STATUS_PATTERN = Pattern.compile("Trạng thái hiện tại: (\\w+)");
    private static final Pattern CPU_PATTERN = Pattern.compile("Tỷ lệ sử dụng CPU: ([\\d.]+)");
    private static final Pattern ALERT_COUNT_PATTERN = Pattern.compile("Số lượng Alert chưa xử lý: (\\d+)");

    @Override
    public String getProviderName() {
        return "MOCK";
    }

    @Override
    public String generate(String prompt) {
        String status = extract(STATUS_PATTERN, prompt, "RUNNING");
        float cpuUsage = parseFloatSafe(extract(CPU_PATTERN, prompt, "0"));
        int unresolvedAlerts = (int) parseFloatSafe(extract(ALERT_COUNT_PATTERN, prompt, "0"));

        int healthScore;
        String diagnosis;
        String rootCause;
        List<String> steps;

        if ("ERROR".equals(status)) {
            healthScore = 20;
            diagnosis = "Instance đang ở trạng thái ERROR, dịch vụ có khả năng đang gián đoạn.";
            rootCause = "Chưa xác định được nguyên nhân chính xác (chế độ offline/rule-based); khả năng cao do crash tiến trình, hết tài nguyên hoặc lỗi kết nối dependency.";
            steps = List.of(
                    "Kiểm tra log ứng dụng và log hệ thống tại thời điểm chuyển sang ERROR.",
                    "Xác minh tình trạng kết nối tới Database/Service phụ thuộc.",
                    "Khởi động lại (restart) instance nếu sự cố không tự phục hồi.",
                    "Nếu lặp lại nhiều lần, cân nhắc rollback về phiên bản deploy gần nhất ổn định."
            );
        } else if (cpuUsage >= 80) {
            healthScore = 45;
            diagnosis = "CPU đang ở mức cao (" + cpuUsage + "%), có nguy cơ nghẽn hiệu năng.";
            rootCause = "Có thể do lưu lượng truy cập tăng đột biến, tiến trình lặp vô hạn, hoặc thiếu tài nguyên so với tải thực tế.";
            steps = List.of(
                    "Theo dõi biểu đồ CPU trong 30-60 phút gần nhất để xác định xu hướng.",
                    "Kiểm tra top process tiêu tốn CPU trên instance.",
                    "Cân nhắc nâng cấp instanceType (SMALL→MEDIUM hoặc MEDIUM→LARGE) nếu tải tăng bền vững.",
                    "Thiết lập auto-scaling hoặc cảnh báo sớm ở ngưỡng 70% để chủ động xử lý."
            );
        } else {
            healthScore = 90;
            diagnosis = "Instance đang hoạt động ổn định, không phát hiện dấu hiệu bất thường rõ rệt.";
            rootCause = "Không có nguyên nhân đáng lo ngại tại thời điểm kiểm tra.";
            steps = List.of(
                    "Tiếp tục theo dõi định kỳ CPU và trạng thái Alert.",
                    "Không cần hành động khắc phục ngay lúc này."
            );
        }

        if (unresolvedAlerts > 0) {
            healthScore = Math.max(0, healthScore - Math.min(20, unresolvedAlerts * 5));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("healthScore", healthScore);
        result.put("diagnosis", diagnosis);
        result.put("rootCause", rootCause);
        result.put("actionableSteps", steps);

        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            // Không thể xảy ra với Map<String,Object> đơn giản, nhưng vẫn xử lý an toàn.
            return "{\"healthScore\":50,\"diagnosis\":\"" + diagnosis + "\",\"rootCause\":\"" + rootCause + "\",\"actionableSteps\":[]}";
        }
    }

    private String extract(Pattern pattern, String text, String defaultValue) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : defaultValue;
    }

    private float parseFloatSafe(String value) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return 0f;
        }
    }
}
