package com.techvalley.monitor.monitoring.service.impl;

import com.techvalley.monitor.alert.Alert;
import com.techvalley.monitor.alert.repository.AlertRepository;
import com.techvalley.monitor.enums.AlertType;
import com.techvalley.monitor.enums.InstanceStatus;
import com.techvalley.monitor.instance.Instance;
import com.techvalley.monitor.instance.repository.InstanceRepository;
import com.techvalley.monitor.monitoring.dto.response.MonitoringInstanceResponse;
import com.techvalley.monitor.monitoring.dto.response.MonitoringReportResponse;
import com.techvalley.monitor.monitoring.mapper.MonitoringMapper;
import com.techvalley.monitor.monitoring.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MonitoringServiceImpl implements MonitoringService {

    private final InstanceRepository instanceRepository;
    private final AlertRepository alertRepository;
    private final MonitoringMapper monitoringMapper;

    private static final Float CPU_WARNING_THRESHOLD = 80.0f;
    private static final int LONG_STOPPED_HOURS = 48;

    @Override
    @Transactional
    public List<MonitoringInstanceResponse> getWarnings() {
        List<Instance> instances = instanceRepository.findByCpuUsageGreaterThanEqual(CPU_WARNING_THRESHOLD);
        instances.forEach(i -> createAlertIfAbsent(i.getId(), AlertType.CPU_HIGH, "Tải CPU cao bất thường: " + i.getCpuUsage() + "%"));

        return instances.stream()
                .map(i -> monitoringMapper.toMonitoringInstanceResponse(i, "Cảnh báo: CPU usage >= 80%"))
                .toList();
    }

    @Override
    @Transactional
    public List<MonitoringInstanceResponse> getErrors() {
        List<Instance> instances = instanceRepository.findByStatus(InstanceStatus.ERROR);
        instances.forEach(i -> createAlertIfAbsent(i.getId(), AlertType.ERROR_DETECTED, "Sự cố máy chủ: Instance đang ở trạng thái ERROR"));

        return instances.stream()
                .map(i -> monitoringMapper.toMonitoringInstanceResponse(i, "Lỗi: Máy chủ đang gặp sự cố (ERROR)"))
                .toList();
    }

    @Override
    @Transactional
    public List<MonitoringInstanceResponse> getLongStopped() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(LONG_STOPPED_HOURS);

        List<Instance> longStopped = instanceRepository.findByStatus(InstanceStatus.STOPPED).stream()
                .filter(i -> {
                    LocalDateTime checkTime = i.getUpdateAt() != null ? i.getUpdateAt() : i.getLauncheAt();
                    return checkTime != null && checkTime.isBefore(threshold);
                })
                .toList();

        longStopped.forEach(i -> createAlertIfAbsent(i.getId(), AlertType.LONG_STOPPED, "Cảnh báo: Máy chủ ngưng hoạt động kéo dài quá 48 giờ"));

        return longStopped.stream()
                .map(i -> monitoringMapper.toMonitoringInstanceResponse(i, "Cảnh báo: Máy chủ bị tạm dừng ít nhất 48 giờ"))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MonitoringReportResponse getOverviewReport() {
        List<Instance> allInstances = instanceRepository.findAll();
        long totalCount = allInstances.size();

        long runningCount = instanceRepository.countByStatus(InstanceStatus.RUNNING);
        long stoppedCount = instanceRepository.countByStatus(InstanceStatus.STOPPED);
        long errorCount = instanceRepository.countByStatus(InstanceStatus.ERROR);

        double avgCpu = allInstances.stream()
                .filter(i -> i.getCpuUsage() != null)
                .mapToDouble(Instance::getCpuUsage)
                .average()
                .orElse(0.0);

        long unresolvedAlerts = alertRepository.countByIsResolved(0);

        return MonitoringReportResponse.builder()
                .totalInstances(totalCount)
                .runningInstances(runningCount)
                .stoppedInstances(stoppedCount)
                .errorInstances(errorCount)
                .averageCpuUsage(Math.round(avgCpu * 100.0) / 100.0)
                .unresolvedAlerts(unresolvedAlerts)
                .build();
    }

    /** Helper method dùng chung để kiểm tra chống trùng lặp và tạo Alert mới */
    private void createAlertIfAbsent(Long instanceId, AlertType type, String message) {
        Optional<Alert> existing = alertRepository.findFirstByInstanceIdAndAlertTypeAndIsResolved(instanceId, type, 0);
        if (existing.isEmpty()) {
            Alert alert = new Alert();
            alert.setInstanceId(instanceId);
            alert.setAlertType(type);
            alert.setMessage(message);
            alert.setIsResolved(0);
            alert.setDetectedAt(LocalDateTime.now());
            alertRepository.save(alert);
        }
    }
}
