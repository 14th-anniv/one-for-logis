# notification-service

Notification and AI integration service for 14logis logistics system.

## Overview

- **Port**: 8700
- **Database Schema**: notification_db
- **Service Discovery**: Registered with Eureka Server

## Features

- **Order Notifications**: AI-based departure time calculation and Slack messaging
- **Manual Messages**: User-triggered Slack messages with sender snapshot
- **API Logging**: External API call monitoring (Slack, Gemini, Naver Maps)
- **Daily Route Optimization** (Challenge): Gemini TSP + Naver Maps routing at 06:00

## Tech Stack

- Spring Boot 3.5.7
- Spring Data JPA
- PostgreSQL
- Spring Cloud Eureka Client
- Spring Cloud OpenFeign
- Spring Kafka 3.2.2
- Apache Kafka 3.7.1 (Confluent Platform 7.5.0)
- Spring WebFlux (WebClient)
- Resilience4j (Retry with Exponential Backoff)
- Lombok

## External APIs

- **Slack API**: chat.postMessage for notifications
- **Google Gemini API**: Departure time calculation, route optimization (Free tier: 60 req/min)
- **Naver Maps API**: Route calculation with waypoints

## Database Tables

- `p_notifications`: Message history with sender/recipient snapshots (20 fields)
- `p_external_api_logs`: External API call monitoring (13 fields)
- `p_company_delivery_routes`: Daily optimized delivery routes (Challenge, not implemented)

## Package Structure (DDD)

```
com.oneforlogis.notification/
├── presentation/       - REST API endpoints, DTOs
├── application/        - Use case orchestration
├── domain/            - Business logic, entities
├── infrastructure/    - DB, external APIs, config
└── global/            - Common utilities
```

## Environment Variables

```properties
# Service Configuration
NOTIFICATION_SERVICE_PORT=8700
EUREKA_SERVER_URL=http://localhost:8761/eureka

# Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
NOTIFICATION_DB=oneforlogis_notification
POSTGRES_USER=root
POSTGRES_PASSWORD=your_password

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
ORDER_CREATED_TOPIC=order.created
DELIVERY_STATUS_CHANGED_TOPIC=delivery.status.changed

# External API Keys
SLACK_BOT_TOKEN=xoxb-your-slack-bot-token
GEMINI_API_KEY=AIza-your-gemini-api-key
```

## Build & Run

```bash
# Build
./gradlew :notification-service:build

# Run
./gradlew :notification-service:bootRun
```

## Docker

```bash
# Build image
./gradlew :notification-service:build -x test
docker-compose build notification-service

# Run with docker-compose
docker-compose up -d notification-service

# Health check
curl http://localhost:8700/actuator/health
# Expected: {"status":"UP"}

# Verify database tables
docker exec oneforlogis-postgres psql -U root -d oneforlogis_notification -c "\dt"
# Expected: p_notifications, p_external_api_logs

# Check Eureka registration
curl http://localhost:8761/eureka/apps/NOTIFICATION-SERVICE
```

## Development Status

### ✅ Completed

**Issue #11** - 초기 설정 (2025-11-05)
- Spring Boot application setup (Port 8700)
- Eureka client registration
- DDD package structure
- Dockerfile

**Issue #12** - DB Entity & Repository (2025-11-05)
- Domain entities: `Notification`, `ExternalApiLog`
- Repository layer: Domain interfaces + Infrastructure implementations
- JPA configurations: Auditing, soft delete with `@SQLRestriction`
- Test coverage: 26 tests (15 Notification + 11 ExternalApiLog) - 100% pass
- Docker integration: PostgreSQL 17 with JSONB support

**Issue #33** - 공통 설정 반영 (2025-11-05)
- SecurityConfig (SecurityConfigBase 상속)
- @Import annotation for common-lib configs
- Spring Security dependency

