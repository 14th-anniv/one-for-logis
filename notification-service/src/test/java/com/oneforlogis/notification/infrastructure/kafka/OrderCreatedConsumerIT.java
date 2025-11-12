package com.oneforlogis.notification.infrastructure.kafka;

import com.oneforlogis.notification.application.event.OrderCreatedEvent;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {"order.created"}
)
@DisplayName("OrderCreatedConsumer 통합 테스트")
class OrderCreatedConsumerIT {

    @Autowired
    private NotificationRepository notificationRepository;

    @Value("${spring.embedded.kafka.brokers}")
    private String brokers;

    // 외부 API는 Mock으로 대체
    @MockBean
    private com.oneforlogis.notification.infrastructure.client.SlackClientWrapper slackClientWrapper;

    @MockBean
    private com.oneforlogis.notification.infrastructure.client.GeminiClientWrapper geminiClientWrapper;

    @BeforeEach
    void setUp() {
        // 테스트 간 격리: DB 초기화
        notificationRepository.findAll().forEach(notificationRepository::delete);
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
    @DisplayName("order.created 이벤트 수신 시 알림 생성")
    void shouldCreateNotificationWhenOrderCreatedEventReceived() {
        // Given: 주문 생성 이벤트
        KafkaTemplate<String, Object> template = kafkaTemplate();

        UUID orderId = UUID.randomUUID();
        String eventId = "event-" + UUID.randomUUID();

        OrderCreatedEvent event = new OrderCreatedEvent(
                eventId,
                OffsetDateTime.now(),
                new OrderCreatedEvent.OrderData(
                        orderId,
                        "홍길동 / hong@test.com",
                        "공급업체A",
                        "수령업체B",
                        "상품 10개",
                        "빠른 배송 부탁드립니다",
                        new OrderCreatedEvent.RouteData(
                                UUID.randomUUID(),
                                "서울센터",
                                List.of("대전센터"),
                                UUID.randomUUID(),
                                "부산센터"
                        ),
                        new OrderCreatedEvent.ReceiverData(
                                "김수령",
                                "부산시 해운대구",
                                "U01234567"
                        ),
                        new OrderCreatedEvent.HubManagerData(
                                "U98765432",
                                "박관리"
                        )
                )
        );

        // When: Kafka 이벤트 발행
        template.send("order.created", eventId, event);

        // Then: 알림이 생성되었는지 확인 (비동기 처리 대기)
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    assertThat(notificationRepository.existsByEventId(eventId)).isTrue();
                });
    }

    @Test
    @DisplayName("동일한 eventId로 중복 이벤트 수신 시 멱등성 보장")
    void shouldEnsureIdempotencyWhenDuplicateEventReceived() {
        // Given: 동일한 eventId를 가진 이벤트
        KafkaTemplate<String, Object> template = kafkaTemplate();

        UUID orderId = UUID.randomUUID();
        String eventId = "duplicate-event-" + UUID.randomUUID();

        OrderCreatedEvent event = new OrderCreatedEvent(
                eventId,
                OffsetDateTime.now(),
                new OrderCreatedEvent.OrderData(
                        orderId,
                        "홍길동 / hong@test.com",
                        "공급업체A",
                        "수령업체B",
                        "상품 10개",
                        "빠른 배송 부탁드립니다",
                        new OrderCreatedEvent.RouteData(
                                UUID.randomUUID(),
                                "서울센터",
                                List.of("대전센터"),
                                UUID.randomUUID(),
                                "부산센터"
                        ),
                        new OrderCreatedEvent.ReceiverData(
                                "김수령",
                                "부산시 해운대구",
                                "U01234567"
                        ),
                        new OrderCreatedEvent.HubManagerData(
                                "U98765432",
                                "박관리"
                        )
                )
        );

        // When: 동일한 이벤트를 2번 발행
        template.send("order.created", eventId, event);
        template.send("order.created", eventId, event);

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
}