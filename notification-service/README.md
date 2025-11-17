# notification-service

14logis 물류 시스템의 알림 및 AI 통합 서비스입니다.

## 개요

- **포트**: 8700
- **데이터베이스 스키마**: notification_db
- **서비스 검색**: Eureka Server에 등록
- **담당 도메인**: AI 기반 배송 시한 계산, Slack 알림, Kafka 이벤트 처리

## 주요 기능

- **주문 알림**: Gemini AI 기반 최종 발송 시한 계산 및 Slack 메시지 발송
- **배송 상태 알림**: Kafka Event + REST API로 배송 상태 변경 알림 (Issue #84, #35)
- **수동 메시지 발송**: 사용자 직접 Slack 메시지 발송 (발신자 정보 스냅샷 저장)
- **API 로그 및 통계**: 외부 API 호출 모니터링 (Slack, Gemini, Naver Maps)
- **일일 경로 최적화** (Challenge, 미구현): Gemini TSP + Naver Maps API 기반 최적 경로 생성

## 기술 스택

- Spring Boot 3.5.7
- Spring Data JPA (DDD Repository 패턴)
- PostgreSQL 17 (JSONB 지원)
- Spring Cloud Eureka Client
- Spring Cloud OpenFeign (user-service 통신)
- Spring Kafka 3.2.2
- Apache Kafka 3.7.1 (Confluent Platform 7.5.0)
- Spring WebFlux (WebClient - 외부 API 호출)
- Resilience4j (Retry with Exponential Backoff)
- common-lib (SecurityConfigBase, BaseEntity, ApiResponse)
- Lombok

## 외부 API

- **Slack API**: chat.postMessage 메시지 발송
- **Google Gemini API**: 최종 발송 시한 계산, 경로 최적화 (Free tier: 60 req/min, gemini-2.5-flash-lite)
- **Naver Maps API**: 경유지 포함 경로 계산 (Challenge 기능, 미구현)

## 데이터베이스 테이블

- `p_notifications`: 알림 메시지 이력 (발신자/수신자 스냅샷 포함, 20개 필드)
  - 멱등성 보장: `event_id` UNIQUE 제약조건
  - MessageType: ORDER_NOTIFICATION, DELIVERY_STATUS_UPDATE, MANUAL, DAILY_ROUTE
  - MessageStatus: PENDING, SENT, FAILED
- `p_external_api_logs`: 외부 API 호출 로그 (13개 필드, JSONB 저장)
  - ApiProvider: SLACK, GEMINI, NAVER_MAPS
  - 성공/실패, 응답시간, 비용 추적
- `p_company_delivery_routes`: 일일 경로 최적화 결과 (Challenge 기능, 미구현)

## 패키지 구조 (DDD)

```
com.oneforlogis.notification/
├── presentation/          # REST API 엔드포인트, Request/Response DTOs
│   ├── controller/        # NotificationController
│   ├── request/          # OrderNotificationRequest, ManualNotificationRequest, DeliveryStatusNotificationRequest
│   ├── response/         # NotificationResponse, ExternalApiLogResponse, ApiStatisticsResponse
│   └── advice/           # NotificationExceptionHandler (FeignException 처리)
├── application/          # 유스케이스 오케스트레이션
│   ├── service/          # NotificationService, ExternalApiLogService
│   └── event/            # OrderCreatedEvent, DeliveryStatusChangedEvent (Kafka DTOs)
├── domain/               # 비즈니스 로직, 엔티티
│   ├── model/            # Notification, ExternalApiLog (엔티티)
│   ├── repository/       # Repository 인터페이스 (도메인 레이어)
│   └── exception/        # NotificationException
├── infrastructure/       # DB, 외부 API, 설정
│   ├── persistence/      # JpaRepository, RepositoryImpl
│   ├── client/           # Slack, Gemini, User FeignClient
│   ├── kafka/            # OrderCreatedConsumer, DeliveryStatusChangedConsumer
│   └── config/           # KafkaConsumerConfig, TopicProperties, ExternalApiConfig
└── global/               # 공통 유틸리티
    ├── config/           # SecurityConfig (common-lib 상속)
    └── util/             # AuthContextUtil
```

## 환경 변수 설정

`.env` 파일에 다음 환경 변수를 설정해야 합니다:

```properties
# 서비스 설정
NOTIFICATION_SERVICE_PORT=8700
EUREKA_SERVER_URL=http://localhost:8761/eureka

# 데이터베이스
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
NOTIFICATION_DB=oneforlogis_notification
POSTGRES_USER=root
POSTGRES_PASSWORD=your_password

# Kafka 설정
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
ORDER_CREATED_TOPIC=order.created
DELIVERY_STATUS_CHANGED_TOPIC=delivery.status.changed

# 외부 API 키
SLACK_BOT_TOKEN=xoxb-your-slack-bot-token
GEMINI_API_KEY=AIza-your-gemini-api-key

# JWT 설정 (user-service 연동)
JWT_SECRET_KEY=your-jwt-secret-key
```

## REST API 엔드포인트

총 **10개의 REST API**가 구현되어 있습니다:

### 알림 발송 API (3개)
1. **POST** `/api/v1/notifications/order` - 주문 알림 발송 (내부 서비스용)
2. **POST** `/api/v1/notifications/delivery-status` - 배송 상태 알림 발송 (ALL)
3. **POST** `/api/v1/notifications/manual` - 수동 메시지 발송 (ALL)

### 알림 조회 API (3개)
4. **GET** `/api/v1/notifications/{id}` - 알림 단건 조회 (ALL)
5. **GET** `/api/v1/notifications` - 알림 목록 조회 (MASTER, 페이징)
6. **GET** `/api/v1/notifications/search` - 알림 필터링 조회 (MASTER, 페이징)

### 외부 API 로그 조회 API (4개)
7. **GET** `/api/v1/notifications/api-logs` - API 로그 전체 조회 (MASTER, 페이징)
8. **GET** `/api/v1/notifications/api-logs/provider/{provider}` - Provider별 로그 조회 (MASTER)
9. **GET** `/api/v1/notifications/api-logs/message/{messageId}` - 메시지별 로그 조회 (MASTER)
10. **GET** `/api/v1/notifications/api-logs/stats` - API 통계 조회 (MASTER)

자세한 API 명세는 [notification-service-api.md](../docs/05-api-specs/notification-service-api.md)를 참조하세요.

## 빌드 및 실행

```bash
# 빌드
./gradlew :notification-service:build

# 로컬 실행
./gradlew :notification-service:bootRun

# 테스트 실행
./gradlew :notification-service:test
```

## Docker 환경

```bash
# 이미지 빌드
./gradlew :notification-service:build -x test
docker-compose -f docker-compose-team.yml build notification-service

# Docker Compose로 실행 (전체 인프라 포함)
docker-compose -f docker-compose-team.yml up -d

# notification-service만 재시작
docker-compose -f docker-compose-team.yml restart notification-service

# 헬스 체크
curl http://localhost:8700/actuator/health
# Expected: {"status":"UP"}

# 데이터베이스 테이블 확인
docker exec postgres-ofl psql -U root -d oneforlogis_notification -c "\dt"
# Expected: p_notifications, p_external_api_logs

# Eureka 등록 확인
curl http://localhost:8761/eureka/apps/NOTIFICATION-SERVICE

# 로그 확인
docker-compose -f docker-compose-team.yml logs -f notification-service
```

## 개발 현황

### ✅ 완료된 기능

**전체 진행률**: 90% (REST API, Kafka Consumer, 리스크 개선 완료 / Challenge 기능 미구현)

**Issue #11** - 초기 설정 (2025-11-05)
- Spring Boot 애플리케이션 설정 (포트: 8700)
- Eureka 클라이언트 등록
- DDD 패키지 구조 생성
- Dockerfile 작성

**Issue #12** - 도메인 엔티티 및 리포지토리 (2025-11-05)
- 도메인 엔티티: `Notification`, `ExternalApiLog` (BaseEntity 상속)
- DDD Repository 패턴 (domain 인터페이스 + infrastructure 구현체)
- JPA 설정: Auditing, Soft Delete (`@SQLRestriction`)
- 테스트 커버리지: 26개 테스트 (15 Notification + 11 ExternalApiLog) - 100% 통과
- Docker 통합: PostgreSQL 17 with JSONB 지원

**Issue #33** - 공통 설정 반영 (2025-11-05)
- SecurityConfig (common-lib SecurityConfigBase 상속)
- `@Import` 어노테이션으로 common-lib Config 등록
- Spring Security 의존성 추가

**Issue #13** - 외부 API 클라이언트 (2025-11-06)
- Slack API 클라이언트 (WebClient + Resilience4j, 재시도 3회)
- Gemini API 클라이언트 (WebClient + Resilience4j, 재시도 2회, gemini-2.5-flash-lite 모델)
- ApiLogDomainService (자동 로깅, 민감 데이터 마스킹)
- Wrapper 패턴 (SlackClientWrapper, GeminiClientWrapper)
- WebClient 의존성 주입 리팩토링 (테스트 용이성)
- 단위 테스트: MockWebServer 사용 (6개)
- 통합 테스트: 실제 API 연동 (3개)
- 테스트 결과: 35/35 통과

**Issue #14** - REST API 구현 (2025-11-07)
- UserServiceClient (user-service FeignClient 통신)
- NotificationController (7개 엔드포인트)
- NotificationService (비즈니스 로직)
  - `sendOrderNotification()`: Gemini AI + Slack 통합
  - `sendManualNotification()`: 사용자 정보 스냅샷 패턴
- ExternalApiLogService (API 로그 관리)
- Request/Response DTOs (record 패턴)
- 테스트: 44/44 통과
- Slack 실제 채널 메시지 발송 성공 (C09QY22AMEE)

**Issue #16** - 조회 및 통계 API (2025-11-10)
- 알림 필터링 조회 API (`GET /search`)
  - 다중 조건 필터링 (senderUsername, recipientSlackId, messageType, status)
  - 팀 표준 페이징 패턴 (size: 10/30/50, sortBy 화이트리스트)
- API 통계 조회 API (`GET /api-logs/stats`)
  - Provider별 통계 집계 (성공률, 응답시간, 총 비용)
- `createPageable()` 헬퍼 메서드 (SQL Injection 방지)
- 테스트: 10/10 통과

**Issue #35** - Kafka 이벤트 소비자 (2025-11-11, PR #83 Merged)
- **Kafka Consumer 구현** (2개)
  - `OrderCreatedConsumer`: order.created → Gemini AI → Slack 알림
  - `DeliveryStatusChangedConsumer`: delivery.status.changed → Slack 알림
  - 멱등성 처리 (event_id 기반 중복 검증, DB unique constraint)
  - ErrorHandlingDeserializer + JsonDeserializer 조합
- **Kafka Configuration**
  - KafkaConsumerConfig: 토픽별 별도 ContainerFactory
  - TopicProperties: @ConfigurationProperties로 토픽 관리
- **Event DTOs** (record 패턴)
  - OrderCreatedEvent, DeliveryStatusChangedEvent
- **DB Schema 수정**
  - MessageType enum: DELIVERY_STATUS_UPDATE 추가
- **통합 테스트**: 4/4 통과 (멱등성 검증 성공)

**Issue #76** - 리스크 개선 (2025-11-12)
- **우선순위 1 (Critical)**
  - 통합 테스트 분리 (Mock 설정)
  - user-service NPE 위험 제거 (FeignClient Fallback)
  - Slack 실패 시 HTTP 500 응답 반환
- **우선순위 2 (High)**
  - Gemini messageId 연계
  - 트랜잭션 분리 (DB 저장 + Slack 발송)
  - NotificationService 단위 테스트: 5/5 통과
  - NotificationException 도메인 예외 생성
- **테스트 결과**: 21/21 통과 (단위 5, 통합 4, Kafka 4, REST API 10)

**Issue #84** - 배송 상태 알림 REST API (2025-11-13, PR #105 Merged)
- **REST API 추가**
  - `POST /api/v1/notifications/delivery-status`: 배송 상태 변경 알림 발송
  - DeliveryStatusNotificationRequest DTO (6 필드)
  - `sendDeliveryStatusNotification()` 메서드
- **기능**
  - Kafka Event + REST API 이중 지원
  - 재발송 기능 (Slack 실패 시)
  - 장애 대응 (Kafka 장애 시 대체 수단)
- **테스트 결과**: Controller 2/2, REST API 10/10 통과

**Issue #109** - Swagger 테스트 & FeignException 처리 (2025-11-13, Ready for PR)
- **Swagger 테스트 수정**
  - Slack ID 통일 (C09QY22AMEE)
  - NotificationControllerTest 26개 케이스 업데이트
- **FeignException 처리**
  - NotificationExceptionHandler에 FeignException 핸들러 추가
  - HTTP 상태 코드 매핑 (400, 401, 403, 404, 500, 502, 503)
- **user-service 연동 개선**
  - UserServiceClient: `getMyInfo()` API 사용으로 변경
  - FeignClient 응답 null 체크 강화

### 🚧 진행 예정

- **Issue #85**: deletedBy 사용자 정보 자동 수집 (예상 0.5일)
- **Issue #86**: Kafka Consumer 보안 강화 - SASL/SSL (CVSS 7.5, 예상 1일)
- **Issue #87-88**: 성능 개선 (Gemini 캐싱, DLQ, 예상 1.5일)
- **Issue #36**: 일일 경로 최적화 스케줄러 (Challenge, 예상 3-4일)

---

## 테스트

### 테스트 실행

```bash
# 전체 테스트 실행
./gradlew :notification-service:test

# 특정 테스트 클래스 실행
./gradlew :notification-service:test --tests NotificationServiceTest

# 통합 테스트 실행
./gradlew :notification-service:test --tests "*IT"
```

### 테스트 커버리지

- **Repository 테스트**: 26개 (Notification 15개 + ExternalApiLog 11개)
- **API 클라이언트 단위 테스트**: 6개 (MockWebServer 사용)
- **API 클라이언트 통합 테스트**: 3개 (실제 API 연동)
- **Service 단위 테스트**: 5개 (NotificationService)
- **Controller 테스트**: 26개 (NotificationControllerTest)
- **Kafka Consumer 통합 테스트**: 4개 (멱등성 검증)
- **총 테스트 수**: 70+ 개
- **테스트 성공률**: 100%

### Docker 환경 테스트

```bash
# Kafka Consumer 테스트
cd notification-service/scripts
./test-kafka-consumer.sh

# REST API 테스트
./test-notification-api.sh
```

---

## 핵심 비즈니스 로직

### 1. 주문 알림 발송 프로세스

```
order-service → POST /api/v1/notifications/order
                     ↓
            NotificationService.sendOrderNotification()
                     ↓
            ┌────────┴────────┐
            ↓                 ↓
     Gemini API          Slack API
  (최종 발송 시한 계산)   (메시지 발송)
            ↓                 ↓
            └────────┬────────┘
                     ↓
              DB 저장 (트랜잭션)
         p_notifications + p_external_api_logs
```

**특징**:
- Gemini AI 기반 최종 발송 시한 계산 (경로 정보 + 배송 요청 사항 분석)
- 트랜잭션 분리 (DB 저장 → Slack 발송)로 에러 메시지 유실 방지
- 외부 API 호출 자동 로깅 (성공/실패, 응답시간, 비용)

### 2. 배송 상태 변경 알림

**방법 1: Kafka Event (비동기)**

```
delivery-service → Kafka: delivery.status.changed
                        ↓
           DeliveryStatusChangedConsumer
                        ↓
              멱등성 검증 (event_id)
                        ↓
                   Slack API
                        ↓
                   DB 저장
```

**방법 2: REST API (동기)**

```
Client → POST /api/v1/notifications/delivery-status
              ↓
   NotificationService.sendDeliveryStatusNotification()
              ↓
          Slack API
              ↓
          DB 저장
```

**차이점**:
- **Kafka**: eventId 저장 (멱등성 보장, 중복 방지)
- **REST**: eventId = null (중복 허용, 재발송 가능)

**사용 시나리오**:
- **Kafka**: 정상적인 배송 상태 변경 (자동 발행)
- **REST**: Slack 발송 실패 시 수동 재전송, 테스트/디버깅, Kafka 장애 시 대체 수단

### 3. 발신자 정보 스냅샷 패턴

수동 메시지 발송 시 사용자 정보를 메시지 발송 시점에 저장하여, 이후 사용자 정보 변경/삭제 시에도 메시지 이력을 보존합니다.

- **저장 항목**: senderUsername, senderSlackId, senderName
- **데이터 소스**: user-service FeignClient (`getMyInfo()` API)
- **SYSTEM 메시지**: 발신자 필드 모두 null

### 4. 외부 API 호출 로깅

모든 외부 API 호출은 자동으로 로깅됩니다 (AOP 패턴 미사용, Wrapper 패턴 사용).

- **저장 항목**: 요청/응답 데이터 (JSONB), HTTP 상태, 성공 여부, 응답시간, 비용
- **용도**: API 성능 모니터링, 비용 추적, 장애 분석
- **통계 API**: Provider별 성공률, 평균 응답시간, 총 비용 조회

---

## 주요 설계 패턴

### DDD (Domain-Driven Design)

- **Domain Layer**: 엔티티, 리포지토리 인터페이스, 도메인 예외
- **Application Layer**: 유스케이스 오케스트레이션, 서비스
- **Infrastructure Layer**: 리포지토리 구현, FeignClient, Kafka Consumer
- **Presentation Layer**: Controller, Request/Response DTOs

### Wrapper 패턴 (외부 API 클라이언트)

```java
SlackClientWrapper → SlackApiClient (WebClient)
                    → ExternalApiLogService (자동 로깅)
```

**장점**:
- 외부 API 호출 로직과 로깅 로직 분리
- 예외 처리 캡슐화
- 테스트 용이성

### 스냅샷 패턴 (발신자 정보)

메시지 발송 시점의 사용자 정보를 DB에 저장하여, 이후 사용자 정보 변경/삭제에도 메시지 이력 보존.

### 멱등성 패턴 (Kafka Consumer)

```sql
UNIQUE CONSTRAINT ON (event_id)
```

- DB 레벨 중복 방지 + 애플리케이션 레벨 `existsByEventId()` 체크
- Kafka 메시지 재전송 시에도 중복 처리 방지

---

## 외부 서비스 연동

### user-service

- **FeignClient**: `UserServiceClient`
- **엔드포인트**: `GET /api/v1/users/my-info` (마이페이지 API)
- **용도**: 수동 메시지 발송 시 발신자 정보 조회
- **Fallback**: `UserServiceClientFallback` (NPE 방지)

### order-service

- **연동 방식**: REST API 호출 (order-service → notification-service)
- **엔드포인트**: `POST /api/v1/notifications/order`
- **데이터**: 주문 정보, 배송 경로, 허브 관리자 정보

### delivery-service

- **연동 방식**: Kafka Event
- **토픽**: `delivery.status.changed`
- **데이터**: 배송 ID, 주문 ID, 이전/현재 상태, 수신자 정보

---

## 모니터링 및 운영

### Swagger UI

- **URL**: http://localhost:8700/swagger-ui.html
- **인증**: X-User-Id, X-User-Name, X-User-Role 헤더 필요
- **API 테스트**: 모든 엔드포인트 테스트 가능

### Actuator Endpoints

```bash
# 헬스 체크
curl http://localhost:8700/actuator/health

# 정보 확인
curl http://localhost:8700/actuator/info
```

### Kafka Consumer 모니터링

```bash
# Consumer Group 상태 확인
docker exec -it kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group notification-service-group \
  --describe

# 토픽 메시지 확인
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order.created \
  --from-beginning
```

### 로그 레벨 설정

```yaml
logging:
  level:
    com.oneforlogis.notification: DEBUG
    org.springframework.kafka: INFO
    org.springframework.cloud.openfeign: DEBUG
```

---

## 참고 문서

- [API 명세서](../docs/05-api-specs/notification-service-api.md)
- [데이터베이스 스키마](../docs/02-development/database-schema.md)
- [비즈니스 규칙](../docs/02-development/business-rules.md)
- [테스트 가이드](../docs/04-testing/testing-guide.md)
- [트러블슈팅](../docs/04-testing/troubleshooting.md)
- [완료 작업 로그](../docs/06-work-log/completed-work.md)

---

## 기여

notification-service는 14logis 프로젝트의 일부입니다. 기여 방법은 프로젝트 루트의 [CLAUDE.md](../CLAUDE.md)를 참조하세요.

## 라이선스

이 프로젝트는 교육 목적으로만 사용됩니다.
