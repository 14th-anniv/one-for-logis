package com.oneforlogis.delivery.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_deliveries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_delivery_order", columnNames = {"order_id"})
})
public class Delivery {

    @Id
    @Column(name = "delivery_id", nullable = false, updatable = false)
    private UUID deliveryId;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private DeliveryStatus status;

    @Column(name = "start_hub_id", nullable = false, length = 64)
    private String startHubId;

    @Column(name = "estimated_distance_km")
    private Double estimatedDistanceKm;

    @Column(name = "estimated_duration_min")
    private Integer estimatedDurationMin;

    @Column(name = "arrived_destination_hub")
    private Boolean arrivedDestinationHub;

    @Column(name = "destination_hub_arrived_at")
    private LocalDateTime destinationHubArrivedAt;

    @Column(name = "destination_hub_id", nullable = false, length = 64)
    private String destinationHubId;

    @Column(name = "receiver_name", nullable = false, length = 100)
    private String receiverName;

    @Column(name = "receiver_address", nullable = false, length = 255)
    private String receiverAddress;

    @Column(name = "receiver_slack_id", length = 64)
    private String receiverSlackId;

    @Column(name = "delivery_staff_id", length = 64)
    private String deliveryStaffId;

    private Delivery(
            UUID deliveryId,
            UUID orderId,
            DeliveryStatus status,
            String startHubId,
            String destinationHubId,
            String receiverName,
            String receiverAddress,
            String receiverSlackId,
            String deliveryStaffId
    ) {
        this.deliveryId = deliveryId;
        this.orderId = orderId;
        this.status = status;
        this.startHubId = startHubId;
        this.destinationHubId = destinationHubId;
        this.receiverName = receiverName;
        this.receiverAddress = receiverAddress;
        this.receiverSlackId = receiverSlackId;
        this.deliveryStaffId = deliveryStaffId;

        this.estimatedDistanceKm = 0.0;
        this.estimatedDurationMin = 0;
        this.arrivedDestinationHub = false;
        this.destinationHubArrivedAt = null;
    }

    public static Delivery createFromOrder(
            UUID orderId,
            String startHubId,
            String destinationHubId,
            String receiverName,
            String receiverAddress,
            String receiverSlackId
    ) {
        return new Delivery(
                UUID.randomUUID(),
                orderId,
                DeliveryStatus.WAITING_AT_HUB,
                startHubId,
                destinationHubId,
                receiverName,
                receiverAddress,
                receiverSlackId,
                null
        );
    }
}