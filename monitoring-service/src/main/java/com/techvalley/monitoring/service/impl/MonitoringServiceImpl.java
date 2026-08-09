package com.techvalley.monitoring.service.impl;

import com.techvalley.monitoring.enums.AlertType;
import com.techvalley.monitoring.enums.InstanceStatus;
import com.techvalley.monitoring.client.AlertServiceClient;
import com.techvalley.monitoring.client.ClientServiceClient;
import com.techvalley.monitoring.client.InstanceServiceClient;
import com.techvalley.monitoring.dto.client.InstanceDto;
import com.techvalley.monitoring.dto.response.MonitoringInstanceResponse;
import com.techvalley.monitoring.dto.response.MonitoringReportResponse;
import com.techvalley.monitoring.exception.AccessDeniedException;
import com.techvalley.monitoring.mapper.MonitoringMapper;
import com.techvalley.monitoring.security.UserContext;
import com.techvalley.monitoring.security.UserContextInfo;
import com.techvalley.monitoring.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringServiceImpl implements MonitoringService {

  private final InstanceServiceClient instanceServiceClient;
  private final AlertServiceClient alertServiceClient;
  private final ClientServiceClient clientServiceClient;
  private final MonitoringMapper monitoringMapper;

  private List<InstanceDto> fetchInstancesWithRbacFilter(Long clientId) {
    UserContextInfo user = UserContext.get();
    if (user == null) {
      throw new AccessDeniedException("Người dùng chưa được xác thực");
    }

    List<InstanceDto> allInstances;
    if (clientId != null) {
      if ("CLIENT_MANAGER".equals(user.getRole())) {
        boolean isOwner = clientServiceClient.checkClientOwnership(clientId, user.getMemberId());
        if (!isOwner) {
          throw new AccessDeniedException(
              "Bạn không có quyền truy cập thông tin giám sát của khách hàng ID: " + clientId);
        }
      }
      allInstances = instanceServiceClient.getInstancesByClientId(clientId);
    } else {
      if ("CLIENT_MANAGER".equals(user.getRole())) {
        List<Long> managedClientIds = clientServiceClient.getClientIdsByManagerId(user.getMemberId());
        if (managedClientIds.isEmpty()) {
          return List.of();
        }
        allInstances = new ArrayList<>();
        for (Long cId : managedClientIds) {
          allInstances.addAll(instanceServiceClient.getInstancesByClientId(cId));
        }
      } else {
        allInstances = instanceServiceClient.getAllInstances();
      }
    }
    return allInstances;
  }

  @Override
  public List<MonitoringInstanceResponse> getMonitoredInstances(Long clientId, InstanceStatus status,
      Float cpuThreshold) {
    List<InstanceDto> instances = fetchInstancesWithRbacFilter(clientId);

    return instances.stream()
        .filter(inst -> status == null || status.equals(inst.getStatus()))
        .filter(inst -> cpuThreshold == null || (inst.getCpuUsage() != null && inst.getCpuUsage() >= cpuThreshold))
        .map(inst -> {
          String warningMsg = null;
          if (inst.getCpuUsage() != null && inst.getCpuUsage() >= 80.0f) {
            warningMsg = String.format("CẢNH BÁO: Máy chủ %s đang bị quá tải CPU (%.1f%%)!", inst.getInstanceName(),
                inst.getCpuUsage());
          }
          return monitoringMapper.toMonitoringInstanceResponse(inst, warningMsg);
        })
        .collect(Collectors.toList());
  }

  @Override
  public MonitoringReportResponse getMonitoringReport(Long clientId) {
    List<InstanceDto> instances = fetchInstancesWithRbacFilter(clientId);

    long totalCount = instances.size();
    long runningCount = 0;
    long stoppedCount = 0;
    long errorCount = 0;
    double totalCpu = 0.0;
    int cpuCount = 0;

    for (InstanceDto inst : instances) {
      if (InstanceStatus.RUNNING.equals(inst.getStatus())) {
        runningCount++;
      } else if (InstanceStatus.STOPPED.equals(inst.getStatus())) {
        stoppedCount++;
      } else if (InstanceStatus.ERROR.equals(inst.getStatus())) {
        errorCount++;
      }

      if (inst.getCpuUsage() != null) {
        totalCpu += inst.getCpuUsage();
        cpuCount++;
      }
    }

    double averageCpu = cpuCount > 0 ? totalCpu / cpuCount : 0.0;

    return MonitoringReportResponse.builder()
        .totalInstances(totalCount)
        .runningInstances(runningCount)
        .stoppedInstances(stoppedCount)
        .errorInstances(errorCount)
        .averageCpuUsage(Math.round(averageCpu * 100.0) / 100.0)
        .unresolvedAlerts(errorCount)
        .totalClients(clientId != null ? 1 : clientServiceClient.getAllClients().size())
        .build();
  }

  @Override
  public void scanAndCheckSystemHealth() {
    log.info("Bắt đầu tiến trình tự động quét sức khỏe hệ thống...");

    List<InstanceDto> allInstances = instanceServiceClient.getAllInstances();
    int alertCreatedCount = 0;

    for (InstanceDto inst : allInstances) {
      // Case 1: CPU_HIGH — Instance đang RUNNING và CPU vượt ngưỡng 80%
      if (InstanceStatus.RUNNING.equals(inst.getStatus()) && inst.getCpuUsage() != null
          && inst.getCpuUsage() >= 80.0f) {
        log.warn("Phát hiện Instance ID={} name={} CPU_HIGH ({}%)", inst.getId(), inst.getInstanceName(),
            inst.getCpuUsage());
        alertServiceClient.createAlert(
            inst.getId(),
            AlertType.CPU_HIGH,
            String.format("Máy chủ ảo %s (ID: %d) vượt ngưỡng CPU cho phép: %.1f%%", inst.getInstanceName(),
                inst.getId(), inst.getCpuUsage()));
        alertCreatedCount++;
      }

      // Case 2: LONG_STOPPED — Instance bị STOPPED kéo dài >= 48 giờ (Lỗi 9)
      if (InstanceStatus.STOPPED.equals(inst.getStatus())) {
        LocalDateTime lastUpdate = inst.getUpdateAt() != null ? inst.getUpdateAt() : inst.getLauncheAt();
        if (lastUpdate != null && lastUpdate.isBefore(LocalDateTime.now().minusHours(48))) {
          log.warn("Phát hiện Instance ID={} name={} bị STOPPED kéo dài hơn 48 giờ (từ {})",
              inst.getId(), inst.getInstanceName(), lastUpdate);
          alertServiceClient.createAlert(
              inst.getId(),
              AlertType.LONG_STOPPED,
              String.format("Máy chủ ảo %s (ID: %d) đã dừng hoạt động hơn 48 giờ kể từ %s",
                  inst.getInstanceName(), inst.getId(), lastUpdate.toLocalDate()));
          alertCreatedCount++;
        }
      }
    }

    log.info("Hoàn tất tiến trình quét sức khỏe hệ thống. Đã kiểm tra {} máy chủ, tạo mới {} cảnh báo.",
        allInstances.size(), alertCreatedCount);
  }
}
