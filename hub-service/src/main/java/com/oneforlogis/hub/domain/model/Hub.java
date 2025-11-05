package com.oneforlogis.hub.domain.model;

import com.oneforlogis.common.model.BaseEntity;
import com.oneforlogis.hub.presentation.request.HubCreateRequest;
import com.oneforlogis.hub.presentation.request.HubUpdateRequest;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_hub")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Hub extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private String name;
    private String address;
    private BigDecimal lat;
    private BigDecimal lon;

    @Builder
    public Hub(String name, String address, BigDecimal lat, BigDecimal lon) {
        this.name = name;
        this.address = address;
        this.lat = lat;
        this.lon = lon;
    }

    public static Hub create(HubCreateRequest request) {
        return Hub.builder()
                .name(request.name())
                .address(request.address())
                .lat(request.lat())
                .lon(request.lon())
                .build();
    }

    public void update(HubUpdateRequest request) {
        this.name = request.name();
        this.address = request.address();
        this.lat = request.lat();
        this.lon = request.lon();
    }
}