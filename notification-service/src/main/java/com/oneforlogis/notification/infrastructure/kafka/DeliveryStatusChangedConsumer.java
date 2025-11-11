package com.oneforlogis.notification.infrastructure.kafka;

import com.oneforlogis.notification.application.event.DeliveryStatusChangedEvent;
import com.oneforlogis.notification.domain.model.MessageType;
import com.oneforlogis.notification.domain.model.Notification;
import com.oneforlogis.notification.domain.model.SenderType;
import com.oneforlogis.notification.domain.repository.NotificationRepository;
import com.oneforlogis.notification.infrastructure.client.SlackClientWrapper;
import com.oneforlogis.notification.infrastructure.client.slack.SlackMessageRequest;
import com.oneforlogis.notification.infrastructure.client.slack.SlackMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryStatusChangedConsumer {

    private final NotificationRepository notificationRepository;
    private final SlackClientWrapper slackClientWrapper;

    @KafkaListener(
            topics = "#{@topicProperties.deliveryStatusChanged}",
            groupId = "notification-service",
            containerFactory = "deliveryStatusChangedKafkaListenerContainerFactory"
    )
    public void onMessage(DeliveryStatusChangedEvent event) {
        log.info("🚚 Received delivery.status.changed event - eventId: {}, deliveryId: {}, status: {} → {}",
                event.eventId(), event.delivery().deliveryId(),
                event.delivery().previousStatus(), event.delivery().currentStatus());

        try {
            // 멱등성 체크: 이미 처리된 이벤트인지 확인
            if (notificationRepository.existsByEventId(event.eventId())) {
                log.info("⏭️ Event already processed (idempotency) - eventId: {}, deliveryId: {}",
                        event.eventId(), event.delivery().deliveryId());
                return;
            }

            var delivery = event.delivery();

            // Slack 메시지 생성
            String message = buildStatusChangeMessage(delivery);

            // Notification 엔티티 생성 및 저장 (먼저 DB에 커밋하여 멱등성 보장)
            Notification savedNotification = saveNotification(event.eventId(), delivery, message);

            // Slack 메시지 발송 (트랜잭션 외부)
            sendSlackNotification(savedNotification, delivery, message);

            log.info("✅ Delivery status notification processed - deliveryId: {}, notificationId: {}",
                    delivery.deliveryId(), savedNotification.getId());

        } catch (Exception e) {
            log.error("❌ Failed to process delivery.status.changed event - eventId: {}, deliveryId: {}, error: {}",
                    event.eventId(), event.delivery().deliveryId(), e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    private Notification saveNotification(String eventId, DeliveryStatusChangedEvent.DeliveryData delivery, String message) {
        Notification notification = Notification.builder()
                .senderType(SenderType.SYSTEM)
                .senderUsername(null)
                .senderSlackId(null)
                .senderName(null)
                .recipientSlackId(delivery.recipientSlackId())
                .recipientName(delivery.recipientName())
                .messageContent(message)
                .messageType(MessageType.DELIVERY_STATUS_UPDATE)
                .referenceId(delivery.deliveryId())
                .eventId(eventId)  // 멱등성 보장용 eventId 저장
                .build();

        return notificationRepository.save(notification);
    }

    @Transactional
    private void sendSlackNotification(Notification notification, DeliveryStatusChangedEvent.DeliveryData delivery, String message) {
        SlackMessageRequest slackRequest = SlackMessageRequest.builder()
                .channel(delivery.recipientSlackId())
                .text(message)
                .build();

        SlackMessageResponse slackResponse = slackClientWrapper.postMessage(
                slackRequest,
                notification.getId()
        );

        // 발송 상태 업데이트
        if (slackResponse != null && slackResponse.isOk()) {
            notification.markAsSent();
            log.info("✅ Slack message sent - deliveryId: {}", delivery.deliveryId());
        } else {
            String error = slackResponse != null ? slackResponse.getError() : "Unknown error";
            notification.markAsFailed(error);
            log.error("❌ Failed to send Slack message - deliveryId: {}, error: {}", delivery.deliveryId(), error);
        }
    }

    private String buildStatusChangeMessage(DeliveryStatusChangedEvent.DeliveryData delivery) {
        return String.format(
                """
                🚚 *배송 상태 업데이트*

                배송 ID: `%s`
                주문 ID: `%s`
                이전 상태: `%s`
                현재 상태: `%s`

                수령인: %s
                """,
                delivery.deliveryId(),
                delivery.orderId(),
                delivery.previousStatus(),
                delivery.currentStatus(),
                delivery.recipientName()
        );
    }
}
