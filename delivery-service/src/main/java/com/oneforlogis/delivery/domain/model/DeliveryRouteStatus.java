package com.oneforlogis.delivery.domain.model;

public enum DeliveryRouteStatus {
    ARRIVED_AT_HUB,     // 허브 도착
    DEPARTED_FROM_HUB,  // 허브 출발
    PICKED_UP,          // 집화
    DROPPED_OFF,        // 하차/인도
    IN_TRANSIT          // 이동 중
}