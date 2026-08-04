package com.techvalley.monitor.cost;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cost_snapshots")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor
public class CostSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK (bigint)

    private Long clientId; // FK tham chiếu tới clients.id

    private Integer snapshotMonth;
    private Float totalCost;
    private Integer instanceCount;
    private LocalDateTime createdAt;
}