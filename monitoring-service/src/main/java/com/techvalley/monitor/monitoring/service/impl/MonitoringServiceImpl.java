package com.techvalley.monitor.monitoring.service.impl;

import com.techvalley.common.security.UserContext;
import com.techvalley.common.security.UserContextInfo;
import com.techvalley.monitor.monitoring.dto.client.ClientDto;
import com.techvalley.monitor.monitoring.dto.client.CreateAlertRequest;
import com.techvalley.monitor.monitoring.dto.client.InstanceDto;
import com.techvalley.monitor.monitoring.dto.response.MonitoringInstanceResponse;
import com.techvalley.monitor.monitoring.dto.response.MonitoringReportResponse;
import com.techvalley.monitor.monitoring.mapper.MonitoringMapper;
import com.techvalley.monitor.monitoring.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringServiceImpl implements MonitoringService {

    @Qualifier("instanceServiceClient")
    private final WebClient instanceServiceClient;
    @Qualifier("alertServiceClient")
    private final WebClient alertServiceClient;
    @Qualifier("clientServiceClient")
    private final WebClient clientServiceClient;
    private final MonitoringMapper monitoringMapper;

    @Value("${monitoring.cpu-warning-threshold:80.0}")
    private Float cpuWarningThreshold;

    @Value("${monitoring.long-stopped-hours:48}")
    private int longStoppedHours;

    // ── Authentication helpers ────────────────────────────────────────────────

    private UserContextInfo validateAndGetUserContext() {
        UserContextInfo user = UserContext.get();
        if (user == null) {
            throw new com.techvalley.monitor.monitoring.exception.AccessDeniedException(
                    "Người dùng chưa được xác thực. Vui lòng cung cấp JWT Token hợp lệ!");
        }
        return user;
    }

    private List<Long> getManagedClientIdsIfManager() {
        UserContextInfo user = validateAndGetUserContext();
        if ("CLIENT_MANAGER".equals(user.getRole())) {
            Long managerId = user.getMemberId();
            List<ClientDto> clients = clientServiceClient.get()
                    .uri("/internal/clients/by-manager/{managerId}", managerId)
                    .retrieve()
                    .bodyToFlux(ClientDto.class)
                    .collectList()
                    .block();
            return clients.stream().map(ClientDto::getId).toList();
        }
        return null; // ADMIN → không lọc theo manager
    }

    private List<InstanceDto> fetchInstances(String path, List<Long> clientIds) {
        return instanceServiceClient.get()
                .uri(uriBuilder -> {
                    var b = uriBuilder.path(path);
                    if (clientIds != null) b.queryParam("clientIds", clientIds);
                    return b.build();
                })
                .retrieve()
                .bodyToFlux(InstanceDto.class)
                .collectList()
                .block();
    }

    // ── Public endpoints ──────────────────────────────────────────────────────

    @Override
    public List<MonitoringInstanceResponse> getWarnings() {
        List<Long> managedClientIds = getManagedClientIdsIfManager();
        if (managedClientIds != null && managedClientIds.isEmpty()) return Collections.emptyList();

        List<InstanceDto> instances = fetchInstances("/internal/instances/high-cpu", managedClientIds);
        instances.forEach(i -> createAlertIfAbsent(i.getId(), "CPU_HIGH",
                "Tải CPU cao bất thường: " + i.getCpuUsage() + "%"));

        return instances.stream()
                .map(i -> monitoringMapper.toMonitoringInstanceResponse(
                        i, "Cảnh báo: CPU usage >= " + cpuWarningThreshold.intValue() + "%"))
                .toList();
    }

    @Override
    public List<MonitoringInstanceResponse> getErrors() {
        List<Long> managedClientIds = getManagedClientIdsIfManager();
        if (managedClientIds != null && managedClientIds.isEmpty()) return Collections.emptyList();

        List<InstanceDto> instances = fetchInstances("/internal/instances/errors", managedClientIds);
        instances.forEach(i -> createAlertIfAbsent(i.getId(), "ERROR_DETECTED",
                "Sự cố máy chủ: Instance đang ở trạng thái ERROR"));

        return instances.stream()
                .map(i -> monitoringMapper.toMonitoringInstanceResponse(
                        i, "Lỗi: Máy chủ đang gặp sự cố (ERROR)"))
                .toList();
    }

    @Override
    public List<MonitoringInstanceResponse> getLongStopped() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(longStoppedHours);
        List<Long> managedClientIds = getManagedClientIdsIfManager();
        if (managedClientIds != null && managedClientIds.isEmpty()) return Collections.emptyList();

        List<InstanceDto> stopped = fetchInstances("/internal/instances/stopped", managedClientIds);
        List<InstanceDto> longStopped = stopped.stream()
                .filter(i -> {
                    LocalDateTime checkTime = i.getUpdateAt() != null ? i.getUpdateAt() : i.getLauncheAt();
                    return checkTime != null && checkTime.isBefore(threshold);
                })
                .toList();

        longStopped.forEach(i -> createAlertIfAbsent(i.getId(), "LONG_STOPPED",
                "Cảnh báo: Máy chủ ngưng hoạt động kéo dài quá " + longStoppedHours + " giờ"));

        return longStopped.stream()
                .map(i -> monitoringMapper.toMonitoringInstanceResponse(
                        i, "Cảnh báo: Máy chủ bị tạm dừng ít nhất " + longStoppedHours + " giờ"))
                .toList();
    }

    @Override
    public MonitoringReportResponse getOverviewReport() {
        List<Long> managedClientIds = getManagedClientIdsIfManager();
        if (managedClientIds != null && managedClientIds.isEmpty()) {
            return MonitoringReportResponse.builder()
                    .totalInstances(0).runningInstances(0).stoppedInstances(0)
                    .errorInstances(0).averageCpuUsage(0.0)
                    .unresolvedAlerts(0).totalClients(0).build();
        }

        // ── Bước 1: Lấy danh sách instance (phải xong trước để có instanceIds) ──
        List<InstanceDto> targetInstances = fetchInstances("/internal/instances", managedClientIds);

        long totalCount    = targetInstances.size();
        long runningCount  = targetInstances.stream().filter(i -> "RUNNING".equals(i.getStatus())).count();
        long stoppedCount  = targetInstances.stream().filter(i -> "STOPPED".equals(i.getStatus())).count();
        long errorCount    = targetInstances.stream().filter(i -> "ERROR".equals(i.getStatus())).count();
        List<Long> instanceIds = targetInstances.stream().map(InstanceDto::getId).toList();

        double avgCpu = targetInstances.stream()
                .filter(i -> i.getCpuUsage() != null)
                .mapToDouble(InstanceDto::getCpuUsage)
                .average().orElse(0.0);

        // ── Bước 2: Gọi song song alert count + client count (độc lập nhau) ──
        Mono<Long> unresolvedAlertsMono = instanceIds.isEmpty()
                ? Mono.just(0L)
                : alertServiceClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/internal/alerts/count-unresolved")
                                .queryParam("instanceIds", instanceIds)
                                .build())
                        .retrieve()
                        .bodyToMono(Long.class)
                        .onErrorReturn(0L);  // alert-service down → fallback 0, không crash report

        Mono<Long> totalClientsMono = managedClientIds != null
                ? Mono.just((long) managedClientIds.size())
                : clientServiceClient.get()
                        .uri("/internal/clients/count")
                        .retrieve()
                        .bodyToMono(Long.class)
                        .onErrorReturn(0L); // client-service down → fallback 0

        // Mono.zip chạy cả 2 Mono song song, chờ cả 2 hoàn thành
        var counts = Mono.zip(unresolvedAlertsMono, totalClientsMono).block();

        return MonitoringReportResponse.builder()
                .totalInstances(totalCount)
                .runningInstances(runningCount)
                .stoppedInstances(stoppedCount)
                .errorInstances(errorCount)
                .averageCpuUsage(Math.round(avgCpu * 100.0) / 100.0)
                .unresolvedAlerts(counts.getT1())
                .totalClients(counts.getT2())
                .build();
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Ghi nhận alert theo kiểu fire-and-forget.
     * Nếu alert-service không phản hồi hoặc trả lỗi:
     *   - Ghi log warning (không mất trace)
     *   - Không ném exception ra ngoài → endpoint /warnings, /errors vẫn trả response bình thường
     */
    private void createAlertIfAbsent(Long instanceId, String type, String message) {
        alertServiceClient.post()
                .uri("/internal/alerts")
                .bodyValue(new CreateAlertRequest(instanceId, type, message))
                .retrieve()
                .toBodilessEntity()
                .doOnError(e -> log.warn(
                        "[MonitoringService] Không thể ghi nhận alert {} cho instance {}: {}",
                        type, instanceId, e.getMessage()))
                .onErrorComplete()  // nuốt lỗi, không propagate lên caller
                .subscribe();       // fire-and-forget: không block calling thread
    }

    // ── System-level scan (dùng cho Scheduler) ───────────────────────────────

    /**
     * Quét toàn bộ hạ tầng và tạo alert nếu cần.
     * Không dùng UserContext / RBAC — chạy với quyền hệ thống.
     * Được gọi bởi MonitoringScheduler mỗi 5 phút.
     */
    @Override
    public void scanAndCreateAlerts() {
        log.info("[Scheduler] Bắt đầu quét tự động sức khỏe hệ thống...");
        int alertCount = 0;

        // ── 1. CPU HIGH: instance đang RUNNING có CPU >= threshold ──
        try {
            List<InstanceDto> highCpuInstances = fetchInstances("/internal/instances/high-cpu", null);
            for (InstanceDto inst : highCpuInstances) {
                createAlertIfAbsent(inst.getId(), "CPU_HIGH",
                        String.format("Tải CPU cao bất thường: %.1f%% (ngưỡng: %.0f%%)",
                                inst.getCpuUsage() != null ? inst.getCpuUsage() : 0f,
                                cpuWarningThreshold));
                alertCount++;
            }
            log.info("[Scheduler] CPU_HIGH: phát hiện {} instance", highCpuInstances.size());
        } catch (Exception e) {
            log.warn("[Scheduler] Không thể quét CPU_HIGH: {}", e.getMessage());
        }

        // ── 2. ERROR: instance đang ở trạng thái ERROR ──
        try {
            List<InstanceDto> errorInstances = fetchInstances("/internal/instances/errors", null);
            for (InstanceDto inst : errorInstances) {
                createAlertIfAbsent(inst.getId(), "ERROR_DETECTED",
                        "Sự cố máy chủ: Instance đang ở trạng thái ERROR");
                alertCount++;
            }
            log.info("[Scheduler] ERROR_DETECTED: phát hiện {} instance", errorInstances.size());
        } catch (Exception e) {
            log.warn("[Scheduler] Không thể quét ERROR: {}", e.getMessage());
        }

        // ── 3. LONG_STOPPED: instance STOPPED quá longStoppedHours giờ ──
        try {
            LocalDateTime threshold = LocalDateTime.now().minusHours(longStoppedHours);
            List<InstanceDto> stoppedInstances = fetchInstances("/internal/instances/stopped", null);
            List<InstanceDto> longStopped = stoppedInstances.stream()
                    .filter(i -> {
                        LocalDateTime checkTime = i.getUpdateAt() != null ? i.getUpdateAt() : i.getLauncheAt();
                        return checkTime != null && checkTime.isBefore(threshold);
                    })
                    .toList();

            for (InstanceDto inst : longStopped) {
                createAlertIfAbsent(inst.getId(), "LONG_STOPPED",
                        String.format("Máy chủ ngưng hoạt động kéo dài quá %d giờ", longStoppedHours));
                alertCount++;
            }
            log.info("[Scheduler] LONG_STOPPED: phát hiện {} instance", longStopped.size());
        } catch (Exception e) {
            log.warn("[Scheduler] Không thể quét LONG_STOPPED: {}", e.getMessage());
        }

        log.info("[Scheduler] Hoàn tất quét tự động. Gửi {} yêu cầu tạo alert.", alertCount);
    }
}