# 알림 서비스 발표 자료

**작성일**: 2024-11-13
**작성자**: 박근용
**발표 시간**: 30~40초 (팀 전체 발표 8분 중)

---

## 📋 발표 개요

### 목적
- 팀 프로젝트 발표에서 알림 서비스의 핵심 기술과 트러블 슈팅을 효과적으로 전달
- 제한된 시간(30~40초) 내에 차별화 포인트 강조

### 발표 구성
**Option 1** (40초 - Kafka + MSA 통신) ⭐ 추천:
1. **핵심 기술 구현** (20초): Kafka 이벤트 기반 알림 시스템
2. **트러블 슈팅** (15초): MSA 서비스 간 통신 안정성
3. **핵심 성과** (5초): 주요 달성 사항 요약

**Option 2** (30초 - MSA 통신 안정성만):
1. **MSA 통신 안정성** (25초): FeignClient Fallback + FeignException Handler
2. **핵심 성과** (5초): 주요 달성 사항 요약

---

## 🎤 발표 스크립트

### Option 1: 40초 버전 (Kafka + MSA 통신 안정성) ⭐ 추천

> "알림 서비스의 핵심은 **Kafka 이벤트 기반 알림 시스템**입니다.
>
> MSA 환경에서 9개 서비스 간 느슨한 결합이 필요했는데, 주문 생성 시 알림 발송은 비동기로 처리해도 되는 작업이라 **Kafka를 도입**했습니다.
>
> order-service가 주문을 생성하면 Kafka에 이벤트를 발행하고, notification-service가 이를 받아서 **Google Gemini AI**로 출발 시한을 계산한 후 Slack으로 알립니다.
>
> 예를 들어 '12월 12일 3시까지 도착' 요청이 오면, 경기북부에서 대전, 부산을 거쳐 가는 경로를 분석해 '12월 11일 오후 2시 출발 필요'라고 계산합니다.
>
> **트러블 슈팅**으로는 **MSA 서비스 간 통신 안정성**을 개선했습니다. user-service 호출 시 두 가지 예외 상황을 분리해서 처리했습니다.
>
> 첫째, 네트워크 장애나 타임아웃은 **FeignClient Fallback**으로 Circuit Breaker를 적용했습니다. Hystrix의 Circuit Breaker 패턴처럼 장애 발생 시 Fallback 메서드가 실행되어 서비스 장애를 격리합니다. null 대신 예외를 던져서 NPE 위험을 제거했습니다.
>
> 둘째, HTTP 에러 응답(403, 404, 500)은 **FeignException Handler**로 처리했습니다. FeignException에서 HTTP 상태 코드를 추출하여 동일한 상태 코드로 응답하고, switch expression으로 사용자 친화적인 한글 메시지로 변환합니다.
>
> 결과적으로 order-service는 알림 전송을 기다리지 않아 응답 속도가 빨라지고, 외부 서비스 장애가 알림 서비스로 전파되지 않습니다."

### Option 2: 30초 버전 (MSA 통신 안정성 중심)

> "알림 서비스는 **MSA 환경에서 서비스 간 통신 안정성**을 핵심으로 구현했습니다.
>
> notification-service는 user-service, Slack API, Gemini API 등 여러 외부 서비스와 통신하는데, 외부 서비스 장애가 알림 서비스로 전파되는 문제가 있었습니다.
>
> 이를 해결하기 위해 **두 가지 예외 상황을 분리**해서 처리했습니다.
>
> 첫째, 네트워크 장애나 타임아웃은 **FeignClient Fallback**으로 Circuit Breaker를 적용했습니다.
> Spring Cloud OpenFeign의 Circuit Breaker 통합 기능을 사용하여, 장애 발생 시 자동으로 Fallback 메서드가 실행됩니다.
> Fallback에서는 null을 반환하지 않고 명시적인 예외를 던져서, 상위 레이어에서 NPE 없이 안전하게 에러를 처리할 수 있습니다.
>
> 둘째, HTTP 에러 응답(403, 404, 500)은 **FeignException Handler**로 처리했습니다.
> FeignException.status()로 원본 HTTP 상태 코드를 추출하고, Java 17 switch expression으로 상태 코드별 한글 메시지를 매핑합니다.
> ResponseEntity에 동일한 HTTP 상태 코드를 설정하여 클라이언트가 정확한 에러 타입을 파악할 수 있습니다.
>
> 결과적으로 외부 서비스 장애가 발생해도 알림 서비스는 정상 동작하고, 클라이언트는 명확한 에러 정보를 받을 수 있습니다."

