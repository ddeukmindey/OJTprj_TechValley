package com.techvalley.instance.specification;

import com.techvalley.instance.enums.InstanceStatus;
import com.techvalley.instance.enums.InstanceType;
import com.techvalley.instance.entity.Instance;
import org.springframework.data.jpa.domain.Specification;

public final class InstanceSpecification {

    private InstanceSpecification() {
    }

    public static Specification<Instance> hasClientId(Long clientId) {
        return (root, query, criteriaBuilder) ->
                clientId == null ? null : criteriaBuilder.equal(root.get("clientId"), clientId);
    }

    public static Specification<Instance> hasStatus(InstanceStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? null : criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Instance> hasInstanceType(InstanceType instanceType) {
        return (root, query, criteriaBuilder) ->
                instanceType == null ? null : criteriaBuilder.equal(root.get("instanceType"), instanceType);
    }

    public static Specification<Instance> hasRegion(String region) {
        return (root, query, criteriaBuilder) ->
                (region == null || region.isBlank()) ? null : criteriaBuilder.equal(root.get("region"), region);
    }

    public static Specification<Instance> searchByName(String search) {
        return (root, query, criteriaBuilder) ->
                (search == null || search.isBlank()) ? null : criteriaBuilder.like(criteriaBuilder.lower(root.get("instanceName")), "%" + search.toLowerCase() + "%");
    }
}
