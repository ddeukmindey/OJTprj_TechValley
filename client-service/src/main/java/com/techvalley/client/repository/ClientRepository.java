package com.techvalley.client.repository;

import com.techvalley.client.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long>, JpaSpecificationExecutor<Client> {

    Page<Client> findByClientNameContainingIgnoreCase(String clientName, Pageable pageable);

    Page<Client> findByManagerId(Long managerId, Pageable pageable);

    Page<Client> findByManagerIdAndClientNameContainingIgnoreCase(Long managerId, String clientName, Pageable pageable);

    List<Client> findByManagerId(Long managerId);
}
