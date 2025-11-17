# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**one-for-logis** (14logis) is a B2B logistics management and delivery system built with microservices architecture (MSA). Spring Boot-based project for managing hub operations, deliveries, orders, and personnel across 17 regional centers in South Korea.

**Key Characteristics**:
- MSA with 9 independent services + common-lib
- PostgreSQL with schema separation per service
- Local development with Docker containers
- JWT-based authentication with role-based authorization
- External integrations: Slack API, Google Gemini API, Naver Maps API

## Current Work

**Branch**: `fix/#109-notification-service-swagger-fix`

**Status**: Issue #109 완료, Issue #84 완료 (PR #105)

**Recent Completed**:
- ✅ Issue #76: notification-service 리스크 개선 (트랜잭션 분리, Fallback, 테스트 21/21)
- ✅ Issue #109: Swagger 테스트 수정 + FeignException 처리 + user-service 연동
- ✅ Issue #84: 배송 상태 알림 REST API (PR #105)

**Next**:
- Issue #85-86: 보안 강화 및 리팩토링 (1.5일)
- Issue #36: Challenge - 일일 경로 최적화 (3-4일)

상세 내역: [docs/06-work-log/completed-work.md](docs/06-work-log/completed-work.md)

---

## Communication Rules

**Language**: Always respond in Korean (한국어)

**Response**: Token-efficient, concise, direct

**Code Changes**: Always ask approval before creating/modifying files

**File/Directory Deletion**: NEVER delete directly - ask user to delete manually with exact path

**Document Updates**: Keep all documentation synchronized
- CLAUDE.md 수정 → 관련 docs/ 파일도 업데이트
- 작업 완료 → completed-work.md AND service-status.md 업데이트
- 아키텍처 변경 → database-schema.md, business-rules.md 업데이트
- 이슈 해결 → troubleshooting.md에 추가

**Execution**: Do not execute build/test/clean commands unless explicitly requested

**Git Commits**:
- Follow team convention (lowercase, imperative)
- NEVER add "🤖 Generated with Claude Code" or "Co-Authored-By: Claude"
- NEVER commit CLAUDE.md or pr.md files

## Team Conventions

**Commit Format**: `type: summary` (lowercase, imperative, max 50 chars)
- Types: feat, fix, chore, docs, refactor, test, style, init, del, move, rename

**Branch Strategy**:
- `main`: production (not used)
- `dev`: integration branch
- `feature/#issueNum-description`: new features
- `fix/#issueNum-description`: bug fixes
- `docs/#issueNum-description`: documentation

**Code Style**:
- Entity fields: NO domain prefix (use `id`, not `hubId`)
- DTOs: `DomainVerb + Request/Response` (no "Dto" suffix)
- DTO Pattern: Use `record` for presentation DTOs (immutability)
- External API DTOs: Use `class` with Builder (infrastructure layer)
- Endpoints: plural domain names (`/api/v1/hubs`, not `/hub`)
- Success codes: 200/201, error codes: custom per service

**Comment Style**:
- Prefer `//` for single-line comments
- Use `/* */` only for multi-line when necessary
- JavaDoc (`/** */`): Only for public APIs and interfaces

---

## Quick Reference

### Documentation
- [프로젝트 소개](docs/01-overview/project-intro.md)
- [아키텍처](docs/01-overview/architecture.md)
- [팀 컨벤션](docs/01-overview/team-conventions.md)
- [데이터베이스 스키마](docs/02-development/database-schema.md)
- [비즈니스 규칙](docs/02-development/business-rules.md)
- [패키지 구조](docs/02-development/package-structure.md)
- [Docker 환경](docs/03-infrastructure/docker-environment.md)
- [서비스 구현 현황](docs/03-infrastructure/service-status.md)
- [테스트 가이드](docs/04-testing/testing-guide.md)
- [트러블슈팅](docs/04-testing/troubleshooting.md)
- [완료 작업 로그](docs/06-work-log/completed-work.md)
- [남은 이슈](docs/06-work-log/left-issues.md)

