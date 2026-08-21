package com.techvalley.monitor.monitoring.scheduler;

import com.techvalley.monitor.monitoring.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cron Job tự động quét sức khỏe toàn bộ hạ tầng định kỳ.
 *
 * Hoạt động theo flow SYSTEM-LEVEL (không cần user trigger):
 *   1. Lấy toàn bộ instance từ instance-service (không lọc theo RBAC)
 *   2. Phát hiện bất thường (CPU_HIGH / ERROR_DETECTED / LONG_STOPPED)
 *   3. Gọi alert-service tạo alert — cơ chế dedup tại alert-service đảm bảo không trùng
 *
 * Anti-duplicate khi scale-out (nhiều container):
 *   - Container PRIMARY: MONITORING_SCHEDULER_ENABLED=true  → Bean được tạo, Cron chạy
 *   - Container phụ   : MONITORING_SCHEDULER_ENABLED=false → Bean không được tạo (@ConditionalOnProperty)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "monitoring.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class MonitoringScheduler {

    private final MonitoringService monitoringService;

    /**
     * Tự động quét chỉ số hạ tầng định kỳ.
     * Mặc định: mỗi 5 phút — có thể ghi đè qua biến môi trường MONITORING_SCHEDULER_CRON.
     *
     * Ví dụ: MONITORING_SCHEDULER_CRON="0 * * * * *" → quét mỗi 1 phút (cho môi trường dev/demo)
     */
    @Scheduled(cron = "${monitoring.scheduler.cron:0 */5 * * * *}")
    public void runAutoScan() {
        log.info("⏰ [MonitoringScheduler] Bắt đầu chu kỳ quét tự động...");
        try {
            monitoringService.scanAndCreateAlerts();
            log.info("✅ [MonitoringScheduler] Hoàn tất chu kỳ quét tự động.");
        } catch (Exception e) {
            // Catch-all: đảm bảo lỗi không làm tắt Scheduler thread của Spring
            log.error("❌ [MonitoringScheduler] Lỗi không mong muốn trong chu kỳ quét: {}", e.getMessage());
        }
    }
}
