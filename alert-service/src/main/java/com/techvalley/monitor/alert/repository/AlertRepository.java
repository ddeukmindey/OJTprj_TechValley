package com.techvalley.monitor.alert.repository;

import com.techvalley.monitor.alert.Alert;
import com.techvalley.monitor.enums.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long>, JpaSpecificationExecutor<Alert> {
    List<Alert> findByInstanceId(Long instanceId);
    Optional<Alert> findFirstByInstanceIdAndAlertTypeAndIsResolved(Long instanceId, AlertType alertType, Integer isResolved);
    long countByIsResolved(Integer isResolved);

    long countByInstanceIdInAndIsResolved(List<Long> instanceIds, Integer isResolved);

    List<Alert> findByInstanceIdInAndAlertTypeNot(List<Long> instanceIds, AlertType excludedType);
}