**Issue #13** - 외부 API 클라이언트 (2025-11-06)
- Slack API client (WebClient + Resilience4j, 3 retry attempts with exponential backoff)
- Gemini API client (WebClient + Resilience4j, 2 retry attempts, gemini-2.5-flash-lite model)
- ApiLogDomainService (automatic logging with sensitive data masking)
- Client wrappers (SlackClientWrapper, GeminiClientWrapper - auto-logging + error handling)
- WebClient dependency injection refactoring (separate beans for testability)
- Unit tests with MockWebServer (6 tests - GeminiApiClientTest, SlackApiClientTest)
- Integration tests with real APIs (3 tests - GeminiApiKeyIntegrationTest, SlackApiAuthIntegrationTest)
- API key validation (Slack Bot Token, Gemini API Key)
- Test results: 35/35 passed (100% success rate)

**Issue #14** - REST API 구현 (2025-11-07)
- User FeignClient (user-service 통신)
- NotificationController (7 endpoints)
  - POST /order: 주문 알림 발송 (Internal API, No Auth)
  - POST /manual: 수동 메시지 발송 (Auth Required)
  - GET /{id}: 알림 단일 조회 (Auth Required)
  - GET /: 알림 목록 조회 (MASTER Only, Pageable)
  - GET /api-logs: 외부 API 로그 전체 조회 (MASTER Only, Pageable)
  - GET /api-logs/provider/{provider}: Provider별 로그 조회 (MASTER Only, Pageable)
  - GET /api-logs/message/{messageId}: 메시지별 로그 조회 (MASTER Only, Pageable)
- NotificationService (비즈니스 로직)
  - sendOrderNotification(): Gemini AI + Slack 통합
  - sendManualNotification(): 사용자 정보 스냅샷 패턴
  - Gemini AI 프롬프트 최적화 (200자 이내 근거, 간소화된 예시)
- ExternalApiLogService (API 로그 관리)
- Request/Response DTOs (record 패턴)
- SecurityConfig (common-lib 통합, @EnableMethodSecurity)
- Unit tests: NotificationControllerTest (8 tests)
- Docker cURL tests: test-notification-api.sh (8 tests)
- Test results: 44/44 passed (100% success rate)
- Slack 실제 채널 메시지 발송 성공 (C09QY22AMEE)

**Issue #16** - 조회 및 통계 API (2025-11-10)
- 알림 필터링 조회 API (GET /search)
  - 다중 조건 필터링 (senderUsername, recipientSlackId, messageType, status)
  - 팀 표준 페이징 패턴 (size 검증, sortBy 화이트리스트, boolean isAsc)
- API 통계 조회 API (GET /api-logs/stats)
  - Provider별 통계 집계 (SLACK, GEMINI, NAVER_MAPS)
  - Stream API 활용 (성공률, 평균/최소/최대 응답시간, 총 비용)
- ApiStatisticsResponse DTO (record, 정적 팩토리 메서드)
- createPageable() 헬퍼 메서드
  - Size 검증 (10, 30, 50만 허용)
  - Page 음수 보정
  - SortBy 화이트리스트 (SQL Injection 방지)
- Repository 페이징 메서드 추가 (ExternalApiLogRepository)
- Unit tests: 기존 4개 수정 + 신규 3개 추가 (총 10개)
- Docker cURL tests: 기존 8개 수정 + 신규 2개 추가 (총 10개)
- Test results: 10/10 passed (100% success rate)

**Issue #35** - Kafka 이벤트 소비자 (2025-11-11)
- **Kafka Consumer 구현** (2개)
  - OrderCreatedConsumer: order.created 토픽 → 주문 알림 발송
  - DeliveryStatusChangedConsumer: delivery.status.changed 토픽 → 배송 상태 업데이트 알림
  - @KafkaListener with custom ContainerFactory
  - 멱등성 처리 (event_id 기반 중복 검증, DB unique constraint)
  - ErrorHandlingDeserializer + JsonDeserializer 조합
  - Spring Kafka 3.2.2 with Kafka 3.7.1 (Confluent Platform 7.5.0)
