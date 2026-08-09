package com.techvalley.client.specification;

import com.techvalley.client.entity.Client;
import org.springframework.data.jpa.domain.Specification;

public final class ClientSpecification {

    private ClientSpecification() {
    }

    public static Specification<Client> hasName(String search) {
        return (root, query, cb) -> (search == null || search.isBlank())
                ? null
                : cb.like(cb.lower(root.get("clientName")), "%" + search.toLowerCase() + "%");
    }

    public static Specification<Client> hasManagerId(Long managerId) {
        return (root, query, cb) -> managerId == null
                ? null
                : cb.equal(root.get("managerId"), managerId);
    }
}
