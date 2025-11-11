package com.oneforlogis.delivery.application.dto;

import com.oneforlogis.delivery.domain.model.Delivery;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
public class DeliveryResponse {

    private final UUID id;
    private final UUID orderId;
    private final String status;
    private final UUID fromHubId;
    private final UUID toHubId;

    private final Double estimatedDistanceKm;
    private final Integer estimatedDurationMin;
    private final Boolean arrivedDestinationHub;

    private final LocalDateTime destinationHubArrivedAt;
    private final Long deliveryStaffId;
    private final String receiverName;
    private final String receiverAddress;
    private final String receiverSlackId;

    @Builder
    public DeliveryResponse(
            UUID id,
            UUID orderId,
            String status,
            UUID fromHubId,
            UUID toHubId,
            Double estimatedDistanceKm,
            Integer estimatedDurationMin,
            Boolean arrivedDestinationHub,
            LocalDateTime destinationHubArrivedAt,
            Long deliveryStaffId,
            String receiverName,
            String receiverAddress,
            String receiverSlackId
    ) {
        this.id = id;
        this.orderId = orderId;
        this.status = status;
        this.fromHubId = fromHubId;
        this.toHubId = toHubId;

        this.estimatedDistanceKm = (estimatedDistanceKm != null) ? estimatedDistanceKm : 0.0;
        this.estimatedDurationMin = (estimatedDurationMin != null) ? estimatedDurationMin : 0;
        this.arrivedDestinationHub =
                (arrivedDestinationHub != null) ? arrivedDestinationHub : false;

        this.destinationHubArrivedAt = destinationHubArrivedAt;
        this.deliveryStaffId = deliveryStaffId;
        this.receiverName = receiverName;
        this.receiverAddress = receiverAddress;
        this.receiverSlackId = receiverSlackId;
    }

    public static DeliveryResponse from(Delivery d) {
        return DeliveryResponse.builder()
                .id(d.getDeliveryId())
                .orderId(d.getOrderId())
                .status(d.getStatus().name())
                .fromHubId(UUID.fromString(d.getStartHubId()))
                .toHubId(UUID.fromString(d.getDestinationHubId()))
                .destinationHubArrivedAt(null)
                .deliveryStaffId(
                        d.getDeliveryStaffId() != null ? Long.valueOf(d.getDeliveryStaffId())
                                : null)
                .receiverName(d.getReceiverName())
                .receiverAddress(d.getReceiverAddress())
                .receiverSlackId(d.getReceiverSlackId())
                .build();
    }
}