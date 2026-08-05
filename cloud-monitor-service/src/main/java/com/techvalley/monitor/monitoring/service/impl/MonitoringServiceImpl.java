package com.techvalley.monitor.monitoring.service.impl;

import com.techvalley.monitor.alert.Alert;
import com.techvalley.monitor.alert.repository.AlertRepository;
import com.techvalley.monitor.client.Client;
import com.techvalley.monitor.client.repository.ClientRepository;
import com.techvalley.monitor.common.security.UserContext;
import com.techvalley.monitor.common.security.UserContextInfo;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MonitoringServiceImpl implements MonitoringService {

    private final InstanceRepository instanceRepository;
    private final AlertRepository alertRepository;
    private final ClientRepository clientRepository;
    private final MonitoringMapper monitoringMapper;

    private static final Float CPU_WARNING_THRESHOLD = 80.0f;
    private static final int LONG_STOPPED_HOURS = 48;

    private List<Long> getManagedClientIdsIfManager() {
        UserContextInfo user = UserContext.get();
        if (user != null && "CLIENT_MANAGER".equals(user.getRole())) {
            Long managerId = user.getMemberId();
            List<Client> clients = clientRepository.findByManagerId(managerId);
            return clients.stream().map(Client::getId).toList();
        }
        return null;
    }

    @Override
    @Transactional
    public List<MonitoringInstanceResponse> getWarnings() {
        List<Long> managedClientIds = getManagedClientIdsIfManager();
        List<Instance> instances;
        if (managedClientIds != null) {
            if (managedClientIds.isEmpty()) return Collections.emptyList();
            instances = instanceRepository.findByClientIdInAndCpuUsageGreaterThanEqual(managedClientIds, CPU_WARNING_THRESHOLD);
        } else {
            instances = instanceRepository.findByCpuUsageGreaterThanEqual(CPU_WARNING_THRESHOLD);
        }
        instances.forEach(i -> createAlertIfAbsent(i.getId(), AlertType.CPU_HIGH, "Tải CPU cao bất thường: " + i.getCpuUsage() + "%"));

        return instances.stream()
                .map(i -> monitoringMapper.toMonitoringInstanceResponse(i, "Cảnh báo: CPU usage >= 80%"))
                .toList();
    }

    @Override
    @Transactional
    public List<MonitoringInstanceResponse> getErrors() {
        List<Long> managedClientIds = getManagedClientIdsIfManager();
        List<Instance> instances;
        if (managedClientIds != null) {
            if (managedClientIds.isEmpty()) return Collections.emptyList();
            instances = instanceRepository.findByClientIdInAndStatus(managedClientIds, InstanceStatus.ERROR);
        } else {
            instances = instanceRepository.findByStatus(InstanceStatus.ERROR);
        }
        instances.forEach(i -> createAlertIfAbsent(i.getId(), AlertType.ERROR_DETECTED, "Sự cố máy chủ: Instance đang ở trạng thái ERROR"));

        return instances.stream()
                .map(i -> monitoringMapper.toMonitoringInstanceResponse(i, "Lỗi: Máy chủ đang gặp sự cố (ERROR)"))
                .toList();
    }

    @Override
    @Transactional
    public List<MonitoringInstanceResponse> getLongStopped() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(LONG_STOPPED_HOURS);
        List<Long> managedClientIds = getManagedClientIdsIfManager();

        List<Instance> baseList;
        if (managedClientIds != null) {
            if (managedClientIds.isEmpty()) return Collections.emptyList();
            baseList = instanceRepository.findByClientIdInAndStatus(managedClientIds, InstanceStatus.STOPPED);
        } else {
            baseList = instanceRepository.findByStatus(InstanceStatus.STOPPED);
        }

        List<Instance> longStopped = baseList.stream()
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
        List<Long> managedClientIds = getManagedClientIdsIfManager();

        List<Instance> targetInstances;
        long totalCount;
        long runningCount;
        long stoppedCount;
        long errorCount;
        long unresolvedAlerts;
        long totalClients;

        if (managedClientIds != null) {
            if (managedClientIds.isEmpty()) {
                return MonitoringReportResponse.builder()
                        .totalInstances(0)
                        .runningInstances(0)
                        .stoppedInstances(0)
                        .errorInstances(0)
                        .averageCpuUsage(0.0)
                        .unresolvedAlerts(0)
                        .totalClients(0)
                        .build();
            }
            targetInstances = instanceRepository.findByClientIdIn(managedClientIds);
            totalCount = targetInstances.size();
            runningCount = instanceRepository.countByClientIdInAndStatus(managedClientIds, InstanceStatus.RUNNING);
            stoppedCount = instanceRepository.countByClientIdInAndStatus(managedClientIds, InstanceStatus.STOPPED);
            errorCount = instanceRepository.countByClientIdInAndStatus(managedClientIds, InstanceStatus.ERROR);

            List<Long> instanceIds = targetInstances.stream().map(Instance::getId).toList();
            unresolvedAlerts = instanceIds.isEmpty() ? 0 : alertRepository.countByInstanceIdInAndIsResolved(instanceIds, 0);
            totalClients = managedClientIds.size();
        } else {
            targetInstances = instanceRepository.findAll();
            totalCount = targetInstances.size();
            runningCount = instanceRepository.countByStatus(InstanceStatus.RUNNING);
            stoppedCount = instanceRepository.countByStatus(InstanceStatus.STOPPED);
            errorCount = instanceRepository.countByStatus(InstanceStatus.ERROR);
            unresolvedAlerts = alertRepository.countByIsResolved(0);
            totalClients = clientRepository.count();
        }

        double avgCpu = targetInstances.stream()
                .filter(i -> i.getCpuUsage() != null)
                .mapToDouble(Instance::getCpuUsage)
                .average()
                .orElse(0.0);

        return MonitoringReportResponse.builder()
                .totalInstances(totalCount)
                .runningInstances(runningCount)
                .stoppedInstances(stoppedCount)
                .errorInstances(errorCount)
                .averageCpuUsage(Math.round(avgCpu * 100.0) / 100.0)
                .unresolvedAlerts(unresolvedAlerts)
                .totalClients(totalClients)
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
