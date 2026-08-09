package com.techvalley.monitoring.service;

import com.techvalley.monitoring.enums.InstanceStatus;
import com.techvalley.monitoring.dto.response.MonitoringInstanceResponse;
import com.techvalley.monitoring.dto.response.MonitoringReportResponse;

import java.util.List;

public interface MonitoringService {

    List<MonitoringInstanceResponse> getMonitoredInstances(Long clientId, InstanceStatus status, Float cpuThreshold);

    MonitoringReportResponse getMonitoringReport(Long clientId);

    void scanAndCheckSystemHealth();
}