### Service Ports
```
Gateway:       8000  (Authentication)
User:          8100  (JWT, Users)
Hub:           8200  (Hubs, Routes, Dijkstra)
Company:       8300  (Companies)
Order:         8400  (Orders)
Product:       8500  (Products, Inventory)
Delivery:      8600  (Deliveries, Personnel)
Notification:  8700  (AI, Slack, Kafka)
Eureka:        8761  (Service Discovery)
Zipkin:        9411  (Tracing)
PostgreSQL:    5432
Redis:         6379
Kafka:         9092
```

### User Roles
```
MASTER > HUB_MANAGER > DELIVERY_MANAGER > COMPANY_MANAGER
```

### Package Structure (DDD)
```
com.oneforlogis.{service}/
├── presentation/      # Controllers, DTOs, Advice
├── application/       # Business flow orchestration
├── domain/            # Entities, Repositories (interface), Domain services
├── infrastructure/    # JPA impl, FeignClients, External APIs
└── global/            # Config, Security, Common
```

### Common Patterns
- **Soft Delete**: deleted_at, deleted_by (never physical deletion)
- **Audit Fields**: created_at, created_by, updated_at, updated_by
- **Pagination**: Default 10, options: 10, 30, 50
- **Snapshot Pattern**: Save sender info at message send time (notification-service)

---

## Development Commands

**Build**:
```bash
./gradlew build
./gradlew :{service-name}:build
```

**Run with Docker**:
```bash
docker-compose -f docker-compose-team.yml up -d
docker-compose logs -f {service}
docker-compose down
```

**Test**:
```bash
./gradlew test
./gradlew :{service-name}:test
```

상세: [docs/03-infrastructure/docker-environment.md](docs/03-infrastructure/docker-environment.md)

---

## My Assigned Domain: notification-service

**Port**: 8700

**Responsibilities**:
- AI 기반 출발 시한 계산 (Google Gemini API)
- Slack 메시지 발송
- Kafka 이벤트 기반 자동 알림 (주문 생성, 배송 상태 변경)
- 외부 API 호출 로그 및 통계
- **Challenge**: 일일 배송 경로 최적화 (Naver Maps API + TSP)

**External APIs**:
- Slack API: `chat.postMessage`
- Google Gemini API: Departure time calculation, route optimization
- Naver Maps API: Route calculation with waypoints (Challenge feature)

**Status**:
- ✅ REST API (10 endpoints)
- ✅ Kafka Consumers (2 consumers, 멱등성 보장)
- ✅ Query/Statistics APIs (페이징, 필터링)
- ✅ 리스크 개선 (Issue #76)
- ✅ 배송 상태 REST API (Issue #84, PR #105)
- ✅ Swagger 테스트 & FeignException 처리 (Issue #109)
- ⏳ 보안 강화 (Issue #85-86)
- ❌ Challenge 기능 (Issue #36)

상세: [docs/05-api-specs/notification-service-api.md](docs/05-api-specs/notification-service-api.md)

---

## Important Notes

**Security**:
- Gateway: JWT authentication ONLY (not authorization)
- Services: Handle authorization (@PreAuthorize)
- Never log sensitive data (passwords, tokens)

**Service Communication**:
- Use FeignClient for sync calls
- Use Kafka for async events
- Implement Circuit Breaker + Fallback
- No cross-service DB access (REST API only)

**Database**:
- PostgreSQL with separate schemas per service
- Soft Delete pattern (never physical deletion)
- Audit fields on all tables

**Testing**:
- Unit tests: 80%+ coverage
- Integration tests: TestContainers
- Mock FeignClients and external APIs

**MSA Principles**:
- Service independence
- Data independence
- Eventual consistency
- Failure isolation

---

For detailed information, refer to documentation in [docs/](docs/) directory.
