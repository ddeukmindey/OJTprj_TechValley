package com.techvalley.monitor.client.repository;

import com.techvalley.monitor.client.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Page<Client> findByClientNameContainingIgnoreCase(String clientName, Pageable pageable);

    Page<Client> findByManagerId(Long managerId, Pageable pageable);

    Page<Client> findByManagerIdAndClientNameContainingIgnoreCase(Long managerId, String clientName, Pageable pageable);

    List<Client> findByManagerId(Long managerId);
}
