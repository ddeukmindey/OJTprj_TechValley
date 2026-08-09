package com.techvalley.alert.service;

import com.techvalley.alert.dto.request.AlertCreateRequest;
import com.techvalley.alert.dto.request.AlertFilterRequest;
import com.techvalley.alert.dto.response.AlertResponse;
import com.techvalley.alert.dto.response.PageResponse;

import java.util.List;

public interface AlertService {

    PageResponse<AlertResponse> getAlerts(AlertFilterRequest filterRequest);

    AlertResponse resolveAlert(Long id);

    AlertResponse createAlert(AlertCreateRequest request);

    List<AlertResponse> createAlertsBatch(List<AlertCreateRequest> requests);

    void deleteAlertsByInstanceId(Long instanceId);
}
