package com.oneforlogis.delivery.infrastructure.repository;

import com.oneforlogis.delivery.application.dto.request.DeliverySearchCond;
import com.oneforlogis.delivery.domain.model.Delivery;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class DeliverySpecifications {

    public static Specification<Delivery> search(DeliverySearchCond cond) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (cond.status() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), cond.status()));
            }

            if (cond.receiverName() != null && !cond.receiverName().isBlank()) {
                predicate = cb.and(predicate,
                        cb.like(root.get("receiverName"), "%" + cond.receiverName() + "%"));
            }

            if (cond.orderId() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("orderId"), cond.orderId()));
            }

            if (cond.fromHubId() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("startHubId"), cond.fromHubId()));
            }

            if (cond.toHubId() != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("destinationHubId"), cond.toHubId()));
            }

            return predicate;
        };
    }
}