package com.techvalley.monitor.client.service.impl;

import com.techvalley.monitor.client.Client;
import com.techvalley.monitor.client.dto.internal.ClientInternalDto;
import com.techvalley.monitor.client.dto.request.ClientRequest;
import com.techvalley.monitor.client.dto.response.*;
import com.techvalley.monitor.client.exception.AccessDeniedException;
import com.techvalley.monitor.client.exception.ClientNotFoundException;
import com.techvalley.monitor.client.repository.ClientRepository;
import com.techvalley.monitor.client.service.ClientService;
import com.techvalley.monitor.enums.ContractPlan;
import com.techvalley.monitor.enums.InstanceStatus;

import org.springframework.lang.NonNull;

import com.techvalley.common.security.UserContext;
import com.techvalley.common.security.UserContextInfo;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.techvalley.monitor.client.dto.external.AlertDto;
import com.techvalley.monitor.client.dto.external.InstanceDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.reactive.function.client.WebClient;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

  private final ClientRepository clientRepository;
  private final EntityManager entityManager;
  @Qualifier("instanceServiceClient")
  private final WebClient instanceServiceClient;
  @Qualifier("alertServiceClient")
  private final WebClient alertServiceClient;

  private void validateAdminRole() {
    UserContextInfo user = UserContext.get();
    if (user == null) {
      throw new AccessDeniedException("Người dùng chưa được xác thực");
    }
    if (!"ADMIN".equals(user.getRole())) {
      throw new AccessDeniedException("Chỉ ADMIN mới có quyền thực hiện hành động này");
    }
  }

  private Client getClientAndValidateAccess(@NonNull Long id) {
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

  private List<InstanceDto> findInstancesByClientId(Long clientId) {
    List<InstanceDto> result = instanceServiceClient.get()
        .uri(uriBuilder -> uriBuilder.path("/internal/instances")
            .queryParam("clientIds", clientId).build())
        .retrieve()
        .bodyToFlux(InstanceDto.class)
        .collectList()
        .block();
    return result != null ? result : Collections.emptyList();
  }

  @Override
  @Transactional
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

    String trimmedSearch = (search != null) ? search.trim() : "";
    boolean hasSearch = !trimmedSearch.isEmpty();

    if ("ADMIN".equals(user.getRole())) {
      if (hasSearch) {
        clientPage = clientRepository.findByClientNameContainingIgnoreCase(trimmedSearch, pageable);
      } else {
        clientPage = clientRepository.findAll(pageable);
      }
    } else { // CLIENT_MANAGER
      Long managerId = user.getMemberId();
      if (hasSearch) {
        clientPage = clientRepository.findByManagerIdAndClientNameContainingIgnoreCase(managerId, trimmedSearch,
            pageable);
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

    return PageResponse.<ClientResponse>builder()
        .items(items)
        .pagination(PageResponse.PaginationInfo.builder()
            .currentPage(clientPage.getNumber() + 1)
            .pageSize(clientPage.getSize())
            .totalElements(clientPage.getTotalElements())
            .totalPages(clientPage.getTotalPages())
            .build())
        .build();
  }

  @Override
  public List<InstanceDto> getClientInstances(@NonNull Long id) {
    getClientAndValidateAccess(id);
    return findInstancesByClientId(id);
  }

  @Override
  public ClientCostResponse getClientCost(@NonNull Long id) {
    Client client = getClientAndValidateAccess(id);
    List<InstanceDto> instances = findInstancesByClientId(id);

    double runningCost = 0.0;
    double stoppedCost = 0.0;

    for (InstanceDto inst : instances) {
      double cost = inst.getMonthlyCost() != null ? inst.getMonthlyCost() : 0.0;
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
  public ClientCostForecastResponse getClientCostForecast(@NonNull Long id) {
    Client client = getClientAndValidateAccess(id);
    List<InstanceDto> instances = findInstancesByClientId(id);

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
        unitPrice = inst.getMonthlyCost() != null ? inst.getMonthlyCost() : 50.0;
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

    // Query last snapshot from cost_snapshots using EntityManager to avoid creating
    // a new repository
    List<com.techvalley.monitor.cost.CostSnapshot> snapshots = entityManager.createQuery(
        "SELECT s FROM CostSnapshot s WHERE s.clientId = :clientId ORDER BY s.createdAt DESC",
        com.techvalley.monitor.cost.CostSnapshot.class)
        .setParameter("clientId", client.getId())
        .setMaxResults(1)
        .getResultList();

    if (!snapshots.isEmpty()) {
      double lastMonthCost = snapshots.get(0).getTotalCost() != null ? snapshots.get(0).getTotalCost() : 0.0;
      if (lastMonthCost > 0 && projectedSpentEndOfMonth > lastMonthCost * 1.2) {
        recommendation = String.format(
            Locale.US,
            "Cảnh báo: Chi phí dự báo cuối tháng ($%.2f) tăng hơn 20%% so với tháng trước ($%.2f). Vui lòng kiểm tra và tối ưu hóa tài nguyên.",
            projectedSpentEndOfMonth, lastMonthCost);
      }
    }

    return ClientCostForecastResponse.builder()
        .clientId(client.getId())
        .currentSpent(Math.round(currentSpent * 100.0) / 100.0)
        .projectedSpentEndOfMonth(Math.round(projectedSpentEndOfMonth * 100.0) / 100.0)
        .activeRunningInstances(activeRunningInstances)
        .recommendation(recommendation)
        .build();
  }

  @Override
  public ClientSlaResponse getClientSla(@NonNull Long id) {
    Client client = getClientAndValidateAccess(id);
    List<InstanceDto> instances = findInstancesByClientId(id);

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

    List<Long> instanceIds = instances.stream().map(i -> i.getId()).collect(Collectors.toList());

    // Get alerts that represent downtime (exclude CPU_HIGH) — gọi qua alert-service
    List<AlertDto> alerts;
    try {
      alerts = alertServiceClient.get()
          .uri(uriBuilder -> uriBuilder.path("/internal/alerts/downtime")
              .queryParam("instanceIds", instanceIds).build())
          .retrieve()
          .bodyToFlux(AlertDto.class)
          .collectList()
          .onErrorResume(e -> Mono.just(Collections.emptyList()))
          .block();
    } catch (Exception e) {
      alerts = Collections.emptyList();
    }
    if (alerts == null) {
      alerts = Collections.emptyList();
    }

    double totalDowntimeMinutes = 0.0;
    for (AlertDto alert : alerts) {
      LocalDateTime alertStart = alert.getDetectedAt() != null ? alert.getDetectedAt() : startOfMonth;
      boolean isResolved = Integer.valueOf(1).equals(alert.getIsResolved());
      LocalDateTime alertEnd;
      if (alert.getResolvedAt() != null) {
        alertEnd = alert.getResolvedAt();
      } else if (isResolved) {
        alertEnd = alertStart;
      } else {
        alertEnd = now;
      }

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

  // ── Internal methods ──────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public List<ClientInternalDto> getClientsByManager(Long managerId) {
    return clientRepository.findByManagerId(managerId).stream()
        .map(ClientInternalDto::from)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public long countClients() {
    return clientRepository.count();
  }
}
