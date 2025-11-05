package com.oneforlogis.notification.infrastructure.persistence;

import com.oneforlogis.notification.domain.model.MessageStatus;
import com.oneforlogis.notification.domain.model.MessageType;
import com.oneforlogis.notification.domain.model.Notification;
import com.oneforlogis.notification.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// NotificationRepository 구현체
// JPA Repository를 래핑하여 도메인 레이어의 인터페이스 구현
@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationJpaRepository jpaRepository;

    @Override
    public Notification save(Notification notification) {
        return jpaRepository.save(notification);
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Notification> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<Notification> findByStatus(MessageStatus status) {
        return jpaRepository.findByStatus(status);
    }

    @Override
    public List<Notification> findByMessageType(MessageType messageType) {
        return jpaRepository.findByMessageType(messageType);
    }

    @Override
    public List<Notification> findByRecipientSlackId(String recipientSlackId) {
        return jpaRepository.findByRecipientSlackId(recipientSlackId);
    }

    @Override
    public List<Notification> findByReferenceId(UUID referenceId) {
        return jpaRepository.findByReferenceId(referenceId);
    }

    @Override
    public List<Notification> findBySenderUsername(String senderUsername) {
        return jpaRepository.findBySenderUsername(senderUsername);
    }

    @Override
    public void delete(Notification notification) {
        notification.markAsDeleted("SYSTEM"); // TODO: 실제 사용자 정보로 변경
        jpaRepository.save(notification);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.findById(id).ifPresent(notification -> {
            notification.markAsDeleted("SYSTEM"); // TODO: 실제 사용자 정보로 변경
            jpaRepository.save(notification);
        });
    }
}
