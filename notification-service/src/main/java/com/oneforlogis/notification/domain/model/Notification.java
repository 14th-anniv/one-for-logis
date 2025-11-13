package com.oneforlogis.notification.domain.model;

import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

// 알림 메시지 엔티티
// 발신자/수신자 정보를 스냅샷으로 저장하여 감사 추적 보장
@Entity
@Table(name = "p_notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class Notification extends BaseEntity {

    @Id
    @Column(name = "message_id")
    private UUID id;

    // 발신자 타입 (USER: 사용자 발송, SYSTEM: 시스템 자동 발송)
    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 20)
    private SenderType senderType;

    // 발신자 사용자명 (USER 타입일 때만 필수)
    @Column(name = "sender_username", length = 100)
    private String senderUsername;

    // 발신자 Slack ID (USER 타입일 때만 필수)
    @Column(name = "sender_slack_id", length = 100)
    private String senderSlackId;

    // 발신자 이름 (USER 타입일 때만 필수)
    @Column(name = "sender_name", length = 100)
    private String senderName;

    // 수신자 Slack ID (필수)
    @Column(name = "recipient_slack_id", nullable = false, length = 100)
    private String recipientSlackId;

    // 수신자 이름 (필수)
    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    // 메시지 내용
    @Column(name = "message_content", nullable = false, columnDefinition = "TEXT")
    private String messageContent;

    // 메시지 타입
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 30)
    private MessageType messageType;

    // 참조 ID (주문 ID, 배송 ID 등)
    @Column(name = "reference_id")
    private UUID referenceId;

    // Kafka 이벤트 ID (멱등성 보장용, Kafka 이벤트 기반 알림만 사용)
    @Column(name = "event_id", unique = true, length = 100)
    private String eventId;

    // 발송 시각
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    // 발송 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MessageStatus status;

    // 에러 메시지 (발송 실패 시)
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Builder
    public Notification(
            SenderType senderType,
            String senderUsername,
            String senderSlackId,
            String senderName,
            String recipientSlackId,
            String recipientName,
            String messageContent,
            MessageType messageType,
            UUID referenceId,
            String eventId
    ) {
        this.id = UUID.randomUUID();
        this.senderType = senderType;
        this.senderUsername = senderUsername;
        this.senderSlackId = senderSlackId;
        this.senderName = senderName;
        this.recipientSlackId = recipientSlackId;
        this.recipientName = recipientName;
        this.messageContent = messageContent;
        this.messageType = messageType;
        this.referenceId = referenceId;
        this.eventId = eventId;
        this.status = MessageStatus.PENDING;
    }

    /**
     * 메시지 발송 성공 처리
     */
    public void markAsSent() {
        this.status = MessageStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    /**
     * 메시지 발송 실패 처리
     */
    public void markAsFailed(String errorMessage) {
        this.status = MessageStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    /**
     * 메시지 내용 업데이트 (Priority 2-1: Gemini 응답 후 메시지 업데이트)
     */
    public void updateMessageContent(String newContent) {
        if (newContent == null || newContent.isBlank()) {
            throw new CustomException(ErrorCode.NOTIFICATION_INVALID_CONTENT);
        }
        this.messageContent = newContent;
    }

    /**
     * 엔티티 저장 전 검증
     */
    @PrePersist
    @PreUpdate
    private void validateEntity() {
        // USER 타입일 경우 sender 정보 필수 (Priority 2-4: CustomException 사용)
        if (senderType == SenderType.USER) {
            if (senderUsername == null || senderUsername.isBlank()) {
                throw new CustomException(ErrorCode.NOTIFICATION_INVALID_SENDER);
            }
            if (senderSlackId == null || senderSlackId.isBlank()) {
                throw new CustomException(ErrorCode.NOTIFICATION_INVALID_SENDER);
            }
            if (senderName == null || senderName.isBlank()) {
                throw new CustomException(ErrorCode.NOTIFICATION_INVALID_SENDER);
            }
        }

        // SYSTEM 타입일 경우 sender 정보는 null이어야 함
        if (senderType == SenderType.SYSTEM) {
            if (senderUsername != null || senderSlackId != null || senderName != null) {
                throw new CustomException(ErrorCode.NOTIFICATION_INVALID_SENDER);
            }
        }

        // 수신자 정보 필수
        if (recipientSlackId == null || recipientSlackId.isBlank()) {
            throw new CustomException(ErrorCode.NOTIFICATION_INVALID_RECIPIENT);
        }
        if (recipientName == null || recipientName.isBlank()) {
            throw new CustomException(ErrorCode.NOTIFICATION_INVALID_RECIPIENT);
        }

        // 메시지 내용 필수
        if (messageContent == null || messageContent.isBlank()) {
            throw new CustomException(ErrorCode.NOTIFICATION_INVALID_CONTENT);
        }
    }
}
