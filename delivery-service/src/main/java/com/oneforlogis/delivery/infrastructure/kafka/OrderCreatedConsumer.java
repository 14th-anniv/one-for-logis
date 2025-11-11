package com.oneforlogis.delivery.infrastructure.kafka;

import com.oneforlogis.delivery.application.event.OrderCreatedMessage;
import com.oneforlogis.delivery.application.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final DeliveryService deliveryService;

    @KafkaListener(
            topics = "#{@topicProperties.orderCreated}",
            groupId = "delivery-service"
    )
    public void onMessage(OrderCreatedMessage message) {
        log.info("📦 Received order.created event for orderId={}", message.order().orderId());
        var deliveryId = deliveryService.createIfAbsentFromOrder(message);
        log.info("🚚 Delivery created/exists for orderId={}, deliveryId={}",
                message.order().orderId(), deliveryId);
    }
}