- **Kafka Configuration**
  - application.yml: consumer group, deserializer, trusted packages
  - KafkaConsumerConfig: 토픽별 별도 ContainerFactory (OrderCreated, DeliveryStatusChanged)
  - TopicProperties: @ConfigurationProperties for topic names
  - ErrorHandlingDeserializer wrapper (JSON 파싱 에러 처리)
  - JsonDeserializer delegate with default types
- **Event DTOs** (record pattern, immutable)
  - OrderCreatedEvent (eventId, occurredAt, order)
  - DeliveryStatusChangedEvent (eventId, occurredAt, delivery)
  - OrderData (15 fields: orderId, ordererInfo, route, receiver, hubManager)
  - DeliveryData (5 fields: deliveryId, orderId, previousStatus, currentStatus, recipient)
  - RouteData, ReceiverData, HubManagerData
- **DB Schema 수정**
  - MessageType enum: DELIVERY_STATUS_UPDATE 추가
  - CHECK constraint 업데이트: p_notifications_message_type_check
  - PostgreSQL ALTER TABLE 실행 (oneforlogis_notification DB)
- **Docker Compose 통합**
  - Kafka + Zookeeper 추가 (docker-compose-team.yml)
  - Dual-port listener: localhost:9092 (external), kafka:29092 (internal)
  - Environment variables: KAFKA_BOOTSTRAP_SERVERS, topics
- **Integration Tests**
  - test-kafka-consumer.sh (4 scenarios: order event, order idempotency, delivery event, delivery idempotency)
  - End-to-end verification: Kafka → Consumer → Slack → DB
  - Test results: 4/4 passed (멱등성 검증 성공)
  - Real Slack channel integration (C09QY22AMEE)
- **Documentation**: docs/review/issue-35-notification-kafka-consumer.md

**Issue #76** - 리스크 개선 (2025-11-12) ✅ **완료**
- **Priority 1 (Critical)**
  - 통합 테스트 분리: OrderCreatedConsumerIT, DeliveryStatusChangedConsumerIT Mock 설정
  - user-service NPE 위험 제거: FeignClient Fallback 구현
  - Slack 실패 시 HTTP 응답 개선: 500 Internal Server Error 반환
- **Priority 2 (High)**
  - Gemini messageId 연계: generateContent()에 messageId 파라미터 추가
  - Slack error 메시지 유실 방지: 트랜잭션 분리 (DB 저장 + Slack 발송)
  - NotificationService 단위 테스트: 5/5 통과 (lenient Mock 패턴)
  - Entity 예외 타입 통일: NotificationException 도메인 예외 생성
- **Test Results**: 단위 5/5, 통합 4/4, Kafka 4/4, REST API 10/10 (전체 21/21 통과)
- **Documentation**: docs/review/issue-76-notification-risk-refactoring.md

**Issue #84** - 배송 상태 알림 REST API (2025-11-13) ✅ **완료**
- **REST API 추가**
  - POST /api/v1/notifications/delivery-status: 배송 상태 변경 알림 발송
  - DeliveryStatusNotificationRequest DTO (6 필드)
  - NotificationService.sendDeliveryStatusNotification() 메서드
  - DeliveryStatusChangedConsumer 로직 재사용 (메시지 형식 통일)
- **기능**
  - Kafka Event + REST API 일관성 유지
  - 재발송 기능 제공 (Slack 실패 시)
  - 테스트/디버깅 용이성
  - 장애 대응 (Kafka 장애 시 대체 수단)
- **Test Results**: Controller 2/2, REST API 10/10 (test-notification-api.sh)
- **Documentation**: docs/review/issue-84-delivery-status-rest-api.md

### 🚧 Pending

- **Issue #85**: deletedBy 사용자 정보 수집 (예상 0.5일)
- **Issue #86**: Kafka Consumer 보안 강화 (CVSS 7.5 - High, 예상 1일)
- **Issue #87-88**: Performance 개선 (Gemini 캐싱, DLQ, 예상 1.5일)
- **Issue #36**: Daily route optimization scheduler (Challenge, 예상 3-4일)
