package com.techvalley.llm.dto.client;

public class AlertDto {
    private Long id;
    private Long instanceId;
    private String alertType;
    private String message;
    private Boolean isResolved;
    private String detectedAt;

    public AlertDto() {
    }

    public AlertDto(Long id, Long instanceId, String alertType, String message, Boolean isResolved, String detectedAt) {
        this.id = id;
        this.instanceId = instanceId;
        this.alertType = alertType;
        this.message = message;
        this.isResolved = isResolved;
        this.detectedAt = detectedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getInstanceId() { return instanceId; }
    public void setInstanceId(Long instanceId) { this.instanceId = instanceId; }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Boolean getIsResolved() { return isResolved; }
    public void setIsResolved(Boolean isResolved) { this.isResolved = isResolved; }

    public String getDetectedAt() { return detectedAt; }
    public void setDetectedAt(String detectedAt) { this.detectedAt = detectedAt; }
}
