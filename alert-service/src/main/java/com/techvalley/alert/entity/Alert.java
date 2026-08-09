package com.techvalley.alert.entity;

import com.techvalley.alert.enums.AlertType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.type.NumericBooleanConverter;
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

    private Long instanceId; // FK tham chiếu tới instances.id

    @Enumerated(EnumType.STRING)
    private AlertType alertType; // CPU_HIGH, ERROR_DETECTED, LONG_STOPPED

    private String message;

    @Column(name = "is_resolve")
    @Convert(converter = NumericBooleanConverter.class)
    private Boolean isResolved = false; // true: Resolved, false: Unresolved

    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;

    @Version
    private Long version;
}
