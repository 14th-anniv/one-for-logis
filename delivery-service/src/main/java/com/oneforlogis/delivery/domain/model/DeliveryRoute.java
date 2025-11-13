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
@Table(name = "delivery_route",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_delivery_route_delivery_seq",
                columnNames = {"delivery_id", "route_seq"}))
public class DeliveryRoute {

    @Id
    @Column(name = "route_id", nullable = false, updatable = false)
    private UUID routeId;

    @Column(name = "delivery_id", nullable = false, updatable = false)
    private UUID deliveryId;

    @Column(name = "route_seq", nullable = false)
    private Integer routeSeq;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private DeliveryRouteStatus routeStatus;

    @Column(name = "hub_id", length = 64)
    private String hubId;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "event_at", nullable = false)
    private LocalDateTime eventAt;

    @Column(name = "remark", length = 255)
    private String remark;

    private DeliveryRoute(
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
        this.routeId = routeId;
        this.deliveryId = deliveryId;
        this.routeSeq = routeSeq;
        this.routeStatus = routeStatus;
        this.hubId = hubId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.eventAt = eventAt;
        this.remark = remark;
    }

    public static DeliveryRoute create(
            UUID deliveryId,
            int routeSeq,
            DeliveryRouteStatus routeStatus,
            String hubId,
            Double latitude,
            Double longitude,
            LocalDateTime eventAt,
            String remark
    ) {
        return new DeliveryRoute(
                UUID.randomUUID(),
                deliveryId,
                routeSeq,
                routeStatus,
                hubId,
                latitude,
                longitude,
                eventAt,
                remark
        );
    }
}