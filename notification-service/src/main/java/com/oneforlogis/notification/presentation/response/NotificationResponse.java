package com.oneforlogis.notification.presentation.response;

import com.oneforlogis.notification.domain.model.MessageStatus;
import com.oneforlogis.notification.domain.model.MessageType;
import com.oneforlogis.notification.domain.model.Notification;
import com.oneforlogis.notification.domain.model.SenderType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "알림 조회 응답 DTO")
public record NotificationResponse(
        @Schema(description = "메시지 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "발신자 타입 (USER: 사용자, SYSTEM: 시스템)", example = "SYSTEM")
        SenderType senderType,

        @Schema(description = "발신자 사용자명 (USER 타입만)", example = "user1")
        String senderUsername,

        @Schema(description = "발신자 Slack ID (USER 타입만)", example = "C09QY22AMEE")
        String senderSlackId,

        @Schema(description = "발신자 이름 (USER 타입만)", example = "김발신")
        String senderName,

        @Schema(description = "수신자 Slack ID", example = "C09QY22AMEE")
        String recipientSlackId,

        @Schema(description = "수신자 이름", example = "이수신")
        String recipientName,

        @Schema(description = "메시지 내용")
        String messageContent,

        @Schema(description = "메시지 타입 (ORDER_NOTIFICATION: 주문 알림, MANUAL: 수동 메시지, DAILY_ROUTE: 일일 경로)", example = "ORDER_NOTIFICATION")
        MessageType messageType,

        @Schema(description = "참조 ID (주문 ID 등)", example = "650e8400-e29b-41d4-a716-446655440000")
        UUID referenceId,

        @Schema(description = "발송 상태 (PENDING: 대기, SENT: 발송 완료, FAILED: 실패)", example = "SENT")
        MessageStatus status,

        @Schema(description = "발송 시각 (ISO 8601 형식)", example = "2025-11-07T10:30:00")
        String sentAt,

        @Schema(description = "에러 메시지 (발송 실패 시)")
        String errorMessage,

        @Schema(description = "생성자", example = "system")
        String createdBy,

        @Schema(description = "생성일시 (ISO 8601 형식)", example = "2025-11-07T10:25:00")
        String createdAt,

        @Schema(description = "수정자", example = "system")
        String updatedBy,

        @Schema(description = "수정일시 (ISO 8601 형식)", example = "2025-11-07T10:30:00")
        String updatedAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getSenderType(),
                notification.getSenderUsername(),
                notification.getSenderSlackId(),
                notification.getSenderName(),
                notification.getRecipientSlackId(),
                notification.getRecipientName(),
                notification.getMessageContent(),
                notification.getMessageType(),
                notification.getReferenceId(),
                notification.getStatus(),
                notification.getSentAt() != null ? notification.getSentAt().toString() : null,
                notification.getErrorMessage(),
                notification.getCreatedBy(),
                notification.getCreatedAt().toString(),
                notification.getUpdatedBy(),
                notification.getUpdatedAt().toString()
        );
    }
}
