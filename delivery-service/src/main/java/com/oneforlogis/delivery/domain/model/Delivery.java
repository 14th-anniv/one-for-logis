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
    private Long deliveryStaffId;

    private Delivery(
            UUID deliveryId,
            UUID orderId,
            DeliveryStatus status,
            String startHubId,
            String destinationHubId,
            String receiverName,
            String receiverAddress,
            String receiverSlackId,
            Long deliveryStaffId
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

    public void updateStatus(DeliveryStatus newStatus) {
        if (this.status == newStatus) {
            throw new IllegalStateException("이미 동일한 상태입니다.");
        }

        switch (this.status) {
            case WAITING_AT_HUB -> {
                if (newStatus != DeliveryStatus.MOVING_BETWEEN_HUBS
                        && newStatus != DeliveryStatus.OUT_FOR_DELIVERY) {
                    throw new IllegalStateException(
                            "허브 대기 중 상태에서는 '허브 이동 중' 또는 '배송 중'으로만 변경할 수 있습니다.");
                }
            }
            case MOVING_BETWEEN_HUBS -> {
                if (newStatus != DeliveryStatus.ARRIVED_DEST_HUB
                        && newStatus != DeliveryStatus.CANCELED) {
                    throw new IllegalStateException(
                            "허브 이동 중 상태에서는 '목적지 허브 도착' 또는 '취소'로만 변경할 수 있습니다.");
                }
            }
            case ARRIVED_DEST_HUB -> {
                if (newStatus != DeliveryStatus.OUT_FOR_DELIVERY
                        && newStatus != DeliveryStatus.MOVING_TO_COMPANY
                        && newStatus != DeliveryStatus.CANCELED) {
                    throw new IllegalStateException(
                            "목적지 허브 도착 상태에서는 '배송 중', '업체 이동 중' 또는 '취소'로만 변경할 수 있습니다.");
                }
            }
            case OUT_FOR_DELIVERY, MOVING_TO_COMPANY -> {
                if (newStatus != DeliveryStatus.COMPLETED
                        && newStatus != DeliveryStatus.CANCELED) {
                    throw new IllegalStateException(
                            "배송/업체 이동 중 상태에서는 '배송 완료' 또는 '취소'로만 변경할 수 있습니다.");
                }
            }
            case COMPLETED, CANCELED ->
                    throw new IllegalStateException("이미 완료되었거나 취소된 배송은 상태를 변경할 수 없습니다.");
        }

        this.status = newStatus;
    }

    public void assignStaff(Long staffId) {
        if (this.status == DeliveryStatus.COMPLETED || this.status == DeliveryStatus.CANCELED) {
            throw new IllegalStateException("이미 완료되었거나 취소된 배송에는 배정할 수 없습니다.");
        }
        this.deliveryStaffId = staffId;
    }

    public void unassignStaff() {
        if (this.status == DeliveryStatus.COMPLETED || this.status == DeliveryStatus.CANCELED) {
            throw new IllegalStateException("완료/취소된 배송은 배정 해제할 수 없습니다.");
        }
        this.deliveryStaffId = null;
    }

    public void applyRouteEvent(DeliveryRouteStatus routeStatus) {
        switch (routeStatus) {
            case ARRIVED_AT_HUB -> {
                if (this.status == DeliveryStatus.WAITING_AT_HUB) {
                    this.status = DeliveryStatus.MOVING_BETWEEN_HUBS;
                } else if (this.status == DeliveryStatus.MOVING_BETWEEN_HUBS) {
                    this.status = DeliveryStatus.ARRIVED_DEST_HUB;
                }
            }
            case DEPARTED_FROM_HUB -> {
                if (this.status == DeliveryStatus.ARRIVED_DEST_HUB) {
                    this.status = DeliveryStatus.OUT_FOR_DELIVERY;
                }
            }
            case PICKED_UP -> {
                if (this.status == DeliveryStatus.OUT_FOR_DELIVERY) {
                    this.status = DeliveryStatus.MOVING_TO_COMPANY;
                }
            }
            case DROPPED_OFF -> {
                if (this.status == DeliveryStatus.MOVING_TO_COMPANY) {
                    this.status = DeliveryStatus.COMPLETED;
                }
            }
            case IN_TRANSIT -> {
                // 상태 전이 없음
            }
        }
    }

    public void addTravelProgress(Double addKm, Integer addMin) {
        if (addKm != null) {
            this.estimatedDistanceKm =
                    (this.estimatedDistanceKm == null ? 0.0 : this.estimatedDistanceKm) + addKm;
        }
        if (addMin != null) {
            this.estimatedDurationMin =
                    (this.estimatedDurationMin == null ? 0 : this.estimatedDurationMin) + addMin;
        }
    }
}