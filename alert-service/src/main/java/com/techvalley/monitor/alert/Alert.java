package com.techvalley.monitor.alert;

import com.techvalley.monitor.enums.AlertType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK (bigint)

    @Version
    private Long version; // Optimistic Locking – chống 2 người resolve cùng 1 alert

    private Long instanceId; // FK tham chiếu tới instances.id

    @Enumerated(EnumType.STRING)
    private AlertType alertType; // CPU_HIGH, ERROR_DETECTED, LONG_STOPPED

    private String message;

    private Integer isResolved; // 0: Unresolved, 1: Resolved

    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
}