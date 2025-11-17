# notification-service 미구현 기능 및 개선 필요 사항

**작성일**: 2025-11-12
**작성자**: notification-service 담당자
**목적**: 타 서비스 미구현 부분, 보안 이슈, 보상 이벤트 필요성 종합 검토

---

## 📋 목차
1. [TODO 주석 정리](#1-todo-주석-정리)
2. [타 서비스 의존성 미구현](#2-타-서비스-의존성-미구현)
3. [보안 취약점](#3-보안-취약점)
4. [보상 이벤트/트랜잭션](#4-보상-이벤트트랜잭션)
5. [비동기 처리](#5-비동기-처리)
6. [Challenge 기능](#6-challenge-기능)
7. [권장 우선순위](#7-권장-우선순위)

---

## 1. TODO 주석 정리

### 🔴 Priority 1 (Critical)

#### 1.1 NotificationRepositoryImpl 사용자 정보 하드코딩
**파일**: `infrastructure/persistence/NotificationRepositoryImpl.java:81, 88`
```java
notification.markAsDeleted("SYSTEM"); // TODO: 실제 사용자 정보로 변경
```

**문제점**:
- 소프트 삭제 시 `deletedBy`가 "SYSTEM"으로 고정
- 실제 삭제 요청자 정보가 저장되지 않음 (감사 추적 불가)

**해결 방안**:
1. **SecurityContext 활용** (권장):
   ```java
   private String getCurrentUsername() {
       Authentication auth = SecurityContextHolder.getContext().getAuthentication();
       if (auth == null || !auth.isAuthenticated()) {
           return "SYSTEM";
       }
       return auth.getName();
   }

   notification.markAsDeleted(getCurrentUsername());
   ```

2. **메서드 파라미터 전달**:
   ```java
   public void deleteById(UUID id, String deletedBy) {
       notification.markAsDeleted(deletedBy);
   }
   ```

**관련 Issue**: 새로운 Issue #85 생성 권장

---

### 🟡 Priority 2 (Important)

#### 1.2 빈 TODO 주석 제거
**파일**: 다수
- `application/dto/NotificationDto.java:3` - "TODO: Define application layer DTOs"
- `domain/exception/NotificationException.java:3` - "TODO: Define domain exceptions"
- `global/util/AuthContextUtil.java:3` - "TODO: Define utility classes"
- `infrastructure/client/SlackClient.java:3` - "TODO: Define Feign clients"
- `presentation/request/NotificationRequest.java:3` - "TODO: Define notification request DTOs"
- `presentation/advice/NotificationExceptionHandler.java:7` - "TODO: Implement exception handlers"

**해결 방안**:
- 이미 구현된 기능이므로 TODO 주석 삭제
- 또는 구체적인 TODO로 변경 (예: "DTO 리팩토링 - presentation → application 이동")

**관련 Issue**: Cleanup Issue로 분류 가능

---

## 2. 타 서비스 의존성 미구현

### 🟡 user-service 미구현 부분

#### 2.1 getUserByUsername API 미구현 (Critical)
**현재 상황**:
- `NotificationController.sendManualNotification()` (85-86줄)에서 호출
- user-service에 해당 API 미구현 → **NPE 위험** (Issue #76-2)

**코드**:
```java
ApiResponse<UserResponse> userResponse = userServiceClient.getUserByUsername(username);
UserResponse user = userResponse.data(); // ⚠️ data()가 null이면 NPE
```

**영향도**:
- 수동 메시지 발송 API (`POST /api/v1/notifications/manual`) 동작 불가
- 사용자 정보 스냅샷 저장 실패

**해결 방안**:
1. **user-service에 API 추가** (우선):
   - `GET /api/v1/users/{username}` 엔드포인트 구현
   - 테스트: `GET /api/v1/users/admin` → UserResponse 반환

2. **notification-service NPE 방어** (Issue #76-2):
   ```java
   if (userResponse.data() == null) {
       throw new CustomException(ErrorCode.USER_NOT_FOUND);
   }
   ```

**관련 Issue**: Issue #76-2 (user-service NPE 위험)

---

#### 2.2 user-service API 스펙 불명확
**문제점**:
- UserResponse DTO 구조 불명확 (slackId 필드 존재 여부)
- user-service 인증/권한 스펙 불명확

**해결 방안**:
- user-service API 명세서 작성 요청
- 팀 스크럼에서 협의

---

### 🟢 delivery-service 의존성 (Challenge 기능)

#### 2.3 일일 배송 데이터 조회 API 미구현
**현재 상황**:
- Issue #36 (Daily route optimization) 구현 시 필요
- delivery-service에서 `GET /api/v1/deliveries/daily` API 필요

**요구 사항**:
```java
// 필요한 API
GET /api/v1/deliveries/daily?date={YYYY-MM-DD}&hubId={UUID}

// Response
{
  "deliveries": [
    {
      "deliveryId": "UUID",
      "orderId": "UUID",
      "destinationAddress": "서울시 강남구...",
      "deliveryPersonnel": { ... },
      "status": "PENDING"
    }
  ]
}
```

**해결 방안**:
- delivery-service 팀과 협의하여 API 추가
- Issue #36 구현 전 선행 작업 필요

---

## 3. 보안 취약점

### 🔴 Priority 1 (Critical)

#### 3.1 Kafka Consumer 인증 부재
**현재 상황**:
- `OrderCreatedConsumer`, `DeliveryStatusChangedConsumer`
- Kafka 메시지 수신 시 **인증/권한 검증 없음**
- SecurityContext 설정 없음 (createdBy가 항상 "system")

**보안 위험**:
1. **악의적 이벤트 발행**: 외부에서 Kafka 토픽에 직접 발행 가능
2. **데이터 무결성**: 검증되지 않은 데이터가 DB에 저장
3. **감사 추적 불가**: createdBy가 "system"으로 고정

**해결 방안**:

**Option A: Kafka 메시지 서명 검증** (권장)
```java
@KafkaListener(...)
public void onMessage(DeliveryStatusChangedEvent event) {
    // 1. 이벤트 서명 검증
    if (!verifyEventSignature(event)) {
        log.error("❌ Invalid event signature - eventId: {}", event.eventId());
        return;
    }

    // 2. 기존 로직
    // ...
}

private boolean verifyEventSignature(Event event) {
    // HMAC-SHA256 등으로 서명 검증
    String expectedSignature = calculateSignature(event);
    return expectedSignature.equals(event.signature());
}
```

**Option B: Kafka ACL + IP 화이트리스트**
- Kafka 설정에서 토픽별 ACL 적용
- notification-service만 consumer 권한 부여
- 네트워크 레벨 방어

**Option C: 이벤트 발행자 정보 포함**
```java
// Event DTO에 발행자 정보 추가
public record OrderCreatedEvent(
    String eventId,
    Instant occurredAt,
    String publisherId,  // ✅ 추가
    String publisherService,  // ✅ 추가
    OrderData order
) {}

// Consumer에서 검증
if (!"order-service".equals(event.publisherService())) {
    log.error("❌ Unauthorized event publisher - service: {}", event.publisherService());
    return;
}
```

**관련 Issue**: 새로운 Issue #86 생성 권장 (보안 이슈)

---

#### 3.2 External API Key 노출 위험
**현재 상황**:
- `application.yml`에 API 키 평문 저장
- Docker 환경변수로 관리 중 (개선 필요)

**보안 위험**:
- Git commit 시 API 키 노출 위험
- Docker 컨테이너 inspect로 환경변수 조회 가능

**해결 방안**:
1. **Spring Cloud Config + Vault** (권장):
   - HashiCorp Vault로 API 키 암호화 저장
   - Spring Cloud Config Server에서 런타임에 로드

2. **AWS Secrets Manager** (클라우드 배포 시):
   ```java
   @Configuration
   public class SecretsConfig {
       @Bean
       public String slackBotToken() {
           return awsSecretsManager.getSecretValue("slack-bot-token");
       }
   }
   ```

3. **현재 단계 (최소한의 보안)**:
   - `.env.docker` 파일을 `.gitignore`에 추가 ✅ (이미 적용됨)
   - README에 환경변수 템플릿만 제공 ✅ (이미 적용됨)

**현재 상태**: 🟢 로컬 개발 환경에서는 충분히 안전

---

## 4. 보상 이벤트/트랜잭션

### 🔴 Priority 1 (Critical - 분산 트랜잭션)

#### 4.1 주문 생성 실패 시 보상 이벤트 미구현
**현재 상황**:
- **order-service → notification-service** 알림 발송 흐름
- notification-service 실패 시 **order-service에 보상 이벤트 없음**

**시나리오**:
```
1. order-service: 주문 생성 성공 (DB commit)
2. notification-service: 알림 발송 시도
   - Gemini API 실패 (타임아웃, 500 에러)
   - Slack API 실패 (네트워크 에러)
3. order-service: ❌ 주문 상태가 PENDING으로 유지
   - 허브 관리자가 주문을 인지하지 못함
   - 배송 지연 발생
```

**현재 구현 (Issue #76-3)**:
- Slack 실패 시에도 `200 OK` 반환 → order-service는 성공으로 인식
- **비즈니스 요구사항 불명확**: "알림 실패가 주문 생성을 막아야 하는가?"

**해결 방안**:

**Option A: 알림 실패 시 주문 생성 실패** (Strong Consistency)
```java
// NotificationService
@Transactional
public NotificationResponse sendOrderNotification(OrderNotificationRequest request) {
    // ...
    SlackMessageResponse slackResponse = slackClientWrapper.postMessage(...);

    if (slackResponse == null || !slackResponse.isOk()) {
        throw new CustomException(ErrorCode.NOTIFICATION_SEND_FAILED);
    }
    // ...
}

// order-service (호출부)
try {
    notificationService.sendOrderNotification(request);
} catch (Exception e) {
    // 주문 생성 롤백 (Saga Pattern의 Compensating Transaction)
    orderRepository.deleteById(orderId);
    throw new CustomException(ErrorCode.ORDER_CREATION_FAILED);
}
```

**Pros**: 데이터 정합성 보장
**Cons**: 외부 API 장애가 주문 생성을 막음 (가용성 하락)

---

**Option B: 알림 실패 시 재발송 이벤트 발행** (Eventual Consistency)
```java
// NotificationService
@Transactional
public NotificationResponse sendOrderNotification(OrderNotificationRequest request) {
    // ...
    if (slackResponse == null || !slackResponse.isOk()) {
        // 1. Notification 상태를 FAILED로 저장
        savedNotification.markAsFailed(errorMsg);

        // 2. 재발송 이벤트 발행 (Kafka DLQ 또는 별도 토픽)
        kafkaTemplate.send("notification.retry", NotificationRetryEvent.of(savedNotification));

        log.warn("⚠️ Notification failed, retry event published - notificationId: {}",
                 savedNotification.getId());
    }

    // 200 OK 반환 (주문 생성은 계속 진행)
    return NotificationResponse.from(savedNotification);
}

// 별도 Scheduler (5분마다 FAILED 상태 재시도)
@Scheduled(cron = "0 */5 * * * *")
public void retryFailedNotifications() {
    List<Notification> failed = notificationRepository.findAllByStatus(MessageStatus.FAILED);
    failed.forEach(notification -> {
        // 최대 3회 재시도
        if (notification.getRetryCount() < 3) {
            SlackMessageResponse response = slackClientWrapper.postMessage(...);
            if (response.isOk()) {
                notification.markAsSent();
            } else {
                notification.incrementRetryCount();
            }
        }
    });
}
```

**Pros**: 높은 가용성, 주문 생성이 외부 API에 의존하지 않음
**Cons**: 일시적 데이터 불일치 (Eventual Consistency)

---

**Option C: Dead Letter Queue (DLQ) 패턴** (권장)
```yaml
# application.yml
spring:
  kafka:
    producer:
      retries: 3
    consumer:
      properties:
        max.poll.records: 10
```

```java
// Kafka Consumer with DLQ
@KafkaListener(
    topics = "order.created",
    containerFactory = "orderCreatedKafkaListenerContainerFactory",
    errorHandler = "kafkaErrorHandler"  // ✅ 에러 핸들러 추가
)
public void onMessage(OrderCreatedEvent event) {
    // ...
    if (slackResponse == null || !slackResponse.isOk()) {
        throw new SlackApiException("Slack API failed");  // DLQ로 이동
    }
}

@Bean
public KafkaListenerErrorHandler kafkaErrorHandler() {
    return (message, exception) -> {
        log.error("❌ Kafka message processing failed, sending to DLQ", exception);
        kafkaTemplate.send("notification.dlq", message);
        return null;
    };
}
```

**Pros**: 표준 패턴, 실패 메시지 자동 재처리, 모니터링 용이
**Cons**: Kafka 설정 복잡도 증가

---

**권장 접근**:
1. **Phase 1**: Option B (Eventual Consistency) - 현재 비즈니스 요구사항에 적합
2. **Phase 2**: Option C (DLQ) - Kafka 성숙도 향상 후 적용
3. **Issue #76-3 우선 해결**: 비즈니스 요구사항 명확화

**관련 Issue**: Issue #76-3 (Slack 실패 HTTP 응답 개선)

---

#### 4.2 배송 상태 변경 실패 시 보상 이벤트 미구현
**현재 상황**:
- **delivery-service → Kafka → notification-service** 흐름
- DeliveryStatusChangedConsumer에서 Slack 실패 시 **보상 처리 없음**

**시나리오**:
```
1. delivery-service: 배송 상태 변경 (PENDING → IN_PROGRESS)
2. Kafka: delivery.status.changed 이벤트 발행
3. notification-service: 이벤트 수신 성공
4. notification-service: Slack 발송 실패
   - notification.status = FAILED로 저장
   - ❌ delivery-service에 실패 알림 없음
```

**문제점**:
- 배송 상태는 변경되었지만 알림은 실패
- 허브 관리자가 배송 상태 변경을 인지하지 못함

**해결 방안**:

**Option A: 보상 이벤트 발행** (권장)
```java
// DeliveryStatusChangedConsumer
@Transactional
public void onMessage(DeliveryStatusChangedEvent event) {
    // ...
    SlackMessageResponse response = slackClientWrapper.postMessage(...);

    if (response == null || !response.isOk()) {
        savedNotification.markAsFailed(errorMsg);

        // 보상 이벤트 발행 (delivery-service가 구독)
        kafkaTemplate.send("notification.failed", NotificationFailedEvent.builder()
            .notificationId(savedNotification.getId())
            .deliveryId(delivery.deliveryId())
            .failureReason(errorMsg)
            .build()
        );

        log.warn("⚠️ Notification failed, compensation event published");
    }
}
```

```java
// delivery-service에 Consumer 추가
@KafkaListener(topics = "notification.failed")
public void onNotificationFailed(NotificationFailedEvent event) {
    // 1. 배송 상태에 알림 실패 플래그 추가
    delivery.markNotificationFailed();

    // 2. 관리자 대시보드에 경고 표시
    // 3. 수동 재발송 UI 제공
}
```

**Option B: 재시도 로직만 구현** (현재 Issue #76에 포함)
```java
// Notification Entity에 retryCount 필드 추가
@Scheduled(cron = "0 */5 * * * *")
public void retryFailedDeliveryNotifications() {
    List<Notification> failed = notificationRepository
        .findAllByMessageTypeAndStatus(MessageType.DELIVERY_STATUS_UPDATE, MessageStatus.FAILED);

    failed.forEach(notification -> {
        if (notification.getRetryCount() < 3) {
            // 재발송 시도
            SlackMessageResponse response = slackClientWrapper.postMessage(...);
            if (response.isOk()) {
                notification.markAsSent();
            } else {
                notification.incrementRetryCount();
            }
        }
    });
}
```

**권장 접근**: Option B (재시도만) → 현재 범위에서 충분

**관련 Issue**: Issue #76-5 (Slack error 메시지 유실), Issue #84 (배송 상태 알림 REST API)

---

## 5. 비동기 처리

### 🟡 Priority 2 (Performance)

#### 5.1 Slack 메시지 발송 비동기 처리 미구현
**현재 상황**:
- `NotificationService.sendOrderNotification()` - 동기 처리
- Gemini AI (2-5초) + Slack API (0.5-2초) = **총 2.5-7초 응답 시간**

**문제점**:
- order-service API 응답 시간 증가 → 사용자 경험 저하
- Slack API 실패 시 order-service 전체 트랜잭션 롤백 (Option A 선택 시)

**해결 방안**:

**Option A: @Async + CompletableFuture**
```java
@Service
public class NotificationService {

    @Async("notificationExecutor")
    @Transactional
    public CompletableFuture<NotificationResponse> sendOrderNotificationAsync(
            OrderNotificationRequest request) {
        NotificationResponse response = sendOrderNotification(request);
        return CompletableFuture.completedFuture(response);
    }
}

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("notification-");
        executor.initialize();
        return executor;
    }
}
```

**Option B: Spring Event + @EventListener**
```java
// order-service
@Transactional
public Order createOrder(OrderRequest request) {
    Order order = orderRepository.save(...);

    // 이벤트 발행 (비동기)
    applicationEventPublisher.publishEvent(new OrderCreatedInternalEvent(order));

    return order;  // 즉시 응답
}

// notification-service FeignClient 호출 (Event Listener에서)
@Async
@EventListener
public void handleOrderCreatedEvent(OrderCreatedInternalEvent event) {
    notificationServiceClient.sendOrderNotification(...);
}
```

**Option C: Kafka 기반 비동기 (현재 구현)** ✅
- order-service가 `order.created` 이벤트 발행
- notification-service가 Consumer로 처리
- **이미 Kafka로 비동기 처리 중** → 추가 작업 불필요

**권장 접근**:
- REST API는 동기 유지 (테스트/재발송용)
- Kafka 이벤트는 비동기 유지 ✅ (현재 상태)
- Issue #76-3 (Slack 실패 응답)과 연계하여 Option C (비동기 전환) 검토

**현재 상태**: 🟢 Kafka로 비동기 처리 완료, REST API는 동기 (정상)

---

#### 5.2 Gemini AI 호출 비동기 최적화
**현재 상황**:
- Gemini API 응답 시간: 2-5초 (평균 3초)
- 주문 알림마다 동기 호출 → 병목 발생

**해결 방안**:

**Option A: Gemini 결과 캐싱**
```java
@Cacheable(value = "geminiDeadlines", key = "#request.departureHub + '-' + #request.destinationHub")
public String calculateDepartureDeadline(OrderNotificationRequest request) {
    // 같은 경로는 캐싱 (TTL: 1시간)
    return geminiClientWrapper.generateDeadline(...);
}
```

**Option B: 배치 처리**
```java
// 주문 알림을 즉시 발송하지 않고 5분마다 배치 처리
@Scheduled(fixedDelay = 300000)  // 5분
public void processPendingNotifications() {
    List<Notification> pending = notificationRepository.findAllByStatus(MessageStatus.PENDING);

    // 경로별로 그룹핑하여 Gemini API 호출 최소화
    Map<String, List<Notification>> grouped = pending.stream()
        .collect(Collectors.groupingBy(n -> n.getDepartureHub() + "-" + n.getDestinationHub()));

    grouped.forEach((route, notifications) -> {
        String deadline = geminiClientWrapper.generateDeadline(route);
        notifications.forEach(n -> {
            n.setDeadline(deadline);
            sendSlackMessage(n);
        });
    });
}
```

**권장 접근**: Option A (캐싱) - 즉시 응답 유지하면서 성능 개선

**관련 Issue**: 새로운 Issue #87 생성 가능 (Performance Optimization)

---

## 6. Challenge 기능

### 🟢 Priority 3 (Nice to Have)

#### 6.1 Daily Route Optimization (Issue #36)
**현재 상황**: 미구현

**필요 사항**:
1. **Naver Maps API 클라이언트**
   ```java
   @Component
   public class NaverMapsApiClient {
       public NaverDirectionsResponse getDirections(
           List<String> waypoints,
           String origin,
           String destination
       ) {
           // Directions 5 API 호출 (경유지 최대 5개)
       }
   }
   ```

2. **Spring Scheduler**
   ```java
   @Scheduled(cron = "0 0 6 * * *")  // 매일 06:00
   public void optimizeDailyRoutes() {
       // 1. delivery-service에서 당일 배송 조회
       // 2. Gemini AI로 TSP 최적화
       // 3. Naver Maps로 실제 경로 계산
       // 4. Slack으로 알림 발송
   }
   ```

3. **delivery-service API 협의**
   - `GET /api/v1/deliveries/daily?date={YYYY-MM-DD}&hubId={UUID}`

**예상 소요**: 3-4일

**의존성**: delivery-service API 구현 완료 필요

---

#### 6.2 Dead Letter Queue (DLQ)
**현재 상황**: 미구현

**필요 사항**:
```yaml
# application.yml
spring:
  kafka:
    consumer:
      properties:
        max.poll.records: 10
    listener:
      ack-mode: MANUAL
```

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent>
        orderCreatedKafkaListenerContainerFactory() {
    factory.setCommonErrorHandler(new DefaultErrorHandler(
        new DeadLetterPublishingRecoverer(kafkaTemplate),
        new FixedBackOff(1000L, 3L)  // 1초 간격, 3회 재시도
    ));
}
```

**예상 소요**: 1일

---

## 7. 권장 우선순위

### 🔴 Immediate (1-2주 내)

| No | Issue | 항목 | 예상 소요 | 비고 |
|----|-------|------|----------|------|
| 1 | #76 | 알림 서비스 리스크 개선 (7개 항목) | 2일 | ⭐ 최우선 |
| 2 | #84 | 배송 상태 알림 REST API | 1일 | Issue #35 후속 |
| 3 | **#85 (신규)** | deletedBy 사용자 정보 수집 | 0.5일 | SecurityContext 활용 |
| 4 | **#86 (신규)** | Kafka Consumer 보안 강화 | 1일 | 이벤트 서명 검증 |

**Total**: 4.5일

---

### 🟡 Important (1개월 내)

| No | Issue | 항목 | 예상 소요 | 비고 |
|----|-------|------|----------|------|
| 5 | #36 | Daily route optimization | 3-4일 | Challenge 기능 |
| 6 | **#87 (신규)** | Gemini AI 캐싱 | 0.5일 | Performance 개선 |
| 7 | **#88 (신규)** | DLQ 구현 | 1일 | Kafka 안정성 |
| 8 | - | TODO 주석 정리 | 0.5일 | Code cleanup |
| 9 | - | user-service API 협의 | 1일 | 팀 협업 |

**Total**: 6-7일

---

### 🟢 Nice to Have (나중에)

| No | 항목 | 예상 소요 | 비고 |
|----|------|----------|------|
| 10 | External API Key Vault 통합 | 1일 | 클라우드 배포 시 |
| 11 | E2E 통합 테스트 | 2일 | QA 강화 |
| 12 | Performance 테스트 | 1일 | 부하 테스트 |
| 13 | API 계약 테스트 (Pact) | 1일 | MSA 안정성 |

**Total**: 5일

---

## 📊 요약

### 미구현 기능 통계

| 카테고리 | Critical | Important | Nice to Have | Total |
|---------|----------|-----------|--------------|-------|
| TODO 정리 | 1 | 6 | 0 | 7 |
| 타 서비스 의존성 | 1 | 2 | 0 | 3 |
| 보안 | 2 | 0 | 1 | 3 |
| 보상 트랜잭션 | 2 | 0 | 0 | 2 |
| 비동기 처리 | 0 | 1 | 0 | 1 |
| Challenge | 0 | 1 | 0 | 1 |
| **Total** | **6** | **10** | **1** | **17** |

---

### 보안 이슈 요약

| No | 이슈 | Severity | CVSS | 해결 방안 |
|----|------|----------|------|----------|
| 1 | Kafka Consumer 인증 부재 | 🔴 High | 7.5 | 이벤트 서명 검증 |
| 2 | deletedBy 하드코딩 (감사 추적 실패) | 🟡 Medium | 5.0 | SecurityContext 활용 |
| 3 | External API Key 노출 위험 | 🟢 Low | 3.0 | 이미 .gitignore 적용 |

---

### 보상 이벤트 필요 여부

| 시나리오 | 현재 상태 | 보상 필요 | 우선순위 | 해결 방안 |
|---------|----------|----------|----------|----------|
| 주문 알림 실패 | ❌ 보상 없음 | ✅ 필요 | 🔴 High | Eventual Consistency + 재시도 |
| 배송 상태 알림 실패 | ❌ 보상 없음 | ⚠️ 선택 | 🟡 Medium | 재시도 로직 (Issue #76) |
| Gemini API 실패 | ✅ Fallback 존재 | ✅ 충분 | - | Resilience4j Retry |
| Slack API 실패 | ✅ 재시도 3회 | ✅ 충분 | - | Resilience4j Retry |

**결론**:
- ✅ **보상 이벤트 필요** (주문 알림 실패 시)
- ✅ **Saga Pattern 고려** (분산 트랜잭션)
- ✅ **Issue #76-3 우선 해결** (비즈니스 요구사항 명확화)

---

## 📝 신규 Issue 제안

### Issue #85: deletedBy 사용자 정보 수집
**Type**: Refactor
**Priority**: 🟡 Medium
**Effort**: 0.5일

**Description**:
NotificationRepositoryImpl의 `markAsDeleted("SYSTEM")` 하드코딩을 SecurityContext 기반 사용자 정보로 변경

**To-do**:
- [ ] SecurityContext 헬퍼 메서드 추가 (AuthContextUtil.getCurrentUsername())
- [ ] NotificationRepositoryImpl 수정
- [ ] ExternalApiLogRepositoryImpl 수정
- [ ] 단위 테스트 작성

---

### Issue #86: Kafka Consumer 보안 강화
**Type**: Security
**Priority**: 🔴 High
**Effort**: 1일

**Description**:
Kafka 이벤트 검증 로직 추가 (이벤트 서명 또는 발행자 정보 검증)

**To-do**:
- [ ] Event DTO에 서명 필드 추가 (OrderCreatedEvent, DeliveryStatusChangedEvent)
- [ ] 서명 생성/검증 로직 구현 (HMAC-SHA256)
- [ ] Consumer에 검증 로직 추가
- [ ] 통합 테스트 작성
- [ ] order-service, delivery-service와 협의

---

### Issue #87: Gemini AI 캐싱
**Type**: Performance
**Priority**: 🟡 Medium
**Effort**: 0.5일

**Description**:
Gemini API 응답 캐싱 (같은 경로는 1시간 TTL)

**To-do**:
- [ ] Spring Cache 설정 (Caffeine)
- [ ] NotificationService.calculateDepartureDeadline() 메서드에 @Cacheable 적용
- [ ] Cache key 전략 설계 (departureHub-destinationHub)
- [ ] 성능 테스트 (응답 시간 측정)

---

### Issue #88: Dead Letter Queue 구현
**Type**: Feature
**Priority**: 🟡 Medium
**Effort**: 1일

**Description**:
Kafka Consumer 실패 메시지 자동 재처리 (DLQ 패턴)

**To-do**:
- [ ] KafkaListenerContainerFactory에 ErrorHandler 추가
- [ ] notification.dlq 토픽 생성
- [ ] DLQ Consumer 구현 (수동 재처리 UI 연동)
- [ ] 통합 테스트 작성

---

**문서 버전**: v1.0
**최종 수정일**: 2025-11-12
**작성자**: notification-service 개발팀