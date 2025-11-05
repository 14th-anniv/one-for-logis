# notification-service

Notification and AI integration service for 14logis logistics system.

## Overview

- **Port**: 8700
- **Database Schema**: notification_db
- **Service Discovery**: Registered with Eureka Server

## Features

- **Order Notifications**: AI-based departure time calculation and Slack messaging
- **Manual Messages**: User-triggered Slack messages with sender snapshot
- **API Logging**: External API call monitoring (Slack, ChatGPT, Naver Maps)
- **Daily Route Optimization** (Challenge): ChatGPT TSP + Naver Maps routing at 06:00

## Tech Stack

- Spring Boot 3.5.7
- Spring Data JPA
- PostgreSQL
- Spring Cloud Eureka Client
- Spring Cloud OpenFeign
- Lombok

## External APIs

- **Slack API**: chat.postMessage for notifications
- **ChatGPT API**: Departure time calculation, route optimization
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
NOTIFICATION_SERVICE_PORT=8700
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
NOTIFICATION_DB=notification_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_password
EUREKA_SERVER_URL=http://localhost:8761/eureka
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

### ✅ Completed (Issue #12)

- Domain entities: `Notification`, `ExternalApiLog`
- Repository layer: Domain interfaces + Infrastructure implementations
- JPA configurations: Auditing, soft delete with `@SQLRestriction`
- Test coverage: 26 tests (15 Notification + 11 ExternalApiLog) - 100% pass
- Docker integration: PostgreSQL 17 with JSONB support

### 🚧 Pending

- Presentation layer (REST API controllers)
- Application layer (Facade, use cases)
- External API clients (Slack, ChatGPT, Naver Maps)
- Business logic implementation
