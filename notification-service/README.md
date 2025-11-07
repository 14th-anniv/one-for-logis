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

### 🚧 Pending

- **Issue #14**: 주문 알림 REST API (Gemini AI 프롬프트, Slack 템플릿)
- **Issue #16**: 조회 및 통계 API (MASTER 권한)
- **Issue #35**: Kafka 이벤트 소비자 (order-created, delivery-status-changed)
- **Issue #36**: Daily route optimization scheduler (Challenge)
