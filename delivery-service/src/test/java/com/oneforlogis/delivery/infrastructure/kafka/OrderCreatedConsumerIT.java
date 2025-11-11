package com.oneforlogis.delivery.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.oneforlogis.delivery.application.event.OrderCreatedMessage;
import com.oneforlogis.delivery.domain.repository.DeliveryRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"order.created"})
class OrderCreatedConsumerIT {

    @Autowired
    DeliveryRepository deliveryRepository;

    @Value("${spring.embedded.kafka.brokers}")
    private String brokers;

    private KafkaTemplate<String, Object> kafkaTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Test
    void consume_order_created_should_persist_delivery() {
        var template = kafkaTemplate();

        UUID orderId = UUID.randomUUID();
        var payload = new OrderCreatedMessage(
                "evt-" + orderId,
                OffsetDateTime.now(),
                new OrderCreatedMessage.Order(
                        orderId,
                        new OrderCreatedMessage.Receiver("임꺽정", "인천 미추홀구 …", "U999"),
                        new OrderCreatedMessage.Route("hub-SEO-A", "hub-INC-B")
                )
        );

        // when
        template.send("order.created", orderId.toString(), payload);

        // then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(deliveryRepository.findByOrderId(orderId)).isPresent()
        );
    }
}