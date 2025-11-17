# PR #67: 배송 생성 기능 및 Kafka 이벤트 수신

## Issue Number
> closed #63

## 📝 Description

### Delivery Service - Kafka Event-Driven Architecture
- **Kafka Consumer 구현**: `order.created` 이벤트 수신 시 배송 자동 생성
- **Idempotency 처리**: 동일 `orderId`로 중복 요청 시 기존 배송 ID 반환
- **테스트 환경 구성**: H2 DB + Embedded Kafka
- **테스트 코드 작성**: 단위 테스트 + 통합 테스트

## 📊 변경 사항

### 변경 파일 (18개 파일, +618/-8)

#### Delivery Service - 전체 구조 생성
- **DeliveryServiceApplication**: Spring Boot 메인 클래스 + `@ConfigurationPropertiesScan`
- **build.gradle**: Kafka, JPA, Validation 의존성 추가 + H2 테스트 DB

#### Domain Layer
- **Delivery Entity**: 
  - `@Id` PK: `deliveryId` (UUID)
  - Unique Constraint: `order_id` (중복 배송 생성 방지)
  - 필수 필드: `orderId`, `status`, `startHubId`, `destinationHubId`, `receiverName`, `receiverAddress`
  - Optional: `receiverSlackId`, `deliveryStaffId`
  - Factory method: `createFromOrder()` - 초기 상태 `WAITING_AT_HUB`

- **DeliveryStatus Enum**: 
  - 7가지 상태: `WAITING_AT_HUB`, `MOVING_BETWEEN_HUBS`, `ARRIVED_DEST_HUB`, `OUT_FOR_DELIVERY`, `MOVING_TO_COMPANY`, `COMPLETED`, `CANCELED`

- **DeliveryRepository**: 
  - `existsByOrderId(UUID orderId)`: 중복 체크용
  - `findByOrderId(UUID orderId)`: 기존 배송 조회용
  - Spring Data JPA 인터페이스 직접 사용 (Repository 분리 없음)

#### Application Layer
- **DeliveryService**: 
  - `createIfAbsentFromOrder(OrderCreatedMessage)`: 
    - 중복 체크 → 기존 배송 ID 반환
    - 없으면 신규 생성 → 저장 후 ID 반환
  - **Idempotency 보장**: 같은 orderId로 여러 번 호출해도 안전

- **OrderCreatedMessage**: 
  - Kafka 메시지 DTO (`record` 타입)
  - 중첩 구조: `Order`, `Receiver`, `Route`
  - 필드: `eventId`, `occurredAt`, `order`

#### Infrastructure Layer
- **OrderCreatedConsumer**: 
  - `@KafkaListener(topics = "#{@topicProperties.orderCreated}", groupId = "delivery-service")`
  - 메시지 수신 → DeliveryService 호출
  - 로그: 주문 수신 확인 + 배송 생성 확인

#### Configuration
- **TopicProperties**: 
  - `@ConfigurationProperties(prefix = "topics")`
  - `orderCreated` topic 이름 외부 설정 가능
- **application.yml**: 
  - Kafka 설정 (bootstrap-servers, consumer group, trusted packages)
  - PostgreSQL 설정
  - Eureka 설정

#### Test
- **DeliveryServiceIdempotencyTest**: 
  - `@DataJpaTest` + `@Import(DeliveryService.class)`
  - 동일 orderId로 2번 호출 → deliveryId 동일 확인
  - DB에 1개만 저장되었는지 확인

- **OrderCreatedConsumerIT**: 
  - `@SpringBootTest` + `@EmbeddedKafka`
  - Kafka 메시지 발행 → Consumer 수신 확인
  - DB에 배송 생성 확인 (Awaitility 사용)

- **application-test.yml**: 
  - H2 DB 설정 (in-memory, PostgreSQL 모드)
  - Embedded Kafka 설정
  - JsonDeserializer 신뢰 패키지 설정

#### Environment & Build
- **.env.example**: Kafka 환경 변수 추가 (`KAFKA_BOOTSTRAP_SERVERS`, `ORDER_CREATED_TOPIC`)
- **settings.gradle**: `delivery-service` 모듈 추가 (알파벳 순 정렬)
- **gradle-wrapper**: 8.13 → 8.10.2 (버전 다운그레이드?)
- **application.yml**: 모든 서비스에 `server.port` 명시

