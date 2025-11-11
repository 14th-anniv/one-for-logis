package com.oneforlogis.delivery.domain.model;

public enum DeliveryStatus {
    WAITING_AT_HUB,          // 허브 대기 중
    MOVING_BETWEEN_HUBS,     // 허브 이동 중
    ARRIVED_DEST_HUB,        // 목적지 허브 도착
    OUT_FOR_DELIVERY,        // 배송 중
    MOVING_TO_COMPANY,       // 업체 이동 중
    COMPLETED,               // 배송 완료
    CANCELED                 // 취소
}