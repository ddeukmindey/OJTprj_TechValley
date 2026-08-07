package com.techvalley.llm.dto.client;

public class InstanceDto {
    private Long id;
    private Long clientId;
    private String instanceName;
    private String instanceType;
    private String region;
    private String status;
    private Double cpuUsage;
    private Double monthlyCost;
    private String launcheAt;

    public InstanceDto() {
    }

    public InstanceDto(Long id, Long clientId, String instanceName, String instanceType, String region, String status, Double cpuUsage, Double monthlyCost, String launcheAt) {
        this.id = id;
        this.clientId = clientId;
        this.instanceName = instanceName;
        this.instanceType = instanceType;
        this.region = region;
        this.status = status;
        this.cpuUsage = cpuUsage;
        this.monthlyCost = monthlyCost;
        this.launcheAt = launcheAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getInstanceName() { return instanceName; }
    public void setInstanceName(String instanceName) { this.instanceName = instanceName; }

    public String getInstanceType() { return instanceType; }
    public void setInstanceType(String instanceType) { this.instanceType = instanceType; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(Double cpuUsage) { this.cpuUsage = cpuUsage; }

    public Double getMonthlyCost() { return monthlyCost; }
    public void setMonthlyCost(Double monthlyCost) { this.monthlyCost = monthlyCost; }

    public String getLauncheAt() { return launcheAt; }
    public void setLauncheAt(String launcheAt) { this.launcheAt = launcheAt; }
}
