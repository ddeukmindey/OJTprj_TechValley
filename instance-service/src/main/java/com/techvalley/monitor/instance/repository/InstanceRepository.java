package com.techvalley.monitor.instance.repository;

import com.techvalley.monitor.enums.InstanceStatus;
import com.techvalley.monitor.instance.Instance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InstanceRepository extends JpaRepository<Instance, Long>, JpaSpecificationExecutor<Instance> {

    List<Instance> findByCpuUsageGreaterThanEqual(Float cpuUsageThreshold);

    List<Instance> findByStatus(InstanceStatus status);

    List<Instance> findByStatusAndUpdateAtBefore(InstanceStatus status, LocalDateTime thresholdDate);

    long countByStatus(InstanceStatus status);

    List<Instance> findByClientIdInAndCpuUsageGreaterThanEqual(List<Long> clientIds, Float cpuUsageThreshold);

    List<Instance> findByClientIdInAndStatus(List<Long> clientIds, InstanceStatus status);

    List<Instance> findByClientIdIn(List<Long> clientIds);

    long countByClientIdInAndStatus(List<Long> clientIds, InstanceStatus status);
}