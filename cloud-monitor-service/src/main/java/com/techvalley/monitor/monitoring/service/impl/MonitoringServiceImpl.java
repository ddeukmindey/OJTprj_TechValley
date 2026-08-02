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
        List<Instance> highCpuInstances = instanceRepository.findByCpuUsageGreaterThanEqual(CPU_WARNING_THRESHOLD);

        for (Instance instance : highCpuInstances) {
            Optional<Alert> existingAlert = alertRepository.findFirstByInstanceIdAndAlertTypeAndIsResolved(
                    instance.getId(), AlertType.CPU_HIGH, 0);

            if (existingAlert.isEmpty()) {
                Alert alert = new Alert();
                alert.setInstanceId(instance.getId());
                alert.setAlertType(AlertType.CPU_HIGH);
                alert.setMessage("Tải CPU cao bất thường: " + instance.getCpuUsage() + "%");
                alert.setIsResolved(0);
                alert.setDetectedAt(LocalDateTime.now());
                alertRepository.save(alert);
            }
        }

        return highCpuInstances.stream()
                .map(instance -> monitoringMapper.toMonitoringInstanceResponse(instance, "Cảnh báo: CPU usage >= 80%"))
                .toList();
    }

    @Override
    @Transactional
    public List<MonitoringInstanceResponse> getErrors() {
        List<Instance> errorInstances = instanceRepository.findByStatus(InstanceStatus.ERROR);

        for (Instance instance : errorInstances) {
            Optional<Alert> existingAlert = alertRepository.findFirstByInstanceIdAndAlertTypeAndIsResolved(
                    instance.getId(), AlertType.ERROR_DETECTED, 0);

            if (existingAlert.isEmpty()) {
                Alert alert = new Alert();
                alert.setInstanceId(instance.getId());
                alert.setAlertType(AlertType.ERROR_DETECTED);
                alert.setMessage("Sự cố máy chủ: Instance đang ở trạng thái ERROR");
                alert.setIsResolved(0);
                alert.setDetectedAt(LocalDateTime.now());
                alertRepository.save(alert);
            }
        }

        return errorInstances.stream()
                .map(instance -> monitoringMapper.toMonitoringInstanceResponse(instance, "Lỗi: Máy chủ đang gặp sự cố (ERROR)"))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonitoringInstanceResponse> getLongStopped() {
        LocalDateTime thresholdDate = LocalDateTime.now().minusHours(LONG_STOPPED_HOURS);
        List<Instance> stoppedInstances = instanceRepository.findByStatus(InstanceStatus.STOPPED);

        List<Instance> longStopped = stoppedInstances.stream()
                .filter(instance -> {
                    LocalDateTime checkTime = instance.getUpdateAt() != null ? instance.getUpdateAt() : instance.getLauncheAt();
                    return checkTime != null && checkTime.isBefore(thresholdDate);
                })
                .toList();

        return longStopped.stream()
                .map(instance -> monitoringMapper.toMonitoringInstanceResponse(instance, "Cảnh báo: Máy chủ bị tạm dừng ít nhất 48 giờ"))
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
}
