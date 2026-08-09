package com.techvalley.client.service.impl;

import com.techvalley.client.client.AlertServiceClient;
import com.techvalley.client.client.InstanceServiceClient;
import com.techvalley.client.dto.client.AlertDto;
import com.techvalley.client.dto.client.InstanceDto;
import com.techvalley.client.dto.request.ClientRequest;
import com.techvalley.client.dto.response.*;
import com.techvalley.client.entity.Client;
import com.techvalley.client.exception.AccessDeniedException;
import com.techvalley.client.exception.ClientNotFoundException;
import com.techvalley.client.repository.ClientRepository;
import com.techvalley.client.security.UserContext;
import com.techvalley.client.security.UserContextInfo;
import com.techvalley.client.service.ClientService;
import com.techvalley.client.enums.AlertType;
import com.techvalley.client.enums.ContractPlan;
import com.techvalley.client.enums.InstanceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ClientServiceImpl implements ClientService {

  private final ClientRepository clientRepository;
  private final InstanceServiceClient instanceServiceClient;
  private final AlertServiceClient alertServiceClient;

  private void validateAdminRole() {
    UserContextInfo user = UserContext.get();
    if (user == null) {
      throw new AccessDeniedException("Người dùng chưa được xác thực");
    }
    if (!"ADMIN".equals(user.getRole())) {
      throw new AccessDeniedException("Chỉ ADMIN mới có quyền thực hiện hành động này");
    }
  }

  private Client getClientAndValidateAccess(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("ID khách hàng không được để trống");
    }
    Client client = clientRepository.findById(id)
        .orElseThrow(() -> new ClientNotFoundException("Không tìm thấy khách hàng với ID: " + id));
    UserContextInfo user = UserContext.get();
    if (user == null) {
      throw new AccessDeniedException("Người dùng chưa được xác thực");
    }
    if ("CLIENT_MANAGER".equals(user.getRole())) {
      if (!user.getMemberId().equals(client.getManagerId())) {
        throw new AccessDeniedException("Bạn không có quyền truy cập thông tin của khách hàng này");
      }
    }
    return client;
  }

  @Override
  @Transactional
  @CacheEvict(value = {"clientCost", "clientSla", "clientInstances"}, allEntries = true)
  public ClientResponse createClient(ClientRequest request) {
    validateAdminRole();

    ContractPlan plan;
    try {
      plan = ContractPlan.valueOf(request.getContractPlan().toUpperCase());
    } catch (Exception e) {
      throw new IllegalArgumentException("Gói hợp đồng không hợp lệ. Chỉ chấp nhận: BASIC, STANDARD, PREMIUM");
    }

    Client client = new Client();
    client.setClientName(request.getName());
    client.setContractPlan(plan);
    client.setManagerId(request.getManagerId());
    client.setCreateAt(LocalDateTime.now());

    Client saved = clientRepository.save(client);

    return ClientResponse.builder()
        .id(saved.getId())
        .name(saved.getClientName())
        .contractPlan(saved.getContractPlan())
        .managerId(saved.getManagerId())
        .createdAt(saved.getCreateAt())
        .email(request.getEmail())
        .company(request.getCompany())
        .build();
  }

  @Override
  public PageResponse<ClientResponse> getClients(int page, int size, String search) {
    UserContextInfo user = UserContext.get();
    if (user == null) {
      throw new AccessDeniedException("Người dùng chưa được xác thực");
    }

    Pageable pageable = PageRequest.of(page - 1, size);
    Page<Client> clientPage;

    String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

    if ("ADMIN".equals(user.getRole())) {
      if (searchTerm != null) {
        clientPage = clientRepository.findByClientNameContainingIgnoreCase(searchTerm, pageable);
      } else {
        clientPage = clientRepository.findAll(pageable);
      }
    } else { // CLIENT_MANAGER
      Long managerId = user.getMemberId();
      if (searchTerm != null) {
        clientPage = clientRepository.findByManagerIdAndClientNameContainingIgnoreCase(managerId, searchTerm, pageable);
      } else {
        clientPage = clientRepository.findByManagerId(managerId, pageable);
      }
    }

    List<ClientResponse> items = clientPage.getContent().stream()
        .map(c -> ClientResponse.builder()
            .id(c.getId())
            .name(c.getClientName())
            .contractPlan(c.getContractPlan())
            .managerId(c.getManagerId())
            .createdAt(c.getCreateAt())
            .build())
        .collect(Collectors.toList());

    return PageResponse.of(items, clientPage);
  }

  @Override
  @Cacheable(value = "clientInstances", key = "#id")
  public List<InstanceDto> getClientInstances(Long id) {
    getClientAndValidateAccess(id);
    return instanceServiceClient.getInstancesByClientId(id);
  }

  @Override
  @Cacheable(value = "clientCost", key = "#id")
  public ClientCostResponse getClientCost(Long id) {
    Client client = getClientAndValidateAccess(id);
    List<InstanceDto> instances = instanceServiceClient.getInstancesByClientId(id);

    double runningCost = 0.0;
    double stoppedCost = 0.0;

    for (InstanceDto inst : instances) {
      double cost = inst.getMonthlyCost() != null ? inst.getMonthlyCost().doubleValue() : 0.0;
      if (InstanceStatus.RUNNING.equals(inst.getStatus())) {
        runningCost += cost;
      } else {
        stoppedCost += cost;
      }
    }

    LocalDateTime now = LocalDateTime.now();
    String currentMonthStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM"));

    return ClientCostResponse.builder()
        .clientId(client.getId())
        .currentMonth(currentMonthStr)
        .totalInstances(instances.size())
        .runningCost(runningCost)
        .stoppedCost(stoppedCost)
        .totalMonthlyCost(runningCost + stoppedCost)
        .build();
  }

  @Override
  public ClientCostForecastResponse getClientCostForecast(Long id) {
    Client client = getClientAndValidateAccess(id);
    List<InstanceDto> instances = instanceServiceClient.getInstancesByClientId(id);

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
    LocalDateTime endOfMonth = startOfMonth.plusMonths(1);
    long totalDaysInMonth = ChronoUnit.DAYS.between(startOfMonth, endOfMonth);

    double elapsedMinutes = Duration.between(startOfMonth, now).toMinutes();
    double elapsedDays = elapsedMinutes / (24.0 * 60.0);
    double remainingDays = totalDaysInMonth - elapsedDays;

    double currentSpent = 0.0;
    double runningForecast = 0.0;
    int activeRunningInstances = 0;

    for (InstanceDto inst : instances) {
      double unitPrice = 50.0;
      if (inst.getInstanceType() != null) {
        switch (inst.getInstanceType()) {
          case SMALL:
            unitPrice = 50.0;
            break;
          case MEDIUM:
            unitPrice = 120.0;
            break;
          case LARGE:
            unitPrice = 250.0;
            break;
        }
      } else {
        unitPrice = inst.getMonthlyCost() != null ? inst.getMonthlyCost().doubleValue() : 50.0;
      }

      LocalDateTime launch = inst.getLauncheAt() != null ? inst.getLauncheAt() : startOfMonth;
      LocalDateTime activeStart = launch.isBefore(startOfMonth) ? startOfMonth : launch;

      if (activeStart.isBefore(now)) {
        if (InstanceStatus.RUNNING.equals(inst.getStatus())) {
          activeRunningInstances++;
          double activeDays = (double) Duration.between(activeStart, now).toMinutes() / (24.0 * 60.0);
          currentSpent += unitPrice * (activeDays / totalDaysInMonth);
          runningForecast += unitPrice * (remainingDays / totalDaysInMonth);
        } else {
          LocalDateTime activeEnd = inst.getUpdateAt() != null ? inst.getUpdateAt() : now;
          if (activeEnd.isBefore(activeStart))
            activeEnd = activeStart;
          if (activeEnd.isAfter(now))
            activeEnd = now;

          double activeDays = (double) Duration.between(activeStart, activeEnd).toMinutes() / (24.0 * 60.0);
          currentSpent += unitPrice * (activeDays / totalDaysInMonth);
        }
      }
    }

    double projectedSpentEndOfMonth = currentSpent + runningForecast;
    String recommendation = "Dự báo chi phí trong tầm kiểm soát.";

    return ClientCostForecastResponse.builder()
        .clientId(client.getId())
        .currentSpent(Math.round(currentSpent * 100.0) / 100.0)
        .projectedSpentEndOfMonth(Math.round(projectedSpentEndOfMonth * 100.0) / 100.0)
        .activeRunningInstances(activeRunningInstances)
        .recommendation(recommendation)
        .build();
  }

  @Override
  @Cacheable(value = "clientSla", key = "#id")
  public ClientSlaResponse getClientSla(Long id) {
    Client client = getClientAndValidateAccess(id);
    List<InstanceDto> instances = instanceServiceClient.getInstancesByClientId(id);

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
    String currentMonthStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM"));

    if (instances.isEmpty()) {
      double targetSla = 95.0;
      if (client.getContractPlan() != null) {
        switch (client.getContractPlan()) {
          case PREMIUM:
            targetSla = 99.9;
            break;
          case STANDARD:
            targetSla = 99.0;
            break;
          case BASIC:
            targetSla = 95.0;
            break;
        }
      }
      return ClientSlaResponse.builder()
          .clientId(client.getId())
          .month(currentMonthStr)
          .slaPercentage(100.0)
          .targetSla(targetSla)
          .status("NORMAL")
          .totalDowntimeHours(0.0)
          .build();
    }

    List<AlertDto> alerts = new ArrayList<>();
    for (InstanceDto inst : instances) {
      List<AlertDto> instAlerts = alertServiceClient.getAlertsByInstanceId(inst.getId());
      if (instAlerts != null) {
        for (AlertDto a : instAlerts) {
          if (a.getAlertType() != AlertType.CPU_HIGH) {
            alerts.add(a);
          }
        }
      }
    }

    double totalDowntimeMinutes = 0.0;
    for (AlertDto alert : alerts) {
      LocalDateTime alertStart = alert.getDetectedAt() != null ? alert.getDetectedAt() : startOfMonth;
      LocalDateTime alertEnd = alert.getResolvedAt() != null ? alert.getResolvedAt() : now;

      LocalDateTime overlapStart = alertStart.isBefore(startOfMonth) ? startOfMonth : alertStart;
      LocalDateTime overlapEnd = alertEnd.isAfter(now) ? now : alertEnd;

      if (overlapStart.isBefore(overlapEnd)) {
        totalDowntimeMinutes += Duration.between(overlapStart, overlapEnd).toMinutes();
      }
    }

    double totalExpectedMinutes = 0.0;
    for (InstanceDto inst : instances) {
      LocalDateTime launch = inst.getLauncheAt() != null ? inst.getLauncheAt() : startOfMonth;
      LocalDateTime activeStart = launch.isBefore(startOfMonth) ? startOfMonth : launch;
      if (activeStart.isBefore(now)) {
        totalExpectedMinutes += Duration.between(activeStart, now).toMinutes();
      }
    }

    double slaPercentage = 100.0;
    if (totalExpectedMinutes > 0) {
      slaPercentage = ((totalExpectedMinutes - totalDowntimeMinutes) / totalExpectedMinutes) * 100.0;
    }

    if (slaPercentage < 0.0)
      slaPercentage = 0.0;
    if (slaPercentage > 100.0)
      slaPercentage = 100.0;

    double targetSla = 95.0;
    if (client.getContractPlan() != null) {
      switch (client.getContractPlan()) {
        case PREMIUM:
          targetSla = 99.9;
          break;
        case STANDARD:
          targetSla = 99.0;
          break;
        case BASIC:
          targetSla = 95.0;
          break;
      }
    }

    String status = slaPercentage < targetSla ? "VIOLATION" : "NORMAL";

    return ClientSlaResponse.builder()
        .clientId(client.getId())
        .month(currentMonthStr)
        .slaPercentage(Math.round(slaPercentage * 100.0) / 100.0)
        .targetSla(targetSla)
        .status(status)
        .totalDowntimeHours(Math.round((totalDowntimeMinutes / 60.0) * 100.0) / 100.0)
        .build();
  }
}
