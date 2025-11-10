package com.oneforlogis.delivery.infrastructure.repository;

import com.oneforlogis.delivery.application.dto.DeliverySearchCond;
import com.oneforlogis.delivery.domain.model.Delivery;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class DeliverySpecifications {

    public static Specification<Delivery> search(DeliverySearchCond cond) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (cond.getStatus() != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("status"), cond.getStatus()));
            }

            if (cond.getReceiverName() != null) {
                predicate = cb.and(predicate,
                        cb.like(root.get("receiverName"), "%" + cond.getReceiverName() + "%"));
            }

            if (cond.getStartHubId() != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("startHubId"), cond.getStartHubId()));
            }

            if (cond.getDestinationHubId() != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("destinationHubId"), cond.getDestinationHubId()));
            }

            return predicate;
        };
    }
}