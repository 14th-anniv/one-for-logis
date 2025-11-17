# Docker Environment Guide

This document provides comprehensive information about Docker setup, environment variables, and execution environments for the 14logis project.

**Last Updated**: 2025-11-07

---

## Table of Contents

1. [Overview](#overview)
2. [Docker Compose Files](#docker-compose-files)
3. [Environment Variables](#environment-variables)
4. [Service Configuration](#service-configuration)
5. [Network Architecture](#network-architecture)
6. [Troubleshooting](#troubleshooting)

---

## Overview

The 14logis project uses Docker Compose for local development with the following infrastructure:

- **PostgreSQL**: Multi-schema database (7 schemas for services)
- **Redis**: Cache for hub-service
- **Eureka Server**: Service discovery
- **8 Microservices**: Spring Boot applications

### Deployment Strategy

- **Local Development**: Docker Compose with Volume mounts (hot-reload)
- **Production**: Not applicable (local-only project)

---

## Docker Compose Files

### 1. `docker-compose.yml` (Team Standard - Image-based)

**Purpose**: Original team configuration using pre-built Docker images

**Characteristics**:
- Services built as Docker images
- No volume mounts (requires rebuild for code changes)
- Suitable for stable releases

**Usage**:
```bash
# Build all images
docker-compose build

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f [service-name]

# Stop all services
docker-compose down
```

### 2. `docker-compose-team.yml` (Volume Mount Strategy)

**Purpose**: Development configuration with Volume mounts for faster iteration

**Characteristics**:
- ✅ Volume mounts for source code (hot-reload without rebuild)
- ✅ Separate build directories per service
- ✅ Uses `.env.docker` for Docker-specific environment variables
- ✅ Faster development cycle (no rebuild needed for code changes)

**Usage**:
```bash
# Start services with volume mounts
docker-compose -f docker-compose-team.yml up -d

# Rebuild specific service
docker-compose -f docker-compose-team.yml up --build [service-name]

# Stop all services
docker-compose -f docker-compose-team.yml down
```

**Volume Mount Structure**:
```yaml
volumes:
  - ./eureka-server/build:/app/build
  - ./eureka-server/src:/app/src
  - ./eureka-server/build.gradle:/app/build.gradle
```

### 3. `docker-compose-v12.yml` (Personal Development)

**Purpose**: Personal development configuration (archived)

**Status**: ⚠️ Deprecated - Use `docker-compose-team.yml` instead

---

## Environment Variables

### Environment Files

| File | Purpose | Usage | Network |
|------|---------|-------|---------|
| `.env` | Local development (IDE, gradlew) | `localhost` hostnames | Host network |
| `.env.docker` | Docker Compose environment | Docker service names | Docker network |

### `.env` (Local Development)

**Location**: Project root
**Used by**: IntelliJ IDEA, Gradle tasks, local Spring Boot runs

```properties
# PostgreSQL (Local)
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_DB=postgres

# Database Schemas
USER_DB=user_db
HUB_DB=hub_db
COMPANY_DB=company_db
PRODUCT_DB=product_db
ORDER_DB=order_db
DELIVERY_DB=delivery_db
NOTIFICATION_DB=notification_db

# Eureka Server (Local)
EUREKA_SERVER_URL=http://localhost:8761/eureka

# Redis (Local)
REDIS_HOST=localhost
REDIS_PORT=6379

# Service Ports
EUREKA_PORT=8761
GATEWAY_PORT=8000
USER_SERVICE_PORT=8100
HUB_SERVICE_PORT=8200
COMPANY_SERVICE_PORT=8300
ORDER_SERVICE_PORT=8400
PRODUCT_SERVICE_PORT=8500
DELIVERY_SERVICE_PORT=8600
NOTIFICATION_SERVICE_PORT=8700

# External APIs (Add your keys here)
SLACK_BOT_TOKEN=xoxb-your-slack-bot-token
GEMINI_API_KEY=your-gemini-api-key
NAVER_MAPS_CLIENT_ID=your-naver-maps-client-id
NAVER_MAPS_CLIENT_SECRET=your-naver-maps-client-secret
```

### `.env.docker` (Docker Environment)

**Location**: Project root
**Used by**: docker-compose-team.yml

```properties
# PostgreSQL (Docker Network)
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_DB=postgres

# Database Schemas (same as .env)
USER_DB=user_db
HUB_DB=hub_db
COMPANY_DB=company_db
PRODUCT_DB=product_db
ORDER_DB=order_db
DELIVERY_DB=delivery_db
NOTIFICATION_DB=notification_db

# Eureka Server (Docker Network)
EUREKA_SERVER_URL=http://eureka-server:8761/eureka

# Redis (Docker Network)
REDIS_HOST=redis
REDIS_PORT=6379

# Service Ports (same as .env)
EUREKA_PORT=8761
GATEWAY_PORT=8000
USER_SERVICE_PORT=8100
HUB_SERVICE_PORT=8200
COMPANY_SERVICE_PORT=8300
ORDER_SERVICE_PORT=8400
PRODUCT_SERVICE_PORT=8500
DELIVERY_SERVICE_PORT=8600
NOTIFICATION_SERVICE_PORT=8700

# External APIs (same as .env)
SLACK_BOT_TOKEN=${SLACK_BOT_TOKEN}
GEMINI_API_KEY=${GEMINI_API_KEY}
NAVER_MAPS_CLIENT_ID=${NAVER_MAPS_CLIENT_ID}
NAVER_MAPS_CLIENT_SECRET=${NAVER_MAPS_CLIENT_SECRET}
```

### Key Differences

| Variable | Local (.env) | Docker (.env.docker) | Reason |
|----------|-------------|----------------------|--------|
| POSTGRES_HOST | localhost | postgres | Docker service name |
| EUREKA_SERVER_URL | http://localhost:8761 | http://eureka-server:8761 | Docker service name |
| REDIS_HOST | localhost | redis | Docker service name |

---

## Service Configuration

### Port Assignments

**Infrastructure Services**:
- PostgreSQL: `5432`
- Redis: `6379`
- Eureka Server: `8761`
- Gateway: `8000`
- Zipkin: `9411` (not started)

**Microservices**:
| Service | Port | Status | Health Check |
|---------|------|--------|--------------|
| user-service | 8100 | ✅ Running | http://localhost:8100/actuator/health |
| hub-service | 8200 | ✅ Running | http://localhost:8200/actuator/health |
| company-service | 8300 | ⚠️ Basic | - |
| order-service | 8400 | ✅ Running | http://localhost:8400/actuator/health |
| product-service | 8500 | ⚠️ Basic | - |
| delivery-service | 8600 | ❌ Not Started | - |
| notification-service | 8700 | ✅ Running | http://localhost:8700/actuator/health |

### Service Port Configuration

**IMPORTANT**: All services must have `server.port` configured in `application.yml`

**Issue encountered (2025-11-07)**: Services were running on default port 8080 instead of configured ports because `server.port` was missing.

**Solution**: Add to each service's `application.yml`:

```yaml
server:
  port: ${SERVICE_PORT:default_port}
```

**Examples**:
```yaml
# eureka-server/src/main/resources/application.yml
server:
  port: ${EUREKA_PORT:8761}

# gateway-service/src/main/resources/application.yml
server:
  port: ${GATEWAY_PORT:8000}

# hub-service/src/main/resources/application.yml
server:
  port: ${HUB_SERVICE_PORT:8200}

# notification-service/src/main/resources/application.yml
server:
  port: ${NOTIFICATION_SERVICE_PORT:8700}
```

### Database Schema Separation

Each service has its own PostgreSQL schema:

```sql
-- Schemas
CREATE DATABASE user_db;
CREATE DATABASE hub_db;
CREATE DATABASE company_db;
CREATE DATABASE product_db;
CREATE DATABASE order_db;
CREATE DATABASE delivery_db;
CREATE DATABASE notification_db;
```

**Table Naming Convention**: All tables prefixed with `p_`
- user_db: `p_users`
- hub_db: `p_hubs`, `p_hub_routes`
- notification_db: `p_notifications`, `p_external_api_logs`

---

## Network Architecture

### Docker Network (docker-compose-team.yml)

```
Docker Network: one-for-logis_default
│
├── postgres:5432 (PostgreSQL)
├── redis:6379 (Redis)
├── eureka-server:8761 (Service Discovery)
├── gateway-service:8000 (API Gateway)
│
├── user-service:8100
├── hub-service:8200
├── order-service:8400
└── notification-service:8700
```

### Service Communication

**Within Docker Network**:
- Services communicate using Docker service names
- Example: `http://eureka-server:8761/eureka`

**From Host to Container**:
- Access via localhost with port mapping
- Example: `http://localhost:8761` (Eureka dashboard)

**Container to External APIs**:
- Direct internet access (Slack, Gemini, Naver Maps)
- No proxy required

---

## Troubleshooting

### Common Issues

#### 1. Port Conflicts

**Issue**: Port already in use

**Windows Reserved Ports**:
- Windows Hyper-V reserves port ranges (e.g., 8153-8252)
- Check with: `netsh interface ipv4 show excludedportrange protocol=tcp`

**Solution**:
```bash
# Check if port is in use
netstat -ano | findstr :8200

# Change service port in .env if needed
HUB_SERVICE_PORT=8210
```

#### 2. Service Running on Wrong Port (8080)

**Issue**: Service ignores configured port, runs on 8080

**Root Cause**: Missing `server.port` in `application.yml`

**Solution**: Add to `application.yml`:
```yaml
server:
  port: ${SERVICE_PORT:default_port}
```

#### 3. Redis Connection Failed

**Issue**: `RedisConnectionException: Unable to connect to localhost:6379`

**Root Cause**: Service trying to connect to `localhost` instead of Docker service name

**Solution**: Check `application.yml`:
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}  # Uses REDIS_HOST from .env.docker
      port: ${REDIS_PORT:6379}
```

#### 4. Database Connection Refused

**Issue**: `Connection refused: localhost:5432`

**Root Cause**: Using `.env` values instead of `.env.docker` in Docker

**Solution**: Ensure docker-compose uses correct env file:
```yaml
services:
  hub-service:
    env_file:
      - .env.docker  # NOT .env
```

#### 5. Eureka Registration Failed

**Issue**: Service cannot register with Eureka

**Root Cause**: EUREKA_SERVER_URL points to localhost

**Solution**: Use Docker service name in `.env.docker`:
```properties
EUREKA_SERVER_URL=http://eureka-server:8761/eureka
```

#### 6. Gradle Build Fails (Windows File Lock)

**Issue**: `Unable to delete directory 'build'`

**Root Cause**: Windows file locking (IntelliJ, Docker, Gradle daemon)

**Solution**:
```bash
# Stop Gradle daemon
./gradlew --stop

# If still fails, close IntelliJ and retry
# Build without clean
./gradlew build -x test
```

### Health Check Commands

**Check all services**:
```bash
# Eureka Server
curl http://localhost:8761/actuator/health

# Gateway Service
curl http://localhost:8000/actuator/health

# User Service
curl http://localhost:8100/actuator/health

# Hub Service
curl http://localhost:8200/actuator/health

# Order Service
curl http://localhost:8400/actuator/health

# Notification Service
curl http://localhost:8700/actuator/health
```

**Check Docker containers**:
```bash
# List all containers
docker-compose -f docker-compose-team.yml ps

# Check logs
docker-compose -f docker-compose-team.yml logs -f [service-name]

# Check specific service logs
docker logs -f [container-id]
```

**Check environment variables in container**:
```bash
# Exec into container
docker exec -it [container-id] sh

# Check environment
env | grep POSTGRES
env | grep EUREKA
env | grep REDIS
```

---

## Best Practices

### 1. Environment Variable Management

✅ **DO**:
- Use `.env` for local IDE development
- Use `.env.docker` for Docker Compose
- Never commit API keys to Git (add `.env` to `.gitignore`)
- Provide `.env.example` with placeholder values

❌ **DON'T**:
- Don't hardcode hostnames in `application.yml`
- Don't use `localhost` in Docker environment
- Don't mix `.env` and `.env.docker` configurations

### 2. Docker Development Workflow

**Recommended Workflow** (with Volume mounts):
```bash
# 1. Start infrastructure only
docker-compose -f docker-compose-team.yml up -d postgres redis eureka-server

# 2. Wait for services to be healthy
sleep 10

# 3. Start microservices
docker-compose -f docker-compose-team.yml up -d

# 4. Make code changes (no rebuild needed with Volume mounts)
# Edit Java files...

# 5. Restart specific service
docker-compose -f docker-compose-team.yml restart notification-service

# 6. View logs
docker-compose -f docker-compose-team.yml logs -f notification-service
```

**When to Rebuild**:
- Dependency changes (build.gradle)
- Configuration changes (application.yml structure)
- Dockerfile changes

### 3. Service Startup Order

**Recommended Order**:
1. PostgreSQL, Redis (infrastructure)
2. Eureka Server (service discovery)
3. Gateway Service (API gateway)
4. Other microservices (parallel)

**Docker Compose handles this automatically with `depends_on`**:
```yaml
services:
  eureka-server:
    depends_on:
      - postgres
      - redis

  gateway-service:
    depends_on:
      - eureka-server

  notification-service:
    depends_on:
      - eureka-server
      - postgres
      - redis
```

### 4. Testing External API Integration

**In Docker Environment**:
```bash
# Test Slack API from container
docker exec -it notification-service sh
curl -X POST https://slack.com/api/chat.postMessage \
  -H "Authorization: Bearer $SLACK_BOT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"channel":"C08321JRW53","text":"Test"}'

# Test Gemini API
curl -X POST "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite:generateContent?key=$GEMINI_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"contents":[{"parts":[{"text":"Hello"}]}]}'
```

---

## Quick Reference

### Environment Comparison

| Aspect | Local Development | Docker Environment |
|--------|------------------|-------------------|
| Config File | `.env` | `.env.docker` |
| PostgreSQL Host | `localhost` | `postgres` |
| Redis Host | `localhost` | `redis` |
| Eureka URL | `http://localhost:8761` | `http://eureka-server:8761` |
| Code Changes | Instant (IDE) | Requires restart (with volumes) |
| Network | Host network | Docker network |
| Use Case | Unit tests, debugging | Integration testing, team demo |

### Common Commands

```bash
# Start all services
docker-compose -f docker-compose-team.yml up -d

# Stop all services
docker-compose -f docker-compose-team.yml down

# Rebuild specific service
docker-compose -f docker-compose-team.yml up --build notification-service

# View logs (all services)
docker-compose -f docker-compose-team.yml logs -f

# View logs (specific service)
docker-compose -f docker-compose-team.yml logs -f notification-service

# Restart service
docker-compose -f docker-compose-team.yml restart notification-service

# Check service status
docker-compose -f docker-compose-team.yml ps

# Remove all containers and volumes
docker-compose -f docker-compose-team.yml down -v

# Build without Docker (for testing)
./gradlew :notification-service:build -x test
```

---

## Related Documentation

- [CLAUDE.md](../CLAUDE.md) - Project overview and conventions
- [troubleshooting.md](./troubleshooting.md) - Common issues and solutions
- [service-status.md](./service-status.md) - Service implementation status
- [testing-guide.md](./testing-guide.md) - Testing strategies

---

## Change Log

- **2025-11-07**: Initial creation with comprehensive Docker and environment setup
- **2025-11-07**: Added service port configuration troubleshooting
- **2025-11-07**: Documented Volume mount strategy (docker-compose-team.yml)