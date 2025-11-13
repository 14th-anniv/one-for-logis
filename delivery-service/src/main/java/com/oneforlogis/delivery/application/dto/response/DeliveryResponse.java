package com.oneforlogis.delivery.application.dto.response;

import com.oneforlogis.delivery.domain.model.Delivery;
import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryResponse(
        UUID id,
        UUID orderId,
        String status,
        UUID fromHubId,
        UUID toHubId,
        // TODO: 경로/거리 계산 도메인 구현 후 매핑 예정
        Double estimatedDistanceKm,
        Integer estimatedDurationMin,
        Boolean arrivedDestinationHub,
        LocalDateTime destinationHubArrivedAt,
        Long deliveryStaffId,
        String receiverName,
        String receiverAddress,
        String receiverSlackId
) {

    // TODO: 계산 로직 연동 전까지 기본값 사용
    public DeliveryResponse {
        if (estimatedDistanceKm == null) {
            estimatedDistanceKm = 0.0;
        }
        if (estimatedDurationMin == null) {
            estimatedDurationMin = 0;
        }
        if (arrivedDestinationHub == null) {
            arrivedDestinationHub = false;
        }
    }

    public static DeliveryResponse from(Delivery d) {
        return new DeliveryResponse(
                d.getDeliveryId(),
                d.getOrderId(),
                d.getStatus().name(),
                UUID.fromString(d.getStartHubId()),
                UUID.fromString(d.getDestinationHubId()),
                null,
                null,
                null,
                null,
                d.getDeliveryStaffId() != null ? Long.valueOf(d.getDeliveryStaffId()) : null,
                d.getReceiverName(),
                d.getReceiverAddress(),
                d.getReceiverSlackId()
        );
    }
}