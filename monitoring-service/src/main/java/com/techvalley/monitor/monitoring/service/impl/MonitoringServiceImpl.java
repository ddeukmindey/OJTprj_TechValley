package com.techvalley.monitor.monitoring.service.impl;

import com.techvalley.monitor.common.security.UserContext;
import com.techvalley.monitor.common.security.UserContextInfo;
import com.techvalley.monitor.monitoring.dto.client.ClientDto;
import com.techvalley.monitor.monitoring.dto.client.CreateAlertRequest;
import com.techvalley.monitor.monitoring.dto.client.InstanceDto;
import com.techvalley.monitor.monitoring.dto.response.MonitoringInstanceResponse;
import com.techvalley.monitor.monitoring.dto.response.MonitoringReportResponse;
import com.techvalley.monitor.monitoring.mapper.MonitoringMapper;
import com.techvalley.monitor.monitoring.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

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

    private static final Float CPU_WARNING_THRESHOLD = 80.0f;
    private static final int LONG_STOPPED_HOURS = 48;

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
        return null;
    }

    private List<InstanceDto> fetchInstances(String path, List<Long> clientIds, Object... extraParams) {
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

    @Override
    public List<MonitoringInstanceResponse> getWarnings() {
        List<Long> managedClientIds = getManagedClientIdsIfManager();
        if (managedClientIds != null && managedClientIds.isEmpty()) return Collections.emptyList();

        List<InstanceDto> instances = fetchInstances("/internal/instances/high-cpu", managedClientIds);
        instances.forEach(i -> createAlertIfAbsent(i.getId(), "CPU_HIGH",
                "Tải CPU cao bất thường: " + i.getCpuUsage() + "%"));

        return instances.stream()
                .map(i -> monitoringMapper.toMonitoringInstanceResponse(i, "Cảnh báo: CPU usage >= 80%"))
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
                .map(i -> monitoringMapper.toMonitoringInstanceResponse(i, "Lỗi: Máy chủ đang gặp sự cố (ERROR)"))
                .toList();
    }

    @Override
    public List<MonitoringInstanceResponse> getLongStopped() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(LONG_STOPPED_HOURS);
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
                "Cảnh báo: Máy chủ ngưng hoạt động kéo dài quá 48 giờ"));

        return longStopped.stream()
                .map(i -> monitoringMapper.toMonitoringInstanceResponse(i, "Cảnh báo: Máy chủ bị tạm dừng ít nhất 48 giờ"))
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

        List<InstanceDto> targetInstances = fetchInstances("/internal/instances", managedClientIds);

        long totalCount = targetInstances.size();
        long runningCount = targetInstances.stream().filter(i -> "RUNNING".equals(i.getStatus())).count();
        long stoppedCount = targetInstances.stream().filter(i -> "STOPPED".equals(i.getStatus())).count();
        long errorCount = targetInstances.stream().filter(i -> "ERROR".equals(i.getStatus())).count();

        List<Long> instanceIds = targetInstances.stream().map(InstanceDto::getId).toList();
        long unresolvedAlerts = instanceIds.isEmpty() ? 0 : alertServiceClient.get()
                .uri(uriBuilder -> uriBuilder.path("/internal/alerts/count-unresolved")
                        .queryParam("instanceIds", instanceIds).build())
                .retrieve()
                .bodyToMono(Long.class)
                .block();

        long totalClients = managedClientIds != null ? managedClientIds.size()
                : clientServiceClient.get().uri("/internal/clients/count")
                    .retrieve().bodyToMono(Long.class).block();

        double avgCpu = targetInstances.stream()
                .filter(i -> i.getCpuUsage() != null)
                .mapToDouble(InstanceDto::getCpuUsage)
                .average().orElse(0.0);

        return MonitoringReportResponse.builder()
                .totalInstances(totalCount).runningInstances(runningCount)
                .stoppedInstances(stoppedCount).errorInstances(errorCount)
                .averageCpuUsage(Math.round(avgCpu * 100.0) / 100.0)
                .unresolvedAlerts(unresolvedAlerts).totalClients(totalClients)
                .build();
    }

    private void createAlertIfAbsent(Long instanceId, String type, String message) {
        alertServiceClient.post()
                .uri("/internal/alerts")
                .bodyValue(new CreateAlertRequest(instanceId, type, message))
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}