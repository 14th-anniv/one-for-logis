package com.oneforlogis.delivery.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.oneforlogis.delivery.application.event.OrderCreatedMessage;
import com.oneforlogis.delivery.domain.repository.DeliveryRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(DeliveryService.class)
class DeliveryServiceIdempotencyTest {

    @Autowired
    DeliveryService deliveryService;
    @Autowired
    DeliveryRepository deliveryRepository;

    @Test
    void same_orderId_twice_should_create_only_one_delivery() {
        UUID orderId = UUID.randomUUID();
        var msg = new OrderCreatedMessage(
                "evt-" + orderId,
                OffsetDateTime.now(),
                new OrderCreatedMessage.Order(
                        orderId,
                        new OrderCreatedMessage.Receiver("홍길동", "서울 중구 세종대로 1", "U123"),
                        new OrderCreatedMessage.Route("hub-SEO-A", "hub-INC-B")
                )
        );

        var d1 = deliveryService.createIfAbsentFromOrder(msg);
        var d2 = deliveryService.createIfAbsentFromOrder(msg);

        assertThat(d1).isEqualTo(d2);
        var all = deliveryRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getOrderId()).isEqualTo(orderId);
    }
}