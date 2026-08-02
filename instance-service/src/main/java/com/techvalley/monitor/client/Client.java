package com.techvalley.monitor.client;

import com.techvalley.monitor.enums.ContractPlan;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "clients")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK (bigint)

    @Column(nullable = false)
    private String clientName;

    @Enumerated(EnumType.STRING)
    private ContractPlan contractPlan; // BASIC, STANDARD, PREMIUM

    private Long managerId; // FK tham chiếu tới members.id

    private LocalDateTime createAt;
}