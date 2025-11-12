package com.oneforlogis.order.domain.model;

public enum OrderStatus {
    PENDING,    // 주문접수
    PAID,       // 결제완료
    PACKING,    // 출고준비
    SHIPPED,    // 배송중
    DELIVERED,  // 배송완료
    CANCELED    // 주문취소
}

