package com.techvalley.monitor.alert.service.impl;

import com.techvalley.common.dto.PageResponse;
import com.techvalley.common.security.UserContext;
import com.techvalley.common.security.UserContextInfo;
import com.techvalley.monitor.alert.Alert;
import com.techvalley.monitor.alert.dto.internal.CreateAlertInternalRequest;
import com.techvalley.monitor.alert.dto.request.AlertFilterRequest;
import com.techvalley.monitor.alert.dto.response.AlertResponse;
import com.techvalley.monitor.alert.exception.AlertAlreadyResolvedException;
import com.techvalley.monitor.alert.exception.AlertNotFoundException;
import com.techvalley.monitor.alert.mapper.AlertMapper;
import com.techvalley.monitor.alert.repository.AlertRepository;
import com.techvalley.monitor.enums.AlertType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "null"})
class AlertServiceImplTest {

    @Mock
    private AlertRepository alertRepository;

    @Spy
    private AlertMapper alertMapper = new AlertMapper();

    @Mock
    private WebClient clientServiceClient;

    @Mock
    private WebClient instanceServiceClient;

    @InjectMocks
    private AlertServiceImpl alertService;

    @BeforeEach
    void setUp() {
        UserContext.set(new UserContextInfo(1L, "admin@techvalley.com", "ADMIN"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("getAlerts as ADMIN should return paged alerts")
    void getAlerts_adminSuccess() {
        Alert alert = new Alert(10L, 100L, AlertType.CPU_HIGH, "High CPU", 0, LocalDateTime.now(), null);
        Page<Alert> page = new PageImpl<>(List.of(alert));

        when(alertRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        AlertFilterRequest filter = new AlertFilterRequest();
        PageResponse<AlertResponse> result = alertService.getAlerts(filter);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(10L, result.getItems().get(0).getId());
        verify(alertRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("resolveAlert should resolve unresolved alert when permitted")
    void resolveAlert_success() {
        Alert alert = new Alert(10L, 100L, AlertType.CPU_HIGH, "High CPU", 0, LocalDateTime.now(), null);
        when(alertRepository.findById(10L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        AlertResponse response = alertService.resolveAlert(10L);

        assertNotNull(response);
        assertTrue(response.isResolved());
        assertNotNull(alert.getResolvedAt());
        assertEquals(1, alert.getIsResolved());
    }

    @Test
    @DisplayName("resolveAlert should throw AlertNotFoundException when id does not exist")
    void resolveAlert_notFound() {
        when(alertRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(AlertNotFoundException.class, () -> alertService.resolveAlert(999L));
    }

    @Test
    @DisplayName("resolveAlert should throw AlertAlreadyResolvedException when alert is already resolved")
    void resolveAlert_alreadyResolved() {
        Alert alert = new Alert(10L, 100L, AlertType.CPU_HIGH, "High CPU", 1, LocalDateTime.now(), LocalDateTime.now());
        when(alertRepository.findById(10L)).thenReturn(Optional.of(alert));

        assertThrows(AlertAlreadyResolvedException.class, () -> alertService.resolveAlert(10L));
    }

    @Test
    @DisplayName("createAlertIfAbsent should save alert if absent")
    void createAlertIfAbsent_newAlert() {
        CreateAlertInternalRequest request = new CreateAlertInternalRequest(100L, "CPU_HIGH", "High CPU usage");
        when(alertRepository.findFirstByInstanceIdAndAlertTypeAndIsResolved(100L, AlertType.CPU_HIGH, 0))
                .thenReturn(Optional.empty());

        alertService.createAlertIfAbsent(request);

        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    @DisplayName("createAlertIfAbsent should skip if unresolved alert already exists")
    void createAlertIfAbsent_alreadyExists() {
        CreateAlertInternalRequest request = new CreateAlertInternalRequest(100L, "CPU_HIGH", "High CPU usage");
        Alert existing = new Alert(1L, 100L, AlertType.CPU_HIGH, "High CPU", 0, LocalDateTime.now(), null);
        when(alertRepository.findFirstByInstanceIdAndAlertTypeAndIsResolved(100L, AlertType.CPU_HIGH, 0))
                .thenReturn(Optional.of(existing));

        alertService.createAlertIfAbsent(request);

        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    @DisplayName("countUnresolved should return total when instanceIds is null and 0 when empty")
    void countUnresolved_test() {
        when(alertRepository.countByIsResolved(0)).thenReturn(5L);

        assertEquals(5L, alertService.countUnresolved(null));
        assertEquals(0L, alertService.countUnresolved(List.of()));
    }

    @Test
    @DisplayName("getDowntimeAlerts should return empty list when instanceIds is null or empty")
    void getDowntimeAlerts_nullOrEmpty() {
        assertTrue(alertService.getDowntimeAlerts(null).isEmpty());
        assertTrue(alertService.getDowntimeAlerts(List.of()).isEmpty());
    }

    @Test
    @DisplayName("getAlertsByInstance should return empty list when instanceId is null")
    void getAlertsByInstance_null() {
        assertTrue(alertService.getAlertsByInstance(null).isEmpty());
    }
}
