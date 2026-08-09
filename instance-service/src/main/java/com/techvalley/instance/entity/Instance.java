package com.techvalley.instance.entity;

import com.techvalley.instance.enums.InstanceStatus;
import com.techvalley.instance.enums.InstanceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "instances")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor
public class Instance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK (bigint)

    private Long clientId; // FK tham chiếu tới clients.id

    private String instanceName;

    @Enumerated(EnumType.STRING)
    private InstanceType instanceType; // SMALL, MEDIUM, LARGE

    private String region;

    @Enumerated(EnumType.STRING)
    private InstanceStatus status; // RUNNING, STOPPED, ERROR

    private Float cpuUsage;

    private Float monthlyCost;

    private LocalDateTime launcheAt;
    private LocalDateTime updateAt;

    @Version
    private Long version;
}
