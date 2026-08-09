package com.techvalley.instance.repository;

import com.techvalley.instance.enums.InstanceStatus;
import com.techvalley.instance.entity.Instance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstanceRepository extends JpaRepository<Instance, Long>, JpaSpecificationExecutor<Instance> {

    List<Instance> findByClientId(Long clientId);

    Page<Instance> findByClientId(Long clientId, Pageable pageable);

    List<Instance> findByStatus(InstanceStatus status);

    List<Instance> findByCpuUsageGreaterThanEqual(Float cpuUsage);

    boolean existsByClientId(Long clientId);
}
