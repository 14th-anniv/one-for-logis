package com.oneforlogis.notification.infrastructure.persistence;

import com.oneforlogis.notification.domain.model.MessageStatus;
import com.oneforlogis.notification.domain.model.MessageType;
import com.oneforlogis.notification.domain.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Notification JPA Repository
 * Spring Data JPA가 자동으로 구현체 생성
 */
public interface NotificationJpaRepository extends JpaRepository<Notification, UUID> {

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
     * 참조 ID로 알림 조회
     */
    List<Notification> findByReferenceId(UUID referenceId);

    /**
     * 발신자 사용자명으로 알림 조회
     */
    List<Notification> findBySenderUsername(String senderUsername);

    /**
     * Kafka 이벤트 ID로 알림 존재 여부 확인 (멱등성 체크)
     */
    boolean existsByEventId(String eventId);

    /**
     * Kafka 이벤트 ID로 알림 조회
     */
    Optional<Notification> findByEventId(String eventId);
}
