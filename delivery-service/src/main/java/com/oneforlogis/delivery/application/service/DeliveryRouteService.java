package com.oneforlogis.delivery.application.service;

import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.delivery.application.dto.DeliveryRouteRequest;
import com.oneforlogis.delivery.application.dto.DeliveryRouteResponse;
import com.oneforlogis.delivery.config.DistanceCalculator;
import com.oneforlogis.delivery.domain.model.Delivery;
import com.oneforlogis.delivery.domain.model.DeliveryRoute;
import com.oneforlogis.delivery.domain.model.DeliveryRouteStatus;
import com.oneforlogis.delivery.domain.repository.DeliveryRepository;
import com.oneforlogis.delivery.domain.repository.DeliveryRouteRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryRouteService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryRouteRepository deliveryRouteRepository;
    private final DistanceCalculator distanceCalculator;

    @Transactional
    public DeliveryRouteResponse appendEvent(UUID deliveryId, DeliveryRouteRequest req) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new CustomException(ErrorCode.DELIVERY_NOT_FOUND));

        if (req.eventAt() == null) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        if (requiresHubId(req.routeStatus()) && req.hubId() == null) {
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        int nextSeq = deliveryRouteRepository.findMaxSequenceByDeliveryId(deliveryId).orElse(0) + 1;

        DeliveryRoute route = DeliveryRoute.create(
                delivery.getDeliveryId(),
                nextSeq,
                req.routeStatus(),
                req.hubId(),
                req.latitude(),
                req.longitude(),
                req.eventAt(),
                req.remark()
        );
        deliveryRouteRepository.save(route);

        if (req.latitude() != null && req.longitude() != null) {
            deliveryRouteRepository
                    .findTopByDeliveryIdAndRouteSeqLessThanOrderByRouteSeqDesc(deliveryId, nextSeq)
                    .ifPresent(prev -> {
                        if (prev.getLatitude() != null && prev.getLongitude() != null) {
                            double legKm = distanceCalculator.distanceKm(
                                    prev.getLatitude(), prev.getLongitude(),
                                    req.latitude(), req.longitude()
                            );
                            int legMin = distanceCalculator.estimateMinutes(legKm);
                            delivery.addTravelProgress(legKm, legMin);
                        }
                    });
        }

        deliveryRepository.save(delivery);
        return DeliveryRouteResponse.from(route);
    }

    private boolean requiresHubId(DeliveryRouteStatus status) {
        return status == DeliveryRouteStatus.ARRIVED_AT_HUB ||
                status == DeliveryRouteStatus.DEPARTED_FROM_HUB;
    }
}