---

## 📌 핵심 기술 구현: Kafka 이벤트 기반 알림 시스템 (20초)

### 1. Kafka 도입 배경 및 이유

**도입 배경**:
- MSA 환경에서 9개 서비스 간 느슨한 결합 필요
- 주문 생성 시 알림 발송은 비동기 처리 가능한 작업
- order-service가 알림 발송 결과를 기다릴 필요 없음
- 알림 서비스 장애 시에도 주문 생성은 정상 진행되어야 함

**Kafka 선택 이유**:
- 이벤트 기반 아키텍처: Producer-Consumer 패턴으로 서비스 간 의존성 제거
- 메시지 영속성: Kafka 브로커에 메시지 저장 → 알림 서비스 재시작 시에도 메시지 유실 없음
- At-Least-Once 전달 보장: 네트워크 장애 시에도 메시지 재전송
- 수평 확장 가능: Consumer Group으로 알림 서비스 인스턴스 여러 개 운영 가능

**기대 효과**:
- 응답 시간 개선: order-service API 응답 속도 향상 (알림 전송 대기 불필요)
- 장애 격리: 알림 서비스 장애가 주문 생성에 영향 없음
- 재처리 가능: Kafka 메시지 보관 → 실패한 알림 재발송 가능

### 2. Kafka 설정 및 구현

#### 2-1. Docker 환경 설정
```yaml
# docker-compose-local.yml
kafka:
  image: confluentinc/cp-kafka:7.5.0
  environment:
    KAFKA_ADVERTISED_LISTENERS:
      PLAINTEXT://localhost:9092,           # 외부 접속용
      PLAINTEXT_INTERNAL://kafka:29092      # 컨테이너 간 통신용
    KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'
  ports:
    - "9092:9092"
```

**리스너 포트 분리 이유**:
- 9092: 호스트 머신에서 테스트/개발용
- 29092: 도커 네트워크 내부 서비스 간 통신용

#### 2-2. Spring Kafka Consumer 설정
```yaml
# application.yml
spring:
  kafka:
    bootstrap-servers: kafka:29092
    consumer:
      group-id: notification-service
      value-deserializer: ErrorHandlingDeserializer
      properties:
        spring.deserializer.value.delegate.class: JsonDeserializer
        spring.json.trusted.packages: "com.oneforlogis.*"
        spring.json.use.type.headers: false
        spring.json.value.default.type: "OrderCreatedEvent"

topics:
  order-created: order.created
  delivery-status-changed: delivery.status.changed
```

**주요 설정**:
- ErrorHandlingDeserializer: 역직렬화 실패 시 Consumer 중단 방지
- JsonDeserializer: JSON 메시지를 Java 객체로 자동 변환
- trusted.packages: Jackson 보안 (패키지 화이트리스트)
- use.type.headers: false → default.type 사용 (테스트 편의성)

#### 2-3. Event DTO (record 패턴)
```java
public record OrderCreatedEvent(
    String eventId,              // 멱등성 보장용 고유 ID
    OffsetDateTime occurredAt,   // 이벤트 발생 시각
    OrderData order              // 주문 정보
) {
    public record OrderData(
        UUID orderId,
        String requestDetails,    // "12월 12일 3시까지 도착"
        RouteData route,          // 출발-경유-도착 허브
        HubManagerData hubManager // Slack ID 포함
    ) {}
}
```

**record 선택 이유**:
- 불변 데이터: 이벤트는 변경되지 않아야 함
- Jackson 자동 지원: 직렬화/역직렬화 자동 처리
- 간결한 코드: boilerplate 제거

#### 2-4. Kafka Consumer 구현
```java
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    @KafkaListener(topics = "#{@topicProperties.orderCreated}")
    public void onMessage(OrderCreatedEvent event) {
        // 1. 멱등성 체크
        if (notificationRepository.existsByEventId(event.eventId())) {
            log.info("⏭️ 이미 처리된 이벤트 - eventId: {}", event.eventId());
            return; // skip
        }

        // 2. Event → Request DTO 변환
        OrderNotificationRequest request = convertToRequest(event);

        // 3. 알림 발송 (Gemini AI + Slack)
        notificationService.sendOrderNotificationFromEvent(request, event.eventId());

        log.info("✅ 알림 전송 완료 - orderId: {}", event.order().orderId());
    }
}
```

### 3. 멱등성 보장 메커니즘

