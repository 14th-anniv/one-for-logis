package com.oneforlogis.notification.domain.model;

// 메세지 발송 상태
public enum MessageStatus {
    // 발송 대기 상태
    PENDING,

    // 발송 완료
    SENT,

    // 발송 실패
    FAILED
}
