package com.oneforlogis.delivery.application.dto;

import com.oneforlogis.delivery.domain.model.DeliveryRoute;
import com.oneforlogis.delivery.domain.model.DeliveryRouteStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryRouteResponse(

        UUID routeId,
        UUID deliveryId,
        Integer routeSeq,
        DeliveryRouteStatus routeStatus,
        String hubId,
        Double latitude,
        Double longitude,
        LocalDateTime eventAt,
        String remark
) {

    public static DeliveryRouteResponse from(DeliveryRoute r) {
        return new DeliveryRouteResponse(
                r.getRouteId(),
                r.getDeliveryId(),
                r.getRouteSeq(),
                r.getRouteStatus(),
                r.getHubId(),
                r.getLatitude(),
                r.getLongitude(),
                r.getEventAt(),
                r.getRemark()
        );
    }
}