## 🌐 Test Result

### 로컬 환경 테스트
- Kafka + PostgreSQL 환경에서 정상 동작 확인
- Postman으로 주문 생성 이벤트 발행 시 배송 자동 생성 확인
- 로그에 `📦 Received order.created event`, `🚚 Delivery created/exists` 출력 확인

### 단위 테스트
- `DeliveryServiceIdempotencyTest`: ✅ 통과
- 동일 orderId 2번 호출 → deliveryId 동일, DB 1건만 저장

### 통합 테스트
- `OrderCreatedConsumerIT`: ✅ 통과
- Embedded Kafka 메시지 발행 → Consumer 수신 → DB 저장 확인 (Awaitility 10초 대기)

## 🔍 코드 리뷰 결과

### ✅ 잘된 점

#### 1. Idempotency 처리 우수 (⭐⭐⭐⭐⭐)
```java
@Transactional
public UUID createIfAbsentFromOrder(OrderCreatedMessage msg) {
    var orderId = msg.order().orderId();
    if (deliveryRepository.existsByOrderId(orderId)) {
        return deliveryRepository.findByOrderId(orderId)
                .orElseThrow()
                .getDeliveryId();
    }
    
    var delivery = Delivery.createFromOrder(...);
    return deliveryRepository.save(delivery).getDeliveryId();
}
```
- Kafka 메시지 중복 수신 대비
- DB Unique Constraint (`uk_delivery_order`) + 애플리케이션 레벨 중복 체크 (이중 방어)
- MSA 환경에서 필수적인 패턴

#### 2. 테스트 커버리지 우수
- 단위 테스트: Idempotency 검증
- 통합 테스트: Embedded Kafka 환경에서 전체 플로우 검증
- H2 DB 사용으로 테스트 속도 개선
- Awaitility 사용으로 비동기 처리 안정적 검증

#### 3. Event-Driven Architecture 정확한 구현
- Kafka Consumer 설정 정확
- Topic 이름 외부 설정 가능 (`@ConfigurationProperties`)
- JsonDeserializer 신뢰 패키지 설정 (보안)
- Consumer Group 명확히 지정

#### 4. Delivery Status Enum 체계적
- 배송 생명주기를 7단계로 명확히 정의
- 추후 상태 변경 로직 구현 시 활용 가능

#### 5. 로깅 명확
- Emoji 사용으로 로그 가독성 향상 (📦, 🚚)
- orderId, deliveryId 모두 로깅하여 추적 용이

### 🚨 Critical Issues (필수 수정)

#### 1. Repository 패턴 불일치 (심각도: 중간)

**현재 코드**:
```java
// domain.repository.DeliveryRepository
public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {
    boolean existsByOrderId(UUID orderId);
    Optional<Delivery> findByOrderId(UUID orderId);
}
```

**문제점**:
- Domain layer에서 직접 JpaRepository 상속
- 팀 DDD 패턴 불일치 (hub-service, company-service, product-service는 분리)
- Domain이 Infrastructure(Spring Data JPA)에 의존

**권장 수정** (팀 표준 패턴):
```java
// domain.repository.DeliveryRepository (인터페이스만)
public interface DeliveryRepository {
    Delivery save(Delivery delivery);
    boolean existsByOrderId(UUID orderId);
    Optional<Delivery> findByOrderId(UUID orderId);
}

// infrastructure.persistence.DeliveryJpaRepository
public interface DeliveryJpaRepository extends JpaRepository<Delivery, UUID> {
    boolean existsByOrderId(UUID orderId);
    Optional<Delivery> findByOrderId(UUID orderId);
}

// infrastructure.persistence.DeliveryRepositoryImpl
@Repository
@RequiredArgsConstructor
public class DeliveryRepositoryImpl implements DeliveryRepository {
    private final DeliveryJpaRepository jpaRepository;
    
    @Override
    public Delivery save(Delivery delivery) {
        return jpaRepository.save(delivery);
    }
    
    @Override
    public boolean existsByOrderId(UUID orderId) {
        return jpaRepository.existsByOrderId(orderId);
    }
    
    @Override
    public Optional<Delivery> findByOrderId(UUID orderId) {
        return jpaRepository.findByOrderId(orderId);
    }
}
```

#### 2. Delivery Entity에 BaseEntity 미적용 (심각도: 중간)

**현재 코드**:
```java
@Entity
@Table(name = "p_deliveries")
public class Delivery {
    @Id
    private UUID deliveryId;
    // ...
    // 감사 필드 없음 (createdAt, createdBy, updatedAt, updatedBy, deleted 등)
}
```

**문제점**:
- 팀 표준 `BaseEntity` 미상속
- Soft Delete 패턴 미적용
- 감사 필드(created_at, created_by, updated_at, updated_by) 없음
- 다른 서비스(hub, company, product)와 불일치

**해결책**:
```java
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "p_deliveries", uniqueConstraints = {
    @UniqueConstraint(name = "uk_delivery_order", columnNames = {"order_id"})
})
public class Delivery extends BaseEntity { // BaseEntity 상속
    
    @Id
    @Column(name = "delivery_id", nullable = false, updatable = false)
    private UUID deliveryId;
    
    // ...
    
    // Factory method에서 감사 필드 자동 처리됨 (BaseEntity의 @PrePersist)
}
```

**주의사항**:
- BaseEntity가 `@EntityListeners(AuditingEntityListener.class)` 적용 확인
- `@EnableJpaAuditing` 설정 확인

#### 3. Delivery Entity 필드 타입 불일치 (심각도: 높음)

**현재 코드**:
```java
@Column(name = "start_hub_id", nullable = false, length = 64)
private String startHubId;  // ❌ String 타입

@Column(name = "destination_hub_id", nullable = false, length = 64)
private String destinationHubId;  // ❌ String 타입

@Column(name = "delivery_staff_id", length = 64)
private String deliveryStaffId;  // ❌ String 타입
```

**문제점**:
- hubId는 UUID인데 String으로 저장 (타입 불일치)
- deliveryStaffId는 Long인데 String으로 저장
- **PR #73과 동일한 문제** (타입 변환 오버헤드, 예외 위험)
- 다른 서비스(hub, company, product)와 불일치

**해결책**:
```java
@Column(name = "start_hub_id", nullable = false)
private UUID startHubId;  // String → UUID

@Column(name = "destination_hub_id", nullable = false)
private UUID destinationHubId;  // String → UUID

@Column(name = "delivery_staff_id")
private Long deliveryStaffId;  // String → Long

// Factory method 수정
public static Delivery createFromOrder(
        UUID orderId,
        UUID startHubId,      // String → UUID
        UUID destinationHubId, // String → UUID
        String receiverName,
        String receiverAddress,
        String receiverSlackId) {
    return new Delivery(
        UUID.randomUUID(),
        orderId,
        DeliveryStatus.WAITING_AT_HUB,
        startHubId,
        destinationHubId,
        receiverName,
        receiverAddress,
        receiverSlackId
    );
}
```

**OrderCreatedMessage도 수정 필요**:
```java
public record Route(UUID startHubId, UUID destinationHubId) { // String → UUID
}
```

#### 4. Gradle Wrapper 버전 다운그레이드 (심각도: 낮음)

**변경 사항**:
```diff
-distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip
+distributionUrl=https\://services.gradle.org/distributions/gradle-8.10.2-bin.zip
```

**문제점**:
- 8.13 → 8.10.2 다운그레이드 (의도된 변경인지 확인 필요)
- 팀원 간 Gradle 버전 불일치 가능성

**확인 필요**:
- 다운그레이드 이유 확인
- 필요 없으면 8.13 유지 권장

### ⚠️ 개선 권장 사항

#### 1. DeliveryService 트랜잭션 경계 개선

**현재 코드**:
```java
@Transactional
public UUID createIfAbsentFromOrder(OrderCreatedMessage msg) {
    var orderId = msg.order().orderId();
    if (deliveryRepository.existsByOrderId(orderId)) { // SELECT
        return deliveryRepository.findByOrderId(orderId) // SELECT (중복 쿼리)
                .orElseThrow()
                .getDeliveryId();
    }
    // ...
}
```

**문제점**:
- `existsByOrderId()` + `findByOrderId()` 2번의 SELECT 쿼리
- 성능 비효율

**개선안**:
```java
@Transactional
public UUID createIfAbsentFromOrder(OrderCreatedMessage msg) {
    var orderId = msg.order().orderId();
    
    // 1번의 쿼리로 처리
    Optional<Delivery> existing = deliveryRepository.findByOrderId(orderId);
    if (existing.isPresent()) {
        return existing.get().getDeliveryId();
    }
    
    var delivery = Delivery.createFromOrder(...);
    return deliveryRepository.save(delivery).getDeliveryId();
}
```

#### 2. Kafka Consumer 에러 처리 강화

**현재 코드**:
```java
@KafkaListener(topics = "#{@topicProperties.orderCreated}", groupId = "delivery-service")
public void onMessage(OrderCreatedMessage message) {
    log.info("📦 Received order.created event for orderId={}", message.order().orderId());
    var deliveryId = deliveryService.createIfAbsentFromOrder(message);
    log.info("🚚 Delivery created/exists for orderId={}, deliveryId={}",
            message.order().orderId(), deliveryId);
}
```

**문제점**:
- 예외 발생 시 처리 로직 없음
- 메시지 재처리 정책 없음

**권장 추가**:
```java
@KafkaListener(
    topics = "#{@topicProperties.orderCreated}", 
    groupId = "delivery-service"
)
public void onMessage(OrderCreatedMessage message) {
    try {
        log.info("📦 Received order.created event for orderId={}", message.order().orderId());
        var deliveryId = deliveryService.createIfAbsentFromOrder(message);
        log.info("🚚 Delivery created/exists for orderId={}, deliveryId={}",
                message.order().orderId(), deliveryId);
    } catch (Exception e) {
        log.error("❌ Failed to process order.created event for orderId={}. Error: {}", 
            message.order().orderId(), e.getMessage(), e);
        // DLQ(Dead Letter Queue) 발행 또는 재시도 로직
        throw e; // Kafka가 재시도하도록
    }
}
```

#### 3. Kafka Configuration 추가

**현재**: application.yml에 기본 설정만 존재

**권장 추가**:
```java
// config.KafkaConfig.java
@Configuration
public class KafkaConfig {
    
    @Bean
    public ConsumerFactory<String, OrderCreatedMessage> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "delivery-service");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // 수동 커밋
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.oneforlogis.*");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, OrderCreatedMessage.class);
        
        return new DefaultKafkaConsumerFactory<>(config);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedMessage> 
            kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderCreatedMessage> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE); // 수동 ACK
        return factory;
    }
}
```

#### 4. application.yml Port 정리

**변경 사항**:
- 모든 서비스 application.yml에 `server.port` 명시
- notification-service: `8701` → `8700` 환경 변수 사용
- gateway-service: `8000` 명시

**확인 필요**:
- Port 변경이 의도된 것인지 확인
- 팀 Port 할당 표준 문서화 권장

#### 5. TopicProperties Validation

**현재 코드**:
```java
@Component
@ConfigurationProperties(prefix = "topics")
public class TopicProperties {
    private String orderCreated = "order.created";
    // getter/setter
}
```

**권장 추가**:
```java
@Component
@ConfigurationProperties(prefix = "topics")
@Validated
public class TopicProperties {
    
    @NotBlank(message = "order.created topic은 필수입니다.")
    private String orderCreated = "order.created";
    
    // getter/setter
}
```

#### 6. 통합 테스트 Timeout 조정

**현재 코드**:
```java
await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
    assertThat(deliveryRepository.findByOrderId(orderId)).isPresent()
);
```

**권장**:
- 10초는 적절하나, CI/CD 환경에서는 더 길게 설정 필요
- 또는 pollInterval 설정 추가

```java
await()
    .atMost(Duration.ofSeconds(10))
    .pollInterval(Duration.ofMillis(500))
    .untilAsserted(() ->
        assertThat(deliveryRepository.findByOrderId(orderId)).isPresent()
    );
```

## 📊 종합 평가

| 항목 | 점수 | 평가 |
|------|------|------|
| Idempotency 처리 | ⭐⭐⭐⭐⭐ | DB Constraint + 애플리케이션 레벨 이중 방어 |
| Event-Driven 구현 | ⭐⭐⭐⭐⭐ | Kafka Consumer 정확한 구현 |
| 테스트 커버리지 | ⭐⭐⭐⭐⭐ | 단위 + 통합 테스트 우수 |
| 팀 표준 준수 | ⭐⭐⭐☆☆ | Repository 패턴 불일치, BaseEntity 미적용 |
| 타입 일관성 | ⭐⭐☆☆☆ | hubId, staffId String 타입 (UUID/Long으로 변경 필요) |
| 에러 처리 | ⭐⭐⭐☆☆ | 기본 로깅만 있음, 재시도 로직 없음 |

**총평**: Idempotency 처리와 테스트 코드가 매우 우수하고, Event-Driven Architecture를 정확히 구현함. 그러나 팀 DDD 패턴(Repository 분리, BaseEntity 상속) 미적용과 Entity 필드 타입 불일치 수정 필요.

## ✅ Merge 전 체크리스트

### 필수 수정 (Blocking Issues)
- [ ] **Repository 패턴 분리** (domain.repository vs infrastructure.persistence)
- [ ] **BaseEntity 상속** (감사 필드, Soft Delete 패턴)
- [ ] **Entity 필드 타입 수정** (hubId: String → UUID, staffId: String → Long)
- [ ] **OrderCreatedMessage Route 타입 수정** (String → UUID)

### 강력 권장
- [ ] DeliveryService 쿼리 최적화 (existsByOrderId + findByOrderId → findByOrderId만)
- [ ] Kafka Consumer 에러 처리 강화 (try-catch, 로깅, DLQ)
- [ ] Kafka Configuration 추가 (수동 커밋, 재시도 정책)
- [ ] Gradle Wrapper 버전 확인 (8.13 vs 8.10.2)

### 선택 사항 (추후 개선)
- [ ] TopicProperties Validation 추가
- [ ] 통합 테스트 pollInterval 설정
- [ ] Port 할당 표준 문서화

## 🔗 Related Links
- PR: https://github.com/14th-anniv/one-for-logis/pull/67
- Issue #63: 배송 생성 기능
- Branch: `feature/#63-create-delivery`
- Related: Order Service (Kafka Producer), PR #73 (배송 단건 조회)

## 👥 Author
- @dain391

## 💬 To Reviewer
> Idempotency 처리 로직이 자연스럽게 동작하는지 확인 부탁드립니다.

**리뷰어 답변**:
- Idempotency 처리 로직은 **매우 우수**합니다! 👍
- DB Unique Constraint + 애플리케이션 레벨 중복 체크로 이중 방어하여 완벽합니다.
- 테스트 코드도 단위/통합 모두 작성하여 품질이 높습니다.
- **Repository 패턴 분리**와 **BaseEntity 상속**을 적용하여 팀 표준과 일치시키세요.
- **Entity 필드 타입**을 UUID/Long으로 수정하세요 (PR #73과 동일 이슈).
- Kafka Consumer 에러 처리를 강화하면 production-ready입니다!

## 🎯 추후 작업 권장

### 1. 배송 담당자 자동 할당 (Issue 등록)
```java
// DeliveryService
@Transactional
public UUID createIfAbsentFromOrder(OrderCreatedMessage msg) {
    // ...
    var delivery = Delivery.createFromOrder(...);
    
    // 배송 담당자 자동 할당 (Round-Robin)
    assignDeliveryStaff(delivery, msg.order().route().destinationHubId());
    
    return deliveryRepository.save(delivery).getDeliveryId();
}

private void assignDeliveryStaff(Delivery delivery, UUID hubId) {
    // Hub별 배송 담당자 조회 (FeignClient)
    // assign_order 기준으로 Round-Robin 할당
    // delivery.setDeliveryStaffId(...)
}
```

### 2. 배송 경로 자동 생성 (Issue 등록)
```java
// HubClient 호출하여 최단 경로 조회
// DeliveryRoute 생성 (delivery_id, sequence, hub_id, estimated_distance, estimated_time)
```

### 3. Notification Service 연동 (Issue 등록)
```java
// 배송 생성 후 Slack 알림 발송
// notificationClient.sendDeliveryCreatedNotification(...)
```

---
**리뷰어**: Claude (AI Code Reviewer)  
**리뷰 완료일**: 2025-11-11  
**상태**: 리뷰 완료 - Repository 패턴 및 Entity 타입 수정 필요  
**중요도**: ⭐⭐⭐⭐☆ (Event-Driven Architecture 핵심 기능)
