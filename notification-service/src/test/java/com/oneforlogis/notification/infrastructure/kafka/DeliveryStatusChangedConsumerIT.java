package com.oneforlogis.notification.infrastructure.kafka;

import com.oneforlogis.notification.application.event.DeliveryStatusChangedEvent;
import com.oneforlogis.notification.domain.model.MessageType;
import com.oneforlogis.notification.domain.repository.NotificationRepository;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {"delivery.status.changed"}
)
@DisplayName("DeliveryStatusChangedConsumer 통합 테스트")
class DeliveryStatusChangedConsumerIT {

    @Autowired
    private NotificationRepository notificationRepository;

    @Value("${spring.embedded.kafka.brokers}")
    private String brokers;

    // 외부 API는 Mock으로 대체
    @MockBean
    private com.oneforlogis.notification.infrastructure.client.SlackClientWrapper slackClientWrapper;

    @BeforeEach
    void setUp() {
        // 테스트 간 격리: DB 초기화
        notificationRepository.findAll().forEach(notificationRepository::delete);

        // Mock 설정: Slack API 성공 응답
        com.oneforlogis.notification.infrastructure.client.slack.SlackMessageResponse slackResponse =
            com.oneforlogis.notification.infrastructure.client.slack.SlackMessageResponse.builder()
                .ok(true)
                .channel("U123456")
                .ts("1234567890.123456")
                .build();
        org.mockito.Mockito.when(slackClientWrapper.postMessage(
            org.mockito.Mockito.any(), org.mockito.Mockito.any()
        )).thenReturn(slackResponse);
    }

    // delivery-service 패턴: 인라인 KafkaTemplate 생성
    private KafkaTemplate<String, Object> kafkaTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Test
    @DisplayName("delivery.status.changed 이벤트 수신 시 알림 생성")
    void shouldCreateNotificationWhenDeliveryStatusChangedEventReceived() {
        // Given: 배송 상태 변경 이벤트
        KafkaTemplate<String, Object> template = kafkaTemplate();

        UUID deliveryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String eventId = "delivery-event-" + UUID.randomUUID();

        DeliveryStatusChangedEvent event = new DeliveryStatusChangedEvent(
                eventId,
                OffsetDateTime.now(),
                new DeliveryStatusChangedEvent.DeliveryData(
                        deliveryId,
                        orderId,
                        "HUB_WAITING",
                        "HUB_MOVING",
                        "U01234567",
                        "Test Hub Manager"
                )
        );

        // When: Kafka 이벤트 발행
        template.send("delivery.status.changed", eventId, event);

        // Then: 알림이 생성되었는지 확인 (비동기 처리 대기)
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    assertThat(notificationRepository.existsByEventId(eventId)).isTrue();

                    // MessageType이 DELIVERY_STATUS_UPDATE인지 확인
                    notificationRepository.findAll().stream()
                            .filter(n -> eventId.equals(n.getEventId()))
                            .findFirst()
                            .ifPresent(notification -> {
                                assertThat(notification.getMessageType()).isEqualTo(MessageType.DELIVERY_STATUS_UPDATE);
                                assertThat(notification.getMessageContent()).contains("배송 상태 업데이트");
                                assertThat(notification.getMessageContent()).contains("HUB_WAITING");
                                assertThat(notification.getMessageContent()).contains("HUB_MOVING");
                            });
                });
    }

    @Test
    @DisplayName("동일한 eventId로 중복 이벤트 수신 시 멱등성 보장")
    void shouldEnsureIdempotencyWhenDuplicateEventReceived() {
        // Given: 동일한 eventId를 가진 이벤트
        KafkaTemplate<String, Object> template = kafkaTemplate();

        UUID deliveryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String eventId = "duplicate-delivery-event-" + UUID.randomUUID();

        DeliveryStatusChangedEvent event = new DeliveryStatusChangedEvent(
                eventId,
                OffsetDateTime.now(),
                new DeliveryStatusChangedEvent.DeliveryData(
                        deliveryId,
                        orderId,
                        "HUB_MOVING",
                        "HUB_ARRIVED",
                        "U98765432",
                        "Another Manager"
                )
        );

        // When: 동일한 이벤트를 2번 발행
        template.send("delivery.status.changed", eventId, event);
        template.send("delivery.status.changed", eventId, event);

        // Then: 1개의 알림만 생성되었는지 확인
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    long count = notificationRepository.findAll().stream()
                            .filter(n -> eventId.equals(n.getEventId()))
                            .count();
                    assertThat(count).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("다양한 배송 상태 변경 시나리오 테스트")
    void shouldHandleVariousStatusChanges() {
        // Given: 여러 배송 상태 변경 이벤트
        KafkaTemplate<String, Object> template = kafkaTemplate();

        UUID deliveryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        // Scenario 1: HUB_WAITING → HUB_MOVING
        String eventId1 = "status-event-1-" + UUID.randomUUID();
        DeliveryStatusChangedEvent event1 = new DeliveryStatusChangedEvent(
                eventId1,
                OffsetDateTime.now(),
                new DeliveryStatusChangedEvent.DeliveryData(
                        deliveryId, orderId,
                        "HUB_WAITING", "HUB_MOVING",
                        "C09QY22AMEE", "Hub Manager 1"
                )
        );

        // Scenario 2: HUB_MOVING → HUB_ARRIVED
        String eventId2 = "status-event-2-" + UUID.randomUUID();
        DeliveryStatusChangedEvent event2 = new DeliveryStatusChangedEvent(
                eventId2,
                OffsetDateTime.now(),
                new DeliveryStatusChangedEvent.DeliveryData(
                        deliveryId, orderId,
                        "HUB_MOVING", "HUB_ARRIVED",
                        "C09QY22AMEE", "Hub Manager 2"
                )
        );

        // Scenario 3: HUB_ARRIVED → DELIVERING
        String eventId3 = "status-event-3-" + UUID.randomUUID();
        DeliveryStatusChangedEvent event3 = new DeliveryStatusChangedEvent(
                eventId3,
                OffsetDateTime.now(),
                new DeliveryStatusChangedEvent.DeliveryData(
                        deliveryId, orderId,
                        "HUB_ARRIVED", "DELIVERING",
                        "U11111111", "Delivery Person"
                )
        );

        // When: 여러 이벤트 발행
        template.send("delivery.status.changed", eventId1, event1);
        template.send("delivery.status.changed", eventId2, event2);
        template.send("delivery.status.changed", eventId3, event3);

        // Then: 3개의 알림이 모두 생성되었는지 확인
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    assertThat(notificationRepository.existsByEventId(eventId1)).isTrue();
                    assertThat(notificationRepository.existsByEventId(eventId2)).isTrue();
                    assertThat(notificationRepository.existsByEventId(eventId3)).isTrue();

                    long count = notificationRepository.findAll().stream()
                            .filter(n -> n.getMessageType() == MessageType.DELIVERY_STATUS_UPDATE)
                            .count();
                    assertThat(count).isGreaterThanOrEqualTo(3);
                });
    }
}