**필요성**:
- Kafka At-Least-Once: 네트워크 장애 시 동일 메시지 재전송
- Offset 커밋 실패 시 중복 이벤트 수신 가능
- 중복 처리 방지 필수: 같은 알림이 여러 번 발송되면 안 됨

**구현 방법**:
- 각 이벤트에 고유 `event_id` 부여
- Consumer에서 `existsByEventId()` 먼저 체크
- 이미 처리된 이벤트: 로그만 남기고 return
- 새 이벤트: 알림 발송 + DB 저장 시 `event_id` 함께 저장
- DB 테이블 UNIQUE 제약조건: 애플리케이션 + DB 이중 보장

**테스트 검증**:
```bash
# 동일한 eventId로 2번 발행
# 결과: DB에 1개만 저장, 두 번째는 skip
```

### 4. 전체 아키텍처 플로우
```
[order-service]
    ↓ Kafka Produce
[order.created Topic]
    ↓ Consumer
[notification-service]
    ↓ 멱등성 체크 (existsByEventId)
    ↓ Google Gemini AI (출발 시한 계산)
    ↓ Slack API (알림 전송)
    ↓ DB 저장 (event_id 포함)
[허브 관리자 Slack 수신]
```

**기술 스택**:
- Apache Kafka 3.7.1 (Confluent Platform 7.5.0)
- Spring Kafka 3.2.2
- Google Gemini 1.5 Flash API
- Slack API

**관련 파일**:
- `OrderCreatedConsumer.java`, `DeliveryStatusChangedConsumer.java`
- `OrderCreatedEvent.java` (record DTO)
- `TopicProperties.java` (@ConfigurationProperties)
- `application.yml` (Kafka Consumer 설정)
- `docker-compose-local.yml` (Kafka + Zookeeper)
- Issue #35, PR #83 참조

---

## 🔧 트러블 슈팅: MSA 서비스 간 통신 안정성 (20초)

**문제 상황**:
- user-service 호출 시 두 가지 예외 상황 발생 가능
- **네트워크 장애/타임아웃**: 서비스 장애가 알림 서비스로 전파
- **HTTP 에러 응답** (403, 404, 500): 기술적 에러 메시지 사용자에게 노출
- HTTP 상태 코드 불일치 (500 응답에 403 에러)

**해결 방법**:

#### 2-1. FeignClient Fallback (Circuit Breaker 패턴)
```java
@FeignClient(
    name = "user-service",
    fallback = UserServiceClientFallback.class
)
public interface UserServiceClient {
    @GetMapping("/api/v1/users/me")
    ApiResponse<UserResponse> getMyInfo(@RequestHeader("X-User-Id") UUID userId);
}

@Component
public class UserServiceClientFallback implements UserServiceClient {
    @Override
    public ApiResponse<UserResponse> getMyInfo(UUID userId) {
        // Circuit Breaker 상황에서 예외 발생 (null 반환 방지 → NPE 위험 제거)
        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR,
            "User service is temporarily unavailable");
    }
}
```

**적용 시점**: 네트워크 장애, 타임아웃, Circuit Open 상태

**동작 원리**:
1. **Spring Cloud OpenFeign**: `@FeignClient(fallback = UserServiceClientFallback.class)` 설정으로 Fallback 클래스 지정
2. **Circuit Breaker 트리거**: 네트워크 장애, 타임아웃, 또는 연속된 실패로 Circuit Open 상태가 되면 자동으로 Fallback 메서드 실행
3. **NPE 방지**: Fallback에서 null을 반환하면 상위 레이어에서 NPE 발생 위험 → 대신 명시적인 CustomException 발생
4. **장애 격리**: user-service 장애가 알림 서비스 전체로 전파되지 않고, Fallback에서 즉시 처리되어 응답 반환

**왜 null 대신 예외를 던지나?**
- null 반환 시: 상위 Service 레이어에서 `userResponse.getData()` 호출 → NPE 발생
- 예외 발생 시: GlobalExceptionHandler에서 일관되게 처리 → 사용자 친화적 메시지 반환

#### 2-2. FeignException 전용 핸들러
```java
@ExceptionHandler(FeignException.class)
protected ResponseEntity<ApiResponse<Void>> handleFeignException(FeignException e) {
    int status = e.status();

    String message = switch (status) {
        case 400 -> "외부 서비스 요청 형식이 올바르지 않습니다. (user-service 연동 실패)";
        case 401 -> "외부 서비스 인증에 실패했습니다. (user-service 연동 실패)";
        case 403 -> "요청한 리소스에 접근할 수 없습니다. (user-service 연동 실패)";
        case 404 -> "요청한 리소스를 찾을 수 없습니다. (user-service 연동 실패)";
        case 500 -> "외부 서비스에서 오류가 발생했습니다. (user-service 연동 실패)";
        case 503 -> "외부 서비스를 일시적으로 사용할 수 없습니다. (user-service 연동 실패)";
        default -> "외부 서비스 연동 중 오류가 발생했습니다.";
    };

    HttpStatus httpStatus = HttpStatus.valueOf(status);
    ApiResponse<Void> response = new ApiResponse<>(false, status, message, null);
    return new ResponseEntity<>(response, httpStatus);
}
```

**적용 시점**: HTTP 에러 응답 (400, 403, 404, 500 등)

**동작 원리**:
1. **FeignException 발생**: user-service가 HTTP 에러 응답(403, 404, 500 등)을 반환하면 Feign이 FeignException 발생
2. **상태 코드 추출**: `e.status()`로 원본 HTTP 상태 코드 추출 (예: 403, 404, 500)
3. **Java 17 switch expression**: 상태 코드별로 사용자 친화적인 한글 메시지 매핑
   ```java
   String message = switch (status) {
       case 403 -> "요청한 리소스에 접근할 수 없습니다. (user-service 연동 실패)";
       case 404 -> "요청한 리소스를 찾을 수 없습니다. (user-service 연동 실패)";
       ...
   };
   ```
4. **HTTP 상태 코드 일치**: `ResponseEntity`에 원본 상태 코드 그대로 설정 (403 → 403, 404 → 404)
   - 기존 문제: user-service에서 403 에러 → notification-service에서 500으로 변환 → 클라이언트 혼란
   - 개선 후: user-service에서 403 에러 → notification-service에서 403 그대로 반환 → 클라이언트가 정확한 에러 타입 파악

**왜 상태 코드를 일치시켜야 하나?**
- 클라이언트가 HTTP 상태 코드로 에러 타입을 판단 (403: 권한 없음, 404: 리소스 없음, 500: 서버 오류)
- 상태 코드가 일치하지 않으면 클라이언트의 재시도 로직이 잘못 동작할 수 있음
- RESTful API 원칙 준수: 에러 상태를 정확히 전달

**해결 방법 요약**:
- Fallback: 네트워크 장애 시 Circuit Breaker 적용, null 반환 대신 예외 발생
- ExceptionHandler: HTTP 에러를 한글 메시지로 변환, 상태 코드 일치 (403 → 403)

**해결 효과**:
- 외부 서비스 장애 격리, NPE 위험 제거
- 정확한 HTTP 코드로 클라이언트 에러 핸들링 가능
- 사용자 친화적 메시지로 디버깅 용이

**두 패턴의 관계**:
- Fallback: **Circuit Breaker 상황** (타임아웃, 네트워크 장애) → 서비스 불가 상태
- ExceptionHandler: **HTTP 에러 응답** (400, 403, 404, 500) → 서비스 응답은 받았지만 에러
- 상호 보완적 관계로 MSA 통신 안정성 확보

**실행 흐름 예시**:
```
[정상 응답]
NotificationController → UserServiceClient → user-service
                                            ← 200 OK + UserResponse

[HTTP 에러]
NotificationController → UserServiceClient → user-service
                                            ← 403 Forbidden
                       ← FeignException (status=403)
                       → FeignExceptionHandler
                       ← ResponseEntity(status=403, message="접근 불가")

[네트워크 장애]
NotificationController → UserServiceClient → (타임아웃/연결 실패)
                       → UserServiceClientFallback
                       ← CustomException("User service is temporarily unavailable")
                       → GlobalExceptionHandler
                       ← ResponseEntity(status=500, message="외부 서비스 일시 불가")
```

**관련 파일**:
- `UserServiceClient.java`, `UserServiceClientFallback.java` (Issue #76)
- `NotificationExceptionHandler.java` (PR #109)
- docs/07-issues/issue-076-notification-risk-refactoring.md
- docs/08-pull-requests/pr-109-notification-swagger-fix.md

---

## 📊 핵심 성과 (5초)

### 구현 완료
- ✅ **AI 기반 출발 시한 계산** (Google Gemini API)
- ✅ **Kafka 이벤트 기반 알림** (멱등성 보장)
- ✅ **외부 API 장애 격리** (트랜잭션 분리)
- ✅ **MSA 통신 안정성** (Circuit Breaker + FeignException 처리)
- ✅ **전체 테스트 통과** (21/21)

### 세부 성과
- REST API: 9개 엔드포인트
- Kafka Consumer: 2개 (order.created, delivery.status.changed)
- 외부 API 통합: 3개 (Slack, Gemini, Naver Maps-미구현)
- 단위 테스트: 5/5
- 통합 테스트: 4/4
- REST API 테스트: 10/10
- Kafka 테스트: 4/4

---


### 예상 질문 & 답변

**Q1. Gemini API를 선택한 이유는? (ChatGPT 대신)**
> A: 세 가지 이유로 Gemini를 선택했습니다.
> 첫째, **비용**: ChatGPT는 월 $20 유료 플랜 또는 API 토큰당 과금($0.0015/1K tokens)이지만, Gemini gemini-2.5-flash-lite는 무료 티어로 60 req/min을 제공합니다.
> 둘째, **성능**: 프롬프트 기반 시간 계산에 필요한 응답 속도와 정확도가 ChatGPT와 유사합니다.
> 셋째, **개발 편의성**: 테스트/개발 환경에서 비용 부담 없이 충분한 API 호출이 가능했습니다.
>
> 📂 참조: `GeminiClientWrapper.java:68` (비용 ZERO), `issue-013-external-api-client.md:508-509`

**Q2. 멱등성 보장이 왜 필요한가?**
> A: Kafka는 최소 한 번 전송(At-Least-Once)을 보장하기 때문에 네트워크 장애 시 동일 메시지가 여러 번 올 수 있습니다.
> eventId로 중복을 체크하지 않으면 같은 알림이 여러 번 발송될 수 있습니다.
> DB UNIQUE 제약조건은 최종 방어선이고, 애플리케이션의 existsByEventId() 체크는 불필요한 처리를 조기에 차단하여 성능을 개선합니다.
>
> 📂 참조: `OrderCreatedConsumer.java:32-37`, `Notification.java` (event_id UNIQUE)

**Q3. 트랜잭션 분리의 장단점은?**
> A: **장점**: DB 저장과 Slack 발송을 분리하여 Slack 실패 시에도 에러 메시지가 DB에 유실되지 않고 FAILED 상태로 저장됩니다.
> **단점**: DB는 저장됐는데 Slack 전송이 실패하면 사용자가 알림을 못 받을 수 있습니다.
> 하지만 FAILED 상태로 남아있어서 추후 재발송이 가능합니다. REQUIRES_NEW 전파 속성으로 별도 트랜잭션을 열어 데이터 일관성을 보장합니다.
>
> 📂 참조: `NotificationService.java:64-111`, `issue-076-notification-risk-refactoring.md:42-56`

**Q4. FeignClient Fallback과 ExceptionHandler의 차이는?**
> A: Fallback은 네트워크 장애/타임아웃 등 Circuit Breaker 상황에서 동작하고, ExceptionHandler는 HTTP 에러 응답(403, 404, 500)을 처리합니다.
> 두 패턴이 상호 보완적으로 MSA 통신 안정성을 확보합니다.
> Fallback에서는 null 대신 예외를 던져 NPE 위험을 제거하고, ExceptionHandler는 사용자 친화적인 한글 메시지로 변환합니다.
>
> 📂 참조: `UserServiceClient.java`, `NotificationExceptionHandler.java:19-31`, `issue-076-notification-risk-refactoring.md`

**Q5. Kafka Consumer에서 멱등성 체크 위치는 왜 맨 앞인가?**
> A: 멱등성 체크를 Consumer 맨 앞에 배치하면 중복 이벤트를 조기에 차단하여 불필요한 Gemini API, Slack API 호출을 방지합니다.
> 이미 처리된 이벤트는 로그만 남기고 즉시 return하므로 비용과 성능 모두 개선됩니다.
>
> 📂 참조: `OrderCreatedConsumer.java:32-37`, `DeliveryStatusChangedConsumer.java`

**Q6. ExternalApiLog에 messageId를 연계한 이유는?**
> A: 알림(Notification)과 외부 API 호출(ExternalApiLog)을 1:N 관계로 연결하여 추적성을 강화하기 위함입니다.
> 하나의 알림 발송 시 Gemini API 1회 + Slack API 1회 총 2번의 외부 호출이 발생하는데, messageId로 연결하면 어떤 알림에서 어떤 API를 호출했고 각각 성공/실패 여부를 추적할 수 있습니다.
> 장애 발생 시 디버깅과 비용 분석에 유용합니다.
>
> 📂 참조: `NotificationService.java:85,256`, `issue-076-notification-risk-refactoring.md:44-48`

**Q7. Slack 실패 시 HTTP 500을 반환하는 이유는?**
> A: 초기에는 Slack 실패 시에도 200 OK를 반환하고 status: FAILED로만 표시했으나, Issue #76에서 개선했습니다.
> Slack 전송 실패는 알림 서비스의 핵심 기능 실패이므로 CustomException을 발생시켜 500 Internal Server Error를 반환합니다.
> DB에는 FAILED 상태로 저장되어 이력이 유지되고, 클라이언트는 명확한 에러 응답을 받아 재시도 로직을 구현할 수 있습니다.
>
> 📂 참조: `NotificationService.java:99-110`, `issue-076-notification-risk-refactoring.md:34-40`

**Q8. lenient Mock 패턴을 사용한 이유는?**
> A: Mockito의 strict stubbing 모드에서 일부 테스트에서만 사용되는 stubbing이 UnnecessaryStubbingException을 발생시킵니다.
> Entity의 일부 getter는 특정 테스트에서만 사용되므로, lenient()를 메서드별로 적용하면 필요한 부분만 완화하고 코드 리뷰 시 의도를 명확히 할 수 있습니다.
> 클래스 레벨 설정은 모든 Mock에 적용되어 엄격성이 저하되므로 권장하지 않습니다.
>
> 📂 참조: `NotificationServiceTest.java:190-220`, `issue-076-notification-risk-refactoring.md:172-286`

**Q9. Resilience4j Retry 전략은?**
> A: Slack은 3회 재시도(지수 백오프 1초 * 2^n), Gemini는 2회 재시도(지수 백오프 2초 * 2^n)로 설정했습니다.
> Slack은 메시지 전송 핵심 기능이라 재시도를 많이 하고, Gemini는 AI 응답이 느릴 수 있어 백오프 간격을 더 길게 설정했습니다.
> 재시도 중에도 ExternalApiLog에 모든 시도가 기록되어 장애 분석이 가능합니다.
>
> 📂 참조: `ExternalApiConfig.java`, `issue-013-external-api-client.md:20,26`

**Q10. Slack 전송 실패 시 재발송은 어떻게 하나?**
> A: 세 가지 방법이 있습니다.
> 첫째, DB에서 FAILED 상태인 알림을 조회하여 배치 작업으로 재발송.
> 둘째, Issue #84에서 추가한 배송 상태 알림 REST API로 수동 재발송.
> 셋째, Kafka 메시지를 재발행하여 Consumer가 다시 처리 (단, eventId가 다르면 멱등성 체크 통과).
> DB에 FAILED 상태와 에러 메시지가 남아있어 재발송 시 원인 분석이 가능합니다.
>
> 📂 참조: `NotificationService.java:105-110`, `DeliveryStatusNotificationRequest.java`, `issue-084-delivery-status-rest-api.md`

---

## 📚 참고 문서

### 내부 문서
- [Issue #35: Kafka Consumer 구현](../07-issues/issue-035-notification-kafka-consumer.md)
- [Issue #76: 리스크 개선 (트랜잭션 분리)](../07-issues/issue-076-notification-risk-refactoring.md)
- [Issue #109: Swagger 테스트 수정](../07-issues/issue-109-notification-swagger-fix.md)
- [PR #109: FeignException 처리](../08-pull-requests/pr-109-notification-swagger-fix.md)
- [Notification Service API 명세](../05-api-specs/notification-service-api.md)

### 소스 코드 위치
```
notification-service/src/main/java/com/oneforlogis/notification/
├── application/service/NotificationService.java          # 트랜잭션 분리
├── infrastructure/kafka/OrderCreatedConsumer.java        # Kafka Consumer
├── infrastructure/client/GeminiClientWrapper.java        # Gemini API 호출
└── infrastructure/client/SlackClientWrapper.java         # Slack API 호출
```

### 테스트 위치
```
notification-service/src/test/java/com/oneforlogis/notification/
├── application/service/NotificationServiceTest.java      # 단위 테스트
└── integration/
    ├── OrderCreatedConsumerIT.java                       # Kafka 통합 테스트
    └── DeliveryStatusChangedConsumerIT.java             # Kafka 통합 테스트
```

---

**마지막 업데이트**: 2024-11-13
**문서 버전**: 1.0
**상태**: 발표 준비 완료
