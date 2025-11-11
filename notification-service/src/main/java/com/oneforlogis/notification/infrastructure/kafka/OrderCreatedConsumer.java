package com.oneforlogis.notification.infrastructure.kafka;

import com.oneforlogis.notification.application.event.OrderCreatedEvent;
import com.oneforlogis.notification.application.service.NotificationService;
import com.oneforlogis.notification.domain.repository.NotificationRepository;
import com.oneforlogis.notification.presentation.request.OrderNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @KafkaListener(
            topics = "#{@topicProperties.orderCreated}",
            groupId = "notification-service",
            containerFactory = "orderCreatedKafkaListenerContainerFactory"
    )
    public void onMessage(OrderCreatedEvent event) {
        log.info("📦 Received order.created event - eventId: {}, orderId: {}",
                event.eventId(), event.order().orderId());

        try {
            // 멱등성 체크: 이미 처리된 이벤트인지 확인
            if (notificationRepository.existsByEventId(event.eventId())) {
                log.info("⏭️ Event already processed (idempotency) - eventId: {}, orderId: {}",
                        event.eventId(), event.order().orderId());
                return;
            }

            // OrderCreatedEvent → OrderNotificationRequest 변환
            OrderNotificationRequest request = convertToRequest(event);

            // 주문 알림 발송 (내부에서 eventId를 Notification에 저장해야 함)
            notificationService.sendOrderNotificationFromEvent(request, event.eventId());

            log.info("✅ Order notification sent successfully - orderId: {}", event.order().orderId());

        } catch (Exception e) {
            log.error("❌ Failed to send order notification - eventId: {}, orderId: {}, error: {}",
                    event.eventId(), event.order().orderId(), e.getMessage(), e);
            // 예외를 던져서 Kafka가 재시도하도록 함
            throw e;
        }
    }

    private OrderNotificationRequest convertToRequest(OrderCreatedEvent event) {
        var order = event.order();
        var route = order.route();
        var receiver = order.receiver();
        var hubManager = order.hubManager();

        // waypoint hub names를 그대로 전달 (List<String>)
        List<String> waypoints = route.waypointHubNames() != null
                ? route.waypointHubNames()
                : List.of();

        return new OrderNotificationRequest(
                order.orderId(),
                order.ordererInfo(),
                order.requestingCompanyName(),
                order.receivingCompanyName(),
                order.productInfo(),
                order.requestDetails(),
                route.startHubName(),
                waypoints,
                route.destinationHubName(),
                receiver.address(),
                String.format("%s / %s", receiver.name(), receiver.slackId()),
                hubManager.slackId(),
                hubManager.name()
        );
    }
}
