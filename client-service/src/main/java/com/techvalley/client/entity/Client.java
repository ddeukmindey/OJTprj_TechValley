package com.techvalley.client.entity;

import com.techvalley.client.enums.ContractPlan;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Column(nullable = false, unique = true)
    private String clientName;

    @Enumerated(EnumType.STRING)
    private ContractPlan contractPlan; // BASIC, STANDARD, PREMIUM

    private LocalDateTime createAt;

    private Long managerId; // FK tham chiếu tới members.id (Chỉ member có role CLIENT_MANAGER)

    @Version
    private Long version;
}
