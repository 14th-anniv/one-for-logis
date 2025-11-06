package com.oneforlogis.hub.domain.model;

import com.oneforlogis.common.model.BaseEntity;
import com.oneforlogis.hub.presentation.request.HubRouteCreateRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "p_hub_route")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class HubRoute extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID fromHubId;

    @Column(nullable = false)
    private UUID toHubId;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal routeDistance; // km 단위

    @Column(nullable = false)
    private Integer routeTime; // 분 단위

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RouteType routeType;

    @Builder
    public HubRoute(UUID fromHubId, UUID toHubId, Integer routeTime, BigDecimal routeDistance, RouteType routeType) {
        this.fromHubId = fromHubId;
        this.toHubId = toHubId;
        this.routeDistance = routeDistance;
        this.routeTime = routeTime;
        this.routeType = routeType;
    }

    public static HubRoute create(HubRouteCreateRequest request) {
        return HubRoute.builder()
                .fromHubId(request.fromHubId())
                .toHubId(request.toHubId())
                .routeDistance(request.routeDistance())
                .routeTime(request.routeTime())
                .routeType(request.routeType())
                .build();
    }
}
