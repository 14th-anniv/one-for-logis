package com.oneforlogis.delivery.domain.repository;

import com.oneforlogis.delivery.domain.model.Delivery;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID>,
        JpaSpecificationExecutor<Delivery> {

    boolean existsByOrderId(UUID orderId);

    Optional<Delivery> findByOrderId(UUID orderId);

    Optional<Delivery> findByDeliveryId(UUID deliveryId);
}