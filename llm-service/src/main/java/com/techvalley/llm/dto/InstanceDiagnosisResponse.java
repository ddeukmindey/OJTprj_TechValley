package com.techvalley.llm.dto;

import java.util.List;

public class InstanceDiagnosisResponse {
    public Long instanceId;
    public String instanceName;
    public int healthScore;
    public String diagnosis;
    public String rootCause;
    public List<String> actionableSteps;
}
