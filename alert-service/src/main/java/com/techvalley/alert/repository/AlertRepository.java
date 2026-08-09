package com.techvalley.alert.repository;

import com.techvalley.alert.entity.Alert;
import com.techvalley.alert.enums.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long>, JpaSpecificationExecutor<Alert> {

    Optional<Alert> findFirstByInstanceIdAndAlertTypeAndIsResolved(Long instanceId, AlertType alertType, Boolean isResolved);

    void deleteByInstanceId(Long instanceId);
}
