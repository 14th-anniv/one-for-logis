package com.oneforlogis.hub.domain.model;

import com.oneforlogis.common.model.BaseEntity;
import com.oneforlogis.hub.presentation.request.HubRouteRequest;
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

    @Column(columnDefinition = "jsonb")
    private String pathNodes; // 중간 경유지

    @Builder
    public HubRoute(UUID fromHubId, UUID toHubId, Integer routeTime, BigDecimal routeDistance, RouteType routeType,  String pathNodes) {
        this.fromHubId = fromHubId;
        this.toHubId = toHubId;
        this.routeDistance = routeDistance;
        this.routeTime = routeTime;
        this.routeType = routeType;
        this.pathNodes = pathNodes;
    }

    public static HubRoute create(HubRouteRequest request) {
        return com.oneforlogis.hub.domain.model.HubRoute.builder()
                .fromHubId(request.fromHubId())
                .toHubId(request.toHubId())
                .routeDistance(request.routeDistance())
                .routeTime(request.routeTime())
                .routeType(RouteType.DIRECT)
                .build();
    }

    public void update(HubRouteRequest request) {
        this.fromHubId = request.fromHubId();
        this.toHubId = request.toHubId();
        this.routeDistance = request.routeDistance();
        this.routeTime = request.routeTime();
    }
}
