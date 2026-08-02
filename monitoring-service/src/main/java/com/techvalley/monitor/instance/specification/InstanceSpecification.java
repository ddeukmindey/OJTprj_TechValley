package com.techvalley.monitor.instance.specification;

import com.techvalley.monitor.enums.InstanceStatus;
import com.techvalley.monitor.instance.Instance;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class InstanceSpecification {

    public static Specification<Instance> hasClientId(Long clientId) {
        return (root, query, cb) -> {
            if (clientId == null) return null;
            return cb.equal(root.get("clientId"), clientId);
        };
    }

    public static Specification<Instance> hasStatus(InstanceStatus status) {
        return (root, query, cb) -> {
            if (status == null) return null;
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Instance> hasRegion(String region) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(region)) return null;
            return cb.equal(root.get("region"), region);
        };
    }
}
