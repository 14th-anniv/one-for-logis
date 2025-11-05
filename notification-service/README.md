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

- `p_notifications`: Message history with sender/recipient snapshots
- `p_external_api_logs`: External API call monitoring
- `p_company_delivery_routes`: Daily optimized delivery routes (Challenge)

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
docker build -t notification-service:latest ./notification-service

# Run container
docker run -p 8700:8700 notification-service:latest
```
