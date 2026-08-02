package com.techvalley.monitor.monitoring.service;

import com.techvalley.monitor.monitoring.dto.response.MonitoringInstanceResponse;
import com.techvalley.monitor.monitoring.dto.response.MonitoringReportResponse;

import java.util.List;

public interface MonitoringService {

    List<MonitoringInstanceResponse> getWarnings();

    List<MonitoringInstanceResponse> getErrors();

    List<MonitoringInstanceResponse> getLongStopped();

    MonitoringReportResponse getOverviewReport();
}
