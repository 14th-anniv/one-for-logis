package com.oneforlogis.hub.domain.model;

import com.oneforlogis.common.model.BaseEntity;
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

    @Column(nullable = false)
    private Integer routeTime; // 분 단위

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal routeDistance; // km 단위

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RouteType routeType;
}
