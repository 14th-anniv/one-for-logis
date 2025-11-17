Kafka 적용 검토 - notification-service 중심

현재 아키텍처 (REST API 기반)

order-service → [REST API] → notification-service
↓
Slack API

문제점:
1. 동기 호출: order-service가 알림 전송 완료까지 대기
2. 강한 결합: order-service가 notification-service 장애에 직접 영향
3. 재시도 어려움: 알림 전송 실패 시 재처리 복잡

  ---
Kafka 적용 후 (메시징 기반)

order-service → [Kafka Topic] → notification-service
↓
Slack API

장점:
1. 비동기 처리: order-service는 메시지만 발행하고 즉시 응답
2. 느슨한 결합: notification-service 장애 시에도 order-service 정상 동작
3. 재시도 자동화: Kafka가 메시지 보관, 재처리 가능
4. 확장성: Consumer 추가로 처리량 증가 가능

  ---
추천 적용 위치 (우선순위)

1. ⭐⭐⭐ 주문 생성 → 알림 발송 (필수 기능, 강력 추천)

현재 (REST API):
// order-service
@Transactional
public void createOrder(OrderRequest request) {
// 1. 주문 생성
Order order = orderRepository.save(order);

      // 2. 배송 생성
      Delivery delivery = deliveryClient.createDelivery(order);

      // 3. 알림 전송 (동기 호출, 블로킹)
      notificationClient.sendOrderNotification(order, delivery);  // ❌ 동기, 실패 시 롤백?
}

Kafka 적용 후:
// order-service (Producer)
@Transactional
public void createOrder(OrderRequest request) {
// 1. 주문 생성
Order order = orderRepository.save(order);

      // 2. 배송 생성
      Delivery delivery = deliveryClient.createDelivery(order);

      // 3. 이벤트 발행 (비동기, 논블로킹)
      kafkaTemplate.send("order-created", OrderCreatedEvent.of(order, delivery));  // ✅ 
비동기
}

// notification-service (Consumer)
@KafkaListener(topics = "order-created")
public void handleOrderCreated(OrderCreatedEvent event) {
// AI로 최종 발송 시한 계산
String deadline = chatGptService.calculateDeadline(event);

      // Slack 알림 전송
      slackService.sendToHubManager(event, deadline);

      // 로그 저장
      externalApiLogRepository.save(log);
}

장점:
- 주문 생성 트랜잭션과 알림 전송 분리
- 알림 실패해도 주문은 정상 처리
- Kafka가 메시지 보관 → 재처리 가능

Topic 설계:
order-created
├── Partition 0: 주문 ID hash 기반 분산
├── Partition 1
└── Partition 2

  ---
2. ⭐⭐ 배송 상태 변경 → 알림 발송 (선택)

시나리오: 배송 담당자가 배송 상태를 업데이트할 때마다 알림

Kafka 적용:
// delivery-service (Producer)
public void updateDeliveryStatus(UUID deliveryId, DeliveryStatus status) {
delivery.updateStatus(status);
deliveryRepository.save(delivery);

      kafkaTemplate.send("delivery-status-changed", DeliveryStatusChangedEvent.of(delivery));
}

// notification-service (Consumer)
@KafkaListener(topics = "delivery-status-changed")
public void handleDeliveryStatusChanged(DeliveryStatusChangedEvent event) {
if (event.isImportantStatus()) {  // 목적지 허브 도착, 배송 완료 등
slackService.sendToRecipient(event);
}
}

Topic 설계:
delivery-status-changed

  ---
3. ⭐ 슬랙 메시지 발송 전용 Queue (선택)

현재 문제: 여러 서비스에서 Slack API를 직접 호출 → API 호출 분산, 비용 추적 어려움

Kafka 적용:
// 모든 서비스 (Producer)
public void sendSlackMessage(String recipient, String message) {
kafkaTemplate.send("slack-messages", SlackMessageEvent.of(recipient, message));
}

// notification-service (Consumer)
@KafkaListener(topics = "slack-messages")
public void handleSlackMessage(SlackMessageEvent event) {
// 모든 Slack 메시지를 중앙 집중 처리
slackService.send(event);

      // 로그 저장 (p_external_api_logs)
      externalApiLogRepository.save(log);
}

장점:
- Slack API 호출 중앙화 → 비용 추적 용이
- Rate Limit 관리 용이
- 재시도 로직 일원화

