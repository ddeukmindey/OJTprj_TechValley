package com.techvalley.llm.dto.response;

import java.util.List;

public class InstanceDiagnosisResponse {
    private Long instanceId;
    private String instanceName;
    private String instanceStatus;  // Trạng thái vận hành máy chủ (RUNNING, STOPPED, ERROR)
    private String issueType;       // Thuộc tính phân loại sự cố (HIGH_CPU, SYSTEM_ERROR, CRITICAL, NONE)
    private Integer healthScore;    // Điểm sức khỏe máy chủ (0 - 100)
    private String diagnosis;       // Tóm tắt tình trạng phân tích
    private String rootCause;       // Nguyên nhân kỹ thuật chi tiết
    private List<String> actionableSteps; // Các bước khắc phục đề xuất
    private Boolean isMockResponse;

    public InstanceDiagnosisResponse() {
    }

    public InstanceDiagnosisResponse(Long instanceId, String instanceName, String instanceStatus, String issueType, Integer healthScore, String diagnosis, String rootCause, List<String> actionableSteps, Boolean isMockResponse) {
        this.instanceId = instanceId;
        this.instanceName = instanceName;
        this.instanceStatus = instanceStatus;
        this.issueType = issueType;
        this.healthScore = healthScore;
        this.diagnosis = diagnosis;
        this.rootCause = rootCause;
        this.actionableSteps = actionableSteps;
        this.isMockResponse = isMockResponse;
    }

    public Long getInstanceId() { return instanceId; }
    public void setInstanceId(Long instanceId) { this.instanceId = instanceId; }

    public String getInstanceName() { return instanceName; }
    public void setInstanceName(String instanceName) { this.instanceName = instanceName; }

    public String getInstanceStatus() { return instanceStatus; }
    public void setInstanceStatus(String instanceStatus) { this.instanceStatus = instanceStatus; }

    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }

    public Integer getHealthScore() { return healthScore; }
    public void setHealthScore(Integer healthScore) { this.healthScore = healthScore; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }

    public List<String> getActionableSteps() { return actionableSteps; }
    public void setActionableSteps(List<String> actionableSteps) { this.actionableSteps = actionableSteps; }

    public Boolean getIsMockResponse() { return isMockResponse; }
    public void setIsMockResponse(Boolean isMockResponse) { this.isMockResponse = isMockResponse; }
}
