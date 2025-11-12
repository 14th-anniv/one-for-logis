package com.oneforlogis.delivery.domain.repository;

import com.oneforlogis.delivery.domain.model.DeliveryRoute;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface DeliveryRouteRepository extends JpaRepository<DeliveryRoute, UUID> {

    @Query("select max(r.routeSeq) from DeliveryRoute r where r.deliveryId = :deliveryId")
    Optional<Integer> findMaxSequenceByDeliveryId(@Param("deliveryId") UUID deliveryId);

    Optional<DeliveryRoute> findTopByDeliveryIdAndRouteSeqLessThanOrderByRouteSeqDesc(
            UUID deliveryId, int routeSeq
    );
}