package com.techvalley.monitor.client;

import com.techvalley.monitor.client.Client;
import com.techvalley.monitor.client.dto.request.ClientRequest;
import com.techvalley.monitor.client.dto.response.*;
import com.techvalley.monitor.client.repository.ClientRepository;
import com.techvalley.monitor.client.service.impl.ClientServiceImpl;
import com.techvalley.monitor.client.exception.*;
import com.techvalley.monitor.enums.*;
import com.techvalley.monitor.instance.Instance;
import com.techvalley.monitor.cost.CostSnapshot;
import com.techvalley.monitor.alert.Alert;
import com.techvalley.monitor.common.security.UserContext;
import com.techvalley.monitor.common.security.UserContextInfo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClientServiceTest {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ClientServiceImpl clientService;

    @BeforeEach
    public void setUp() {
        // Default role setup, can be overridden inside specific tests
        UserContext.set(new UserContextInfo(1L, "admin@techvalley.com", "ADMIN"));
    }

    @AfterEach
    public void tearDown() {
        UserContext.clear();
    }

    // ==========================================
    // CREATE CLIENT TESTS
    // ==========================================

    @Test
    public void testCreateClient_Success() {
        // Arrange
        UserContext.set(new UserContextInfo(1L, "admin@techvalley.com", "ADMIN"));
        ClientRequest request = ClientRequest.builder()
                .name("New Client")
                .contractPlan("PREMIUM")
                .managerId(2L)
                .email("new@client.com")
                .company("Client Inc")
                .build();
        
        Client client = new Client(1L, "New Client", ContractPlan.PREMIUM, 2L, LocalDateTime.now());
        when(clientRepository.save(any(Client.class))).thenReturn(client);

        // Act
        ClientResponse response = clientService.createClient(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("New Client", response.getName());
        assertEquals(ContractPlan.PREMIUM, response.getContractPlan());
        assertEquals(2L, response.getManagerId());
        assertEquals("new@client.com", response.getEmail());
        assertEquals("Client Inc", response.getCompany());
        verify(clientRepository, times(1)).save(any(Client.class));
    }

    @Test
    public void testCreateClient_Failure_NotAdmin() {
        // Arrange
        UserContext.set(new UserContextInfo(2L, "manager@techvalley.com", "CLIENT_MANAGER"));
        ClientRequest request = ClientRequest.builder()
                .name("New Client")
                .contractPlan("PREMIUM")
                .managerId(2L)
                .build();

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> clientService.createClient(request));
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    public void testCreateClient_Failure_Unauthenticated() {
        // Arrange
        UserContext.clear();
        ClientRequest request = ClientRequest.builder()
                .name("New Client")
                .contractPlan("PREMIUM")
                .managerId(2L)
                .build();

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> clientService.createClient(request));
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    public void testCreateClient_Failure_InvalidPlan() {
        // Arrange
        UserContext.set(new UserContextInfo(1L, "admin@techvalley.com", "ADMIN"));
        ClientRequest request = ClientRequest.builder()
                .name("New Client")
                .contractPlan("INVALID_PLAN_NAME")
                .managerId(2L)
                .build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> clientService.createClient(request));
        verify(clientRepository, never()).save(any(Client.class));
    }

    // ==========================================
    // GET CLIENTS TESTS
    // ==========================================

    @Test
    public void testGetClients_Admin_NoSearch() {
        // Arrange
        UserContext.set(new UserContextInfo(1L, "admin@techvalley.com", "ADMIN"));
        Client client1 = new Client(1L, "Client A", ContractPlan.BASIC, 2L, LocalDateTime.now());
        Client client2 = new Client(2L, "Client B", ContractPlan.STANDARD, 2L, LocalDateTime.now());
        List<Client> list = List.of(client1, client2);
        Page<Client> pageResult = new PageImpl<>(list, PageRequest.of(0, 10), 2);
        
        when(clientRepository.findAll(any(Pageable.class))).thenReturn(pageResult);

        // Act
        PageResponse<ClientResponse> response = clientService.getClients(1, 10, null);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getItems().size());
        assertEquals("Client A", response.getItems().get(0).getName());
        assertEquals("Client B", response.getItems().get(1).getName());
        assertEquals(1, response.getPagination().getCurrentPage());
        assertEquals(2, response.getPagination().getTotalElements());
    }

    @Test
    public void testGetClients_Admin_WithSearch() {
        // Arrange
        UserContext.set(new UserContextInfo(1L, "admin@techvalley.com", "ADMIN"));
        Client client1 = new Client(1L, "Client A", ContractPlan.BASIC, 2L, LocalDateTime.now());
        Page<Client> pageResult = new PageImpl<>(List.of(client1), PageRequest.of(0, 10), 1);
        
        when(clientRepository.findByClientNameContainingIgnoreCase(eq("Client A"), any(Pageable.class))).thenReturn(pageResult);

        // Act
        PageResponse<ClientResponse> response = clientService.getClients(1, 10, "Client A");

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals("Client A", response.getItems().get(0).getName());
    }

    @Test
    public void testGetClients_Manager_NoSearch() {
        // Arrange
        UserContext.set(new UserContextInfo(2L, "manager@techvalley.com", "CLIENT_MANAGER"));
        Client client1 = new Client(1L, "Client A", ContractPlan.BASIC, 2L, LocalDateTime.now());
        Page<Client> pageResult = new PageImpl<>(List.of(client1), PageRequest.of(0, 10), 1);
        
        when(clientRepository.findByManagerId(eq(2L), any(Pageable.class))).thenReturn(pageResult);

        // Act
        PageResponse<ClientResponse> response = clientService.getClients(1, 10, "");

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals("Client A", response.getItems().get(0).getName());
    }

    @Test
    public void testGetClients_Manager_WithSearch() {
        // Arrange
        UserContext.set(new UserContextInfo(2L, "manager@techvalley.com", "CLIENT_MANAGER"));
        Client client1 = new Client(1L, "Client A", ContractPlan.BASIC, 2L, LocalDateTime.now());
        Page<Client> pageResult = new PageImpl<>(List.of(client1), PageRequest.of(0, 10), 1);
        
        when(clientRepository.findByManagerIdAndClientNameContainingIgnoreCase(eq(2L), eq("Client A"), any(Pageable.class))).thenReturn(pageResult);

        // Act
        PageResponse<ClientResponse> response = clientService.getClients(1, 10, "Client A");

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals("Client A", response.getItems().get(0).getName());
    }

    @Test
    public void testGetClients_Failure_Unauthenticated() {
        // Arrange
        UserContext.clear();

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> clientService.getClients(1, 10, null));
    }

    // ==========================================
    // GET CLIENT INSTANCES TESTS
    // ==========================================

    @Test
    public void testGetClientInstances_Success_Admin() {
        // Arrange
        UserContext.set(new UserContextInfo(1L, "admin@techvalley.com", "ADMIN"));
        Long clientId = 10L;
        Client client = new Client(clientId, "Test Client", ContractPlan.PREMIUM, 2L, LocalDateTime.now());
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        Instance inst1 = new Instance(1L, "web-server-1", "ap-southeast-1", InstanceType.MEDIUM, InstanceStatus.RUNNING, 30f, 120.0f, clientId, LocalDateTime.now(), LocalDateTime.now());
        TypedQuery<Instance> mockQuery = Mockito.mock(TypedQuery.class);
        when(entityManager.createQuery("SELECT i FROM Instance i WHERE i.clientId = :clientId", Instance.class)).thenReturn(mockQuery);
        when(mockQuery.setParameter("clientId", clientId)).thenReturn(mockQuery);
        when(mockQuery.getResultList()).thenReturn(List.of(inst1));

        // Act
        List<Instance> instances = clientService.getClientInstances(clientId);

        // Assert
        assertNotNull(instances);
        assertEquals(1, instances.size());
        assertEquals(1L, instances.get(0).getId());
    }

    @Test
    public void testGetClientInstances_Success_Manager() {
        // Arrange
        UserContext.set(new UserContextInfo(2L, "manager@techvalley.com", "CLIENT_MANAGER"));
        Long clientId = 10L;
        Client client = new Client(clientId, "Test Client", ContractPlan.PREMIUM, 2L, LocalDateTime.now());
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        Instance inst1 = new Instance(1L, "web-server-1", "ap-southeast-1", InstanceType.MEDIUM, InstanceStatus.RUNNING, 30f, 120.0f, clientId, LocalDateTime.now(), LocalDateTime.now());
        TypedQuery<Instance> mockQuery = Mockito.mock(TypedQuery.class);
        when(entityManager.createQuery("SELECT i FROM Instance i WHERE i.clientId = :clientId", Instance.class)).thenReturn(mockQuery);
        when(mockQuery.setParameter("clientId", clientId)).thenReturn(mockQuery);
        when(mockQuery.getResultList()).thenReturn(List.of(inst1));

        // Act
        List<Instance> instances = clientService.getClientInstances(clientId);

        // Assert
        assertNotNull(instances);
        assertEquals(1, instances.size());
    }

    @Test
    public void testGetClientInstances_Failure_Forbidden() {
        // Arrange
        UserContext.set(new UserContextInfo(3L, "other-manager@techvalley.com", "CLIENT_MANAGER"));
        Long clientId = 10L;
        Client client = new Client(clientId, "Test Client", ContractPlan.PREMIUM, 2L, LocalDateTime.now());
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> clientService.getClientInstances(clientId));
    }

    @Test
    public void testGetClientInstances_Failure_NotFound() {
        // Arrange
        UserContext.set(new UserContextInfo(1L, "admin@techvalley.com", "ADMIN"));
        Long clientId = 99L;
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ClientNotFoundException.class, () -> clientService.getClientInstances(clientId));
    }

    // ==========================================
    // GET CLIENT COST TESTS
    // ==========================================

    @Test
    public void testGetClientCost() {
        // Arrange
        Long clientId = 10L;
        Client client = new Client(clientId, "Test Client", ContractPlan.PREMIUM, 1L, LocalDateTime.now());
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        Instance inst1 = new Instance(1L, "web-server-1", "ap-southeast-1", InstanceType.MEDIUM, InstanceStatus.RUNNING, 30f, 120.0f, clientId, LocalDateTime.now(), LocalDateTime.now());
        Instance inst2 = new Instance(2L, "db-server-1", "ap-southeast-1", InstanceType.LARGE, InstanceStatus.STOPPED, 0f, 250.0f, clientId, LocalDateTime.now(), LocalDateTime.now());
        List<Instance> instances = List.of(inst1, inst2);

        TypedQuery<Instance> mockQuery = Mockito.mock(TypedQuery.class);
        when(entityManager.createQuery("SELECT i FROM Instance i WHERE i.clientId = :clientId", Instance.class)).thenReturn(mockQuery);
        when(mockQuery.setParameter("clientId", clientId)).thenReturn(mockQuery);
        when(mockQuery.getResultList()).thenReturn(instances);

        // Act
        ClientCostResponse costResponse = clientService.getClientCost(clientId);

        // Assert
        assertNotNull(costResponse);
        assertEquals(clientId, costResponse.getClientId());
        assertEquals(2, costResponse.getTotalInstances());
        assertEquals(120.0, costResponse.getRunningCost());
        assertEquals(250.0, costResponse.getStoppedCost());
        assertEquals(370.0, costResponse.getTotalMonthlyCost());
    }

    // ==========================================
    // COST FORECAST TESTS
    // ==========================================

    @Test
    public void testGetClientCostForecast_WithSnapshot_Warning() {
        // Arrange
        UserContext.set(new UserContextInfo(1L, "admin@techvalley.com", "ADMIN"));
        Long clientId = 10L;
        Client client = new Client(clientId, "Test Client", ContractPlan.PREMIUM, 1L, LocalDateTime.now());
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        Instance inst1 = new Instance(1L, "web-server-1", "ap-southeast-1", InstanceType.MEDIUM, InstanceStatus.RUNNING, 30f, 120.0f, clientId, startOfMonth, now);
        Instance inst2 = new Instance(2L, "db-server-1", "ap-southeast-1", InstanceType.LARGE, InstanceStatus.STOPPED, 0f, 250.0f, clientId, startOfMonth, startOfMonth);
        List<Instance> instances = List.of(inst1, inst2);

        TypedQuery<Instance> mockInstanceQuery = Mockito.mock(TypedQuery.class);
        when(entityManager.createQuery("SELECT i FROM Instance i WHERE i.clientId = :clientId", Instance.class)).thenReturn(mockInstanceQuery);
        when(mockInstanceQuery.setParameter("clientId", clientId)).thenReturn(mockInstanceQuery);
        when(mockInstanceQuery.getResultList()).thenReturn(instances);

        // Mock CostSnapshot
        CostSnapshot snapshot = new CostSnapshot(1L, clientId, 202607, 90.0f, 2, LocalDateTime.now());
        TypedQuery<CostSnapshot> mockSnapshotQuery = Mockito.mock(TypedQuery.class);
        when(entityManager.createQuery(contains("CostSnapshot"), eq(CostSnapshot.class))).thenReturn(mockSnapshotQuery);
        when(mockSnapshotQuery.setParameter("clientId", clientId)).thenReturn(mockSnapshotQuery);
        when(mockSnapshotQuery.setMaxResults(1)).thenReturn(mockSnapshotQuery);
        when(mockSnapshotQuery.getResultList()).thenReturn(List.of(snapshot));

        // Act
        ClientCostForecastResponse forecastResponse = clientService.getClientCostForecast(clientId);

        // Assert
        assertNotNull(forecastResponse);
        assertEquals(clientId, forecastResponse.getClientId());
        assertEquals(1, forecastResponse.getActiveRunningInstances());
        assertEquals(120.0, forecastResponse.getProjectedSpentEndOfMonth());
        assertTrue(forecastResponse.getRecommendation().contains("Cảnh báo"));
    }

    @Test
    public void testGetClientCostForecast_NoSnapshot() {
        // Arrange
        UserContext.set(new UserContextInfo(1L, "admin@techvalley.com", "ADMIN"));
        Long clientId = 10L;
        Client client = new Client(clientId, "Test Client", ContractPlan.PREMIUM, 1L, LocalDateTime.now());
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        Instance inst1 = new Instance(1L, "web-server-1", "ap-southeast-1", InstanceType.MEDIUM, InstanceStatus.RUNNING, 30f, 120.0f, clientId, startOfMonth, now);
        List<Instance> instances = List.of(inst1);

        TypedQuery<Instance> mockInstanceQuery = Mockito.mock(TypedQuery.class);
        when(entityManager.createQuery("SELECT i FROM Instance i WHERE i.clientId = :clientId", Instance.class)).thenReturn(mockInstanceQuery);
        when(mockInstanceQuery.setParameter("clientId", clientId)).thenReturn(mockInstanceQuery);
        when(mockInstanceQuery.getResultList()).thenReturn(instances);

        TypedQuery<CostSnapshot> mockSnapshotQuery = Mockito.mock(TypedQuery.class);
        when(entityManager.createQuery(contains("CostSnapshot"), eq(CostSnapshot.class))).thenReturn(mockSnapshotQuery);
        when(mockSnapshotQuery.setParameter("clientId", clientId)).thenReturn(mockSnapshotQuery);
        when(mockSnapshotQuery.setMaxResults(1)).thenReturn(mockSnapshotQuery);
        when(mockSnapshotQuery.getResultList()).thenReturn(Collections.emptyList());

        // Act
        ClientCostForecastResponse forecastResponse = clientService.getClientCostForecast(clientId);

        // Assert
        assertNotNull(forecastResponse);
        assertEquals("Dự báo chi phí trong tầm kiểm soát.", forecastResponse.getRecommendation());
    }

    // ==========================================
    // SLA CALCULATION TESTS
    // ==========================================

    @Test
    public void testGetClientSla_NoInstances() {
        // Arrange
        Long clientId = 10L;
        Client client = new Client(clientId, "Test Client", ContractPlan.PREMIUM, 1L, LocalDateTime.now());
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        TypedQuery<Instance> mockQuery = Mockito.mock(TypedQuery.class);
        when(entityManager.createQuery("SELECT i FROM Instance i WHERE i.clientId = :clientId", Instance.class)).thenReturn(mockQuery);
        when(mockQuery.setParameter("clientId", clientId)).thenReturn(mockQuery);
        when(mockQuery.getResultList()).thenReturn(Collections.emptyList());

        // Act
        ClientSlaResponse slaResponse = clientService.getClientSla(clientId);

        // Assert
        assertNotNull(slaResponse);
        assertEquals(100.0, slaResponse.getSlaPercentage());
        assertEquals(99.9, slaResponse.getTargetSla());
        assertEquals("NORMAL", slaResponse.getStatus());
    }

    @Test
    public void testGetClientSla_WithDowntime_Violation() {
        // Arrange
        UserContext.set(new UserContextInfo(1L, "admin@techvalley.com", "ADMIN"));
        Long clientId = 10L;
        Client client = new Client(clientId, "Test Client", ContractPlan.PREMIUM, 1L, LocalDateTime.now());
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        long diffMinutes = java.time.Duration.between(startOfMonth, now).toMinutes();

        LocalDateTime launchTime = diffMinutes > 100 ? now.minusMinutes(100) : startOfMonth;
        long expectedMinutes = java.time.Duration.between(launchTime, now).toMinutes();
        long downtimeMinutes = expectedMinutes / 10;
        LocalDateTime alertStart = launchTime;
        LocalDateTime alertEnd = launchTime.plusMinutes(downtimeMinutes);

        Instance inst1 = new Instance(1L, "web-server-1", "ap-southeast-1", InstanceType.MEDIUM, InstanceStatus.RUNNING, 30f, 120.0f, clientId, launchTime, now);
        List<Instance> instances = List.of(inst1);

        TypedQuery<Instance> mockInstanceQuery = Mockito.mock(TypedQuery.class);
        when(entityManager.createQuery("SELECT i FROM Instance i WHERE i.clientId = :clientId", Instance.class)).thenReturn(mockInstanceQuery);
        when(mockInstanceQuery.setParameter("clientId", clientId)).thenReturn(mockInstanceQuery);
        when(mockInstanceQuery.getResultList()).thenReturn(instances);

        Alert alert = new Alert(1L, 1L, AlertType.ERROR_DETECTED, "System error", 0, alertStart, alertEnd);
        TypedQuery<Alert> mockAlertQuery = Mockito.mock(TypedQuery.class);
        when(entityManager.createQuery(contains("Alert"), eq(Alert.class))).thenReturn(mockAlertQuery);
        when(mockAlertQuery.setParameter("instanceIds", List.of(1L))).thenReturn(mockAlertQuery);
        when(mockAlertQuery.setParameter("cpuHigh", AlertType.CPU_HIGH)).thenReturn(mockAlertQuery);
        when(mockAlertQuery.getResultList()).thenReturn(List.of(alert));

        // Act
        ClientSlaResponse slaResponse = clientService.getClientSla(clientId);

        // Assert
        assertNotNull(slaResponse);
        assertEquals(clientId, slaResponse.getClientId());
        assertEquals(90.0, slaResponse.getSlaPercentage());
        assertEquals(99.9, slaResponse.getTargetSla());
        assertEquals("VIOLATION", slaResponse.getStatus());
        assertEquals(Math.round((downtimeMinutes / 60.0) * 100.0) / 100.0, slaResponse.getTotalDowntimeHours());
    }

    @Test
    public void testGetClientSla_WithDowntime_Normal() {
        // Arrange
        UserContext.set(new UserContextInfo(1L, "admin@techvalley.com", "ADMIN"));
        Long clientId = 10L;
        Client client = new Client(clientId, "Test Client", ContractPlan.BASIC, 1L, LocalDateTime.now());
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        long diffMinutes = java.time.Duration.between(startOfMonth, now).toMinutes();

        LocalDateTime launchTime = diffMinutes > 100 ? now.minusMinutes(100) : startOfMonth;
        long expectedMinutes = java.time.Duration.between(launchTime, now).toMinutes();
        long downtimeMinutes = expectedMinutes / 100;
        if (downtimeMinutes == 0) downtimeMinutes = 1;
        if (downtimeMinutes >= expectedMinutes) downtimeMinutes = expectedMinutes - 1;

        LocalDateTime alertStart = launchTime;
        LocalDateTime alertEnd = launchTime.plusMinutes(downtimeMinutes);

        Instance inst1 = new Instance(1L, "web-server-1", "ap-southeast-1", InstanceType.MEDIUM, InstanceStatus.RUNNING, 30f, 120.0f, clientId, launchTime, now);
        List<Instance> instances = List.of(inst1);

        TypedQuery<Instance> mockInstanceQuery = Mockito.mock(TypedQuery.class);
        when(entityManager.createQuery("SELECT i FROM Instance i WHERE i.clientId = :clientId", Instance.class)).thenReturn(mockInstanceQuery);
        when(mockInstanceQuery.setParameter("clientId", clientId)).thenReturn(mockInstanceQuery);
        when(mockInstanceQuery.getResultList()).thenReturn(instances);

        Alert alert = new Alert(1L, 1L, AlertType.ERROR_DETECTED, "System error", 0, alertStart, alertEnd);
        TypedQuery<Alert> mockAlertQuery = Mockito.mock(TypedQuery.class);
        when(entityManager.createQuery(contains("Alert"), eq(Alert.class))).thenReturn(mockAlertQuery);
        when(mockAlertQuery.setParameter("instanceIds", List.of(1L))).thenReturn(mockAlertQuery);
        when(mockAlertQuery.setParameter("cpuHigh", AlertType.CPU_HIGH)).thenReturn(mockAlertQuery);
        when(mockAlertQuery.getResultList()).thenReturn(List.of(alert));

        // Act
        ClientSlaResponse slaResponse = clientService.getClientSla(clientId);

        // Assert
        assertNotNull(slaResponse);
        double expectedSla = ((double)(expectedMinutes - downtimeMinutes) / expectedMinutes) * 100.0;
        expectedSla = Math.round(expectedSla * 100.0) / 100.0;
        assertEquals(expectedSla, slaResponse.getSlaPercentage());
        assertEquals(95.0, slaResponse.getTargetSla());
        assertEquals("NORMAL", slaResponse.getStatus());
    }
}
