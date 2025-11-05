package com.oneforlogis.notification.infrastructure.persistence;

import com.oneforlogis.notification.domain.model.*;
import com.oneforlogis.notification.domain.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

// NotificationRepository 통합 테스트
// @DataJpaTest: JPA 관련 컴포넌트만 로드 (in-memory H2 사용)
@DataJpaTest
@Import({NotificationRepositoryImpl.class, com.oneforlogis.notification.config.TestJpaConfig.class})
@ActiveProfiles("test")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    @DisplayName("USER 타입 알림 저장 및 조회 테스트")
    void saveAndFindUserNotification() {
        // Given
        Notification notification = Notification.builder()
                .senderType(SenderType.USER)
                .senderUsername("testuser")
                .senderSlackId("U12345")
                .senderName("테스트 사용자")
                .recipientSlackId("U67890")
                .recipientName("수신자")
                .messageContent("테스트 메시지입니다.")
                .messageType(MessageType.MANUAL)
                .referenceId(UUID.randomUUID())
                .build();

        // When
        Notification saved = notificationRepository.save(notification);
        Optional<Notification> found = notificationRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getSenderType()).isEqualTo(SenderType.USER);
        assertThat(found.get().getSenderUsername()).isEqualTo("testuser");
        assertThat(found.get().getStatus()).isEqualTo(MessageStatus.PENDING);
    }

    @Test
    @DisplayName("SYSTEM 타입 알림 저장 테스트")
    void saveSystemNotification() {
        // Given
        Notification notification = Notification.builder()
                .senderType(SenderType.SYSTEM)
                .senderUsername(null) // SYSTEM은 sender 정보 null
                .senderSlackId(null)
                .senderName(null)
                .recipientSlackId("U67890")
                .recipientName("수신자")
                .messageContent("시스템 자동 알림입니다.")
                .messageType(MessageType.ORDER_NOTIFICATION)
                .referenceId(UUID.randomUUID())
                .build();

        // When
        Notification saved = notificationRepository.save(notification);
        Optional<Notification> found = notificationRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getSenderType()).isEqualTo(SenderType.SYSTEM);
        assertThat(found.get().getSenderUsername()).isNull();
    }

    @Test
    @DisplayName("상태별 알림 조회 테스트")
    void findByStatus() {
        // Given
        Notification notification1 = createNotification(MessageStatus.PENDING);
        Notification notification2 = createNotification(MessageStatus.PENDING);
        notificationRepository.save(notification1);
        notificationRepository.save(notification2);

        // When
        List<Notification> pendingList = notificationRepository.findByStatus(MessageStatus.PENDING);

        // Then
        assertThat(pendingList).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("메시지 타입별 알림 조회 테스트")
    void findByMessageType() {
        // Given
        Notification notification = Notification.builder()
                .senderType(SenderType.SYSTEM)
                .recipientSlackId("U67890")
                .recipientName("수신자")
                .messageContent("주문 알림")
                .messageType(MessageType.ORDER_NOTIFICATION)
                .build();
        notificationRepository.save(notification);

        // When
        List<Notification> orderNotifications = notificationRepository.findByMessageType(MessageType.ORDER_NOTIFICATION);

        // Then
        assertThat(orderNotifications).isNotEmpty();
        assertThat(orderNotifications.get(0).getMessageType()).isEqualTo(MessageType.ORDER_NOTIFICATION);
    }

    @Test
    @DisplayName("수신자 Slack ID로 알림 조회 테스트")
    void findByRecipientSlackId() {
        // Given
        String recipientSlackId = "U99999";
        Notification notification = Notification.builder()
                .senderType(SenderType.SYSTEM)
                .recipientSlackId(recipientSlackId)
                .recipientName("특정 수신자")
                .messageContent("테스트")
                .messageType(MessageType.MANUAL)
                .build();
        notificationRepository.save(notification);

        // When
        List<Notification> result = notificationRepository.findByRecipientSlackId(recipientSlackId);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getRecipientSlackId()).isEqualTo(recipientSlackId);
    }

    @Test
    @DisplayName("참조 ID로 알림 조회 테스트")
    void findByReferenceId() {
        // Given
        UUID referenceId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .senderType(SenderType.SYSTEM)
                .recipientSlackId("U12345")
                .recipientName("수신자")
                .messageContent("주문 관련 알림")
                .messageType(MessageType.ORDER_NOTIFICATION)
                .referenceId(referenceId)
                .build();
        notificationRepository.save(notification);

        // When
        List<Notification> result = notificationRepository.findByReferenceId(referenceId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getReferenceId()).isEqualTo(referenceId);
    }

    @Test
    @DisplayName("발신자 사용자명으로 알림 조회 테스트")
    void findBySenderUsername() {
        // Given
        String username = "sender123";
        Notification notification = Notification.builder()
                .senderType(SenderType.USER)
                .senderUsername(username)
                .senderSlackId("U11111")
                .senderName("발신자")
                .recipientSlackId("U22222")
                .recipientName("수신자")
                .messageContent("테스트 메시지")
                .messageType(MessageType.MANUAL)
                .build();
        notificationRepository.save(notification);

        // When
        List<Notification> result = notificationRepository.findBySenderUsername(username);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getSenderUsername()).isEqualTo(username);
    }

    @Test
    @DisplayName("알림 발송 성공 처리 테스트")
    void markNotificationAsSent() {
        // Given
        Notification notification = createNotification(MessageStatus.PENDING);
        Notification saved = notificationRepository.save(notification);

        // When
        saved.markAsSent();
        notificationRepository.save(saved);

        // Then
        Optional<Notification> found = notificationRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(MessageStatus.SENT);
        assertThat(found.get().getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("알림 발송 실패 처리 테스트")
    void markNotificationAsFailed() {
        // Given
        Notification notification = createNotification(MessageStatus.PENDING);
        Notification saved = notificationRepository.save(notification);

        // When
        String errorMessage = "Slack API 호출 실패";
        saved.markAsFailed(errorMessage);
        notificationRepository.save(saved);

        // Then
        Optional<Notification> found = notificationRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(MessageStatus.FAILED);
        assertThat(found.get().getErrorMessage()).isEqualTo(errorMessage);
    }

    @Test
    @DisplayName("Soft Delete 후 BaseEntity의 deletedAt 필드 확인")
    void softDeleteSetsDeletedAtField() {
        // Given
        Notification notification = createNotification(MessageStatus.PENDING);
        Notification saved = notificationRepository.save(notification);
        UUID id = saved.getId();

        // When
        notificationRepository.deleteById(id);
        entityManager.flush();
        entityManager.clear(); // 영속성 컨텍스트 초기화

        // Then - 네이티브 쿼리로 직접 확인 (H2에서도 동작)
        Object result = entityManager.createNativeQuery(
                "SELECT deleted_at FROM p_notifications WHERE message_id = ?")
                .setParameter(1, id)
                .getSingleResult();

        assertThat(result).isNotNull(); // deleted_at이 설정됨
    }

    @Test
    @DisplayName("Soft Delete 후 일반 조회에서 제외 확인")
    void softDeletedNotificationIsNotFoundByNormalQuery() {
        // Given
        Notification notification1 = createNotification(MessageStatus.PENDING);
        Notification notification2 = createNotification(MessageStatus.PENDING);
        notificationRepository.save(notification1);
        Notification saved2 = notificationRepository.save(notification2);

        int beforeCount = notificationRepository.findAll().size();

        // When - notification2만 삭제
        notificationRepository.deleteById(saved2.getId());
        entityManager.flush();
        entityManager.clear();

        // Then - findAll 결과에서 제외됨
        List<Notification> afterList = notificationRepository.findAll();
        assertThat(afterList.size()).isEqualTo(beforeCount - 1);
        assertThat(afterList).noneMatch(n -> n.getId().equals(saved2.getId()));
    }

    @Test
    @DisplayName("USER 타입에서 sender 정보 없으면 저장 실패")
    void userTypeWithoutSenderInfoValidation() {
        // Given - USER 타입인데 senderUsername만 null
        Notification notification = Notification.builder()
                .senderType(SenderType.USER)
                .senderUsername(null)
                .senderSlackId("U12345")
                .senderName("테스트")
                .recipientSlackId("U67890")
                .recipientName("수신자")
                .messageContent("테스트")
                .messageType(MessageType.MANUAL)
                .build();

        // When & Then - 저장 시도 시 예외 발생
        assertThatThrownBy(() -> {
            notificationRepository.save(notification);
            entityManager.flush();
        }).isInstanceOf(Exception.class); // IllegalStateException 또는 PersistenceException
    }

    @Test
    @DisplayName("USER 타입에서 senderSlackId 없으면 저장 실패")
    void userTypeWithoutSenderSlackIdValidation() {
        // Given - USER 타입인데 senderSlackId가 null
        Notification notification = Notification.builder()
                .senderType(SenderType.USER)
                .senderUsername("testuser")
                .senderSlackId(null)
                .senderName("테스트")
                .recipientSlackId("U67890")
                .recipientName("수신자")
                .messageContent("테스트")
                .messageType(MessageType.MANUAL)
                .build();

        // When & Then
        assertThatThrownBy(() -> {
            notificationRepository.save(notification);
            entityManager.flush();
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("USER 타입에서 senderName 없으면 저장 실패")
    void userTypeWithoutSenderNameValidation() {
        // Given - USER 타입인데 senderName이 null
        Notification notification = Notification.builder()
                .senderType(SenderType.USER)
                .senderUsername("testuser")
                .senderSlackId("U12345")
                .senderName(null)
                .recipientSlackId("U67890")
                .recipientName("수신자")
                .messageContent("테스트")
                .messageType(MessageType.MANUAL)
                .build();

        // When & Then
        assertThatThrownBy(() -> {
            notificationRepository.save(notification);
            entityManager.flush();
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("SYSTEM 타입에서 sender 정보 있으면 저장 실패")
    void systemTypeWithSenderInfoValidation() {
        // Given - SYSTEM 타입인데 senderUsername이 있음
        Notification notification = Notification.builder()
                .senderType(SenderType.SYSTEM)
                .senderUsername("testuser")
                .senderSlackId(null)
                .senderName(null)
                .recipientSlackId("U67890")
                .recipientName("수신자")
                .messageContent("테스트")
                .messageType(MessageType.ORDER_NOTIFICATION)
                .build();

        // When & Then
        assertThatThrownBy(() -> {
            notificationRepository.save(notification);
            entityManager.flush();
        }).isInstanceOf(Exception.class);
    }

    // Helper method
    private Notification createNotification(MessageStatus status) {
        Notification notification = Notification.builder()
                .senderType(SenderType.SYSTEM)
                .recipientSlackId("U12345")
                .recipientName("테스트 수신자")
                .messageContent("테스트 메시지")
                .messageType(MessageType.MANUAL)
                .build();

        if (status == MessageStatus.SENT) {
            notification.markAsSent();
        } else if (status == MessageStatus.FAILED) {
            notification.markAsFailed("테스트 에러");
        }

        return notification;
    }
}