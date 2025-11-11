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

### 🚧 Pending

- **Issue #35**: Kafka 이벤트 소비자 (order-created, delivery-status-changed)
- **Issue #36**: Daily route optimization scheduler (Challenge)
- **DTO Refactoring**: presentation → application 계층 이동 (튜터 권장사항)
