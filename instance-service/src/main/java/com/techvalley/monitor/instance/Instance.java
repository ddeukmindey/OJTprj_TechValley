package com.techvalley.monitor.instance;

import com.techvalley.monitor.enums.InstanceStatus;
import com.techvalley.monitor.enums.InstanceType;
import jakarta.persistence.*;
import lombok.*;
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

    @Version
    private Long version; // Optimistic Locking – tự tăng mỗi lần update, chống race condition

    private String instanceName;
    private String region;

    @Enumerated(EnumType.STRING)
    private InstanceType instanceType; // SMALL, MEDIUM, LARGE

    @Enumerated(EnumType.STRING)
    private InstanceStatus status; // RUNNING, STOPPED, ERROR

    private Float cpuUsage;
    private Float monthlyCost;

    private Long clientId; // FK tham chiếu tới clients.id

    private LocalDateTime launcheAt;
    private LocalDateTime updateAt;

    @PrePersist
    protected void onCreate() {
        this.launcheAt = LocalDateTime.now();
        this.updateAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateAt = LocalDateTime.now();
    }
}