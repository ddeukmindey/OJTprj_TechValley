package com.techvalley.monitoring.scheduler;

import com.techvalley.monitoring.service.MonitoringService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
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
   * Gắn @SchedulerLock để khi Scale-out multi-container chỉ 1 container duy nhất thực thi.
   */
  @Scheduled(cron = "0 */5 * * * *")
  @SchedulerLock(name = "MonitoringScheduler_runAutoScan", lockAtMostFor = "10m", lockAtLeastFor = "1m")
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
