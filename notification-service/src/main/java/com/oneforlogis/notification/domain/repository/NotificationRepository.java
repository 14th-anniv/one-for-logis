package com.oneforlogis.notification.domain.repository;

import com.oneforlogis.notification.domain.model.MessageStatus;
import com.oneforlogis.notification.domain.model.MessageType;
import com.oneforlogis.notification.domain.model.Notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Notification 도메인 Repository 인터페이스
 * 인프라스트럭처 레이어에서 구현
 */
public interface NotificationRepository {

    /**
     * 알림 저장
     */
    Notification save(Notification notification);

    /**
     * ID로 알림 조회
     */
    Optional<Notification> findById(UUID id);

    /**
     * 모든 알림 조회 (Soft Delete 제외)
     */
    List<Notification> findAll();

    /**
     * 상태별 알림 조회
     */
    List<Notification> findByStatus(MessageStatus status);

    /**
     * 메시지 타입별 알림 조회
     */
    List<Notification> findByMessageType(MessageType messageType);

    /**
     * 수신자 Slack ID로 알림 조회
     */
    List<Notification> findByRecipientSlackId(String recipientSlackId);

    /**
     * 참조 ID로 알림 조회 (주문 ID, 배송 ID 등)
     */
    List<Notification> findByReferenceId(UUID referenceId);

    /**
     * 발신자 사용자명으로 알림 조회
     */
    List<Notification> findBySenderUsername(String senderUsername);

    /**
     * 알림 삭제 (Soft Delete)
     */
    void delete(Notification notification);

    /**
     * ID로 알림 삭제 (Soft Delete)
     */
    void deleteById(UUID id);
}
