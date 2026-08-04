package com.techvalley.monitor.monitoring.specification;

import com.techvalley.monitor.enums.InstanceStatus;
import com.techvalley.monitor.instance.Instance;
import org.springframework.data.jpa.domain.Specification;

public class MonitoringSpecification {

    public static Specification<Instance> hasCpuGreaterThanEqual(Float cpuThreshold) {
        return (root, query, criteriaBuilder) ->
                cpuThreshold == null ? null : criteriaBuilder.greaterThanOrEqualTo(root.get("cpuUsage"), cpuThreshold);
    }

    public static Specification<Instance> hasStatus(InstanceStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? null : criteriaBuilder.equal(root.get("status"), status);
    }
}
