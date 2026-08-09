package com.techvalley.monitoring.scheduler;

import com.techvalley.monitoring.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "monitoring.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class MonitoringScheduler {

  private final MonitoringService monitoringService;

  /**
   * Tự động quét chỉ số hạ tầng định kỳ (cứ mỗi 5 phút).
   *
   * Chống trùng lặp khi Scale-out Multi-container:
   * - Cấu hình biến môi trường "monitoring.scheduler.enabled=true" cho container
   * PRIMARY.
   * - Các container phụ đặt "monitoring.scheduler.enabled=false" → Bean này không
   * được tạo
   * → Cron Job không chạy trên các container phụ.
   * Không dùng ShedLock vì service này là No-DB (không kết nối trực tiếp vào
   * PostgreSQL).
   */
  @Scheduled(cron = "0 */5 * * * *")
  public void runAutoScan() {
    log.info("⏰ Bắt đầu chạy Cron Job quét tự động chỉ số hạ tầng...");
    try {
      monitoringService.scanAndCheckSystemHealth();
      log.info("✅ Hoàn tất Cron Job quét tự động chỉ số hạ tầng.");
    } catch (Exception e) {
      log.error("Lỗi khi chạy Cron Job quét hạ tầng: {}", e.getMessage());
    }
  }
}
