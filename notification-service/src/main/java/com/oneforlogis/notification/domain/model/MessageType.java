package com.oneforlogis.notification.domain.model;

// 메시지 타입
public enum MessageType {

    // 주문 생성 시 알림 (AI 출발시간 계산 포함)
    ORDER_NOTIFICATION,

    // 일일 경로 최적화 알림 (Challenge 기능, 매일 06:00)
    DAILY_ROUTE,

    DELIVERY_STATUS_UPDATE, // 사용자가 직접 발송하는 수동 메시지

    MANUAL
}