Topic 설계:
slack-messages
├── Partition 0: 우선순위 높음
├── Partition 1: 일반
└── Partition 2: 배치

  ---
4. ⭐⭐ 도전 과제: 매일 6시 배송 경로 최적화 알림 (도전 기능)

Scheduler → Kafka → notification-service:

// notification-service (Producer + Consumer)
@Scheduled(cron = "${daily-route.schedule}")  // 매일 6시
public void scheduleRouteOptimization() {
List<Delivery> todayDeliveries = deliveryRepository.findTodayDeliveries();

      for (Delivery delivery : todayDeliveries) {
          kafkaTemplate.send("route-optimization-request",
RouteOptimizationEvent.of(delivery));
}
}

@KafkaListener(topics = "route-optimization-request", concurrency = "3")
public void handleRouteOptimization(RouteOptimizationEvent event) {
// AI로 배송 순서 최적화 (TSP)
List<String> optimizedRoute = chatGptService.optimizeRoute(event);

      // Naver Maps API로 경로 계산
      NaverMapsResponse response = naverMapsService.getDirections(optimizedRoute);

      // Slack 알림 전송
      slackService.sendDailyRoute(event, optimizedRoute, response);
}

장점:
- 동시에 여러 배송 담당자의 경로를 병렬 처리 (concurrency = 3)
- 외부 API 호출 실패 시 재시도 자동화

Topic 설계:
route-optimization-request
├── Partition 0: Hub A 배송 담당자
├── Partition 1: Hub B 배송 담당자
└── Partition 2: Hub C 배송 담당자

  ---
Kafka vs REST API 비교 (알림 서비스 관점)

| 항목     | REST API         | Kafka                    |
  |--------|------------------|--------------------------|
| 호출 방식  | 동기 (Blocking)    | 비동기 (Non-blocking)       |
| 장애 영향  | order-service 실패 | order-service 정상, 메시지 보관 |
| 재시도    | 복잡 (수동 구현)       | 자동 (Kafka 보관)            |
| 확장성    | 서버 증설 어려움        | Consumer 추가로 쉽게 확장       |
| 트랜잭션   | 분산 트랜잭션 복잡       | 이벤트 기반으로 분리              |
| 모니터링   | API 로그 분산        | Topic 메트릭 중앙 집중          |
| 구현 복잡도 | 낮음               | 중간 (Kafka 설정 필요)         |

  ---
추천 적용 전략

Phase 1: 핵심 기능 Kafka 적용

1. 주문 생성 → 알림 발송 (order-created topic)
2. 기존 REST API는 남겨두고 Kafka를 추가 (Fallback)

Phase 2: 선택 기능 확장

3. Slack 메시지 중앙화 (slack-messages topic)
4. 배송 상태 변경 알림 (delivery-status-changed topic)

Phase 3: 도전 과제

5. 매일 6시 경로 최적화 (route-optimization-request topic)

  ---
기술 스택 추가

현재:
# docker-compose-v12.yml
services:
postgres: ...
redis: ...
eureka-server: ...

Kafka 추가:
services:
zookeeper:
image: confluentinc/cp-zookeeper:latest
environment:
ZOOKEEPER_CLIENT_PORT: 2181

    kafka:
      image: confluentinc/cp-kafka:latest
      depends_on:
        - zookeeper
      environment:
        KAFKA_BROKER_ID: 1
        KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
        KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      ports:
        - "9092:9092"

Gradle 의존성:
// notification-service/build.gradle
implementation 'org.springframework.kafka:spring-kafka'

  ---
결론 및 제안

✅ Kafka 적용 강력 추천

1. 주문 생성 → 알림 발송 (필수)
2. Slack 메시지 중앙화 (선택, 비용 추적 용이)
3. 매일 6시 경로 최적화 (도전 과제)

⚠️ 주의사항

- REST API 완전 제거 X, Fallback으로 유지
- Kafka Consumer 실패 처리 (Dead Letter Queue)
- 메시지 순서 보장 필요 시 Partition Key 설정

📝 다음 단계

1. Kafka 환경 구성 (docker-compose)
2. order-created topic Producer/Consumer 구현
3. 기존 REST API와 병행 운영 후 검증
4. Kafka 모니터링 (Kafka UI, Prometheus)
