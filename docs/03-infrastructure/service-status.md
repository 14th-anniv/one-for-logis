# Service Implementation Status

This document tracks the implementation status of all microservices in the 14logis project.

**Last Updated**: 2025-11-12 (after Issue #76, PR #81, #83, #85, #77, #79)

---

## Overview Table

| Service | Port | Application.yml | DDD Structure | Key Features | Status |
|---------|------|-----------------|---------------|--------------|--------|
| **common-lib** | - | ❌ | ❌ | BaseEntity, ApiResponse, UserPrincipal, SecurityConfigBase, **GlobalExceptionHandler (PR #75)** | ✅ 100% |
| **eureka-server** | 8761 | ✅ | ❌ | Service Discovery | ✅ 100% |
| **gateway-service** | 8000 | ✅ | ❌ | API Gateway, JWT Auth, Routing | ⚠️ 50% |
| **user-service** | 8100 | ✅ | ⚠️ | **로그인/회원가입 (PR #81)**, User management, JWT, Authorization | ⚠️ 30% |
| **hub-service** | 8200 | ✅ | ✅ | Hub CRUD, 허브 경로 CRUD, 다익스트라 최단 경로, Redis 3단계 캐싱 | ⚠️ 75% |
| **company-service** | 8300 | ✅ | ✅ | Company CRUD, 검색/페이징, **HubClient 연동 (PR #75)**, DDD pattern, record DTOs | ⚠️ 60% |
| **product-service** | 8500 | ✅ | ✅ | **기본 CRUD 완료 (PR #65)**, 페이징, DDD pattern - FeignClient 미구현 | ⚠️ 40% |
| **order-service** | 8400 | ✅ | ⚠️ | Order management | ⚠️ 20% |
| **delivery-service** | 8600 | ✅ | ⚠️ | **Kafka Consumer (PR #67, #83)**, **목록/검색 조회 (PR #77)**, **배송 상태 변경 (PR #85)** - BaseEntity/Repository 패턴 미적용 | ⚠️ 40% |
| **notification-service** | 8700 | ✅ | ✅ | Entities, Repositories, External APIs, REST APIs (9 endpoints), **Query/Statistics APIs (PR #68)**, **Kafka Consumers (Issue #35)**, **Risk Refactoring (Issue #76)**, Tests (21/21) | ⚠️ 90% |
| **zipkin-server** | 9411 | ❌ | ❌ | Distributed tracing | ❌ 0% |

---

## Detailed Status by Service

### common-lib 📦

**Overall Progress**: 100% (Fully Implemented and Integrated + **PR #75 Critical Update**)

#### ✅ Core Components

**API Response & Pagination**
- ✅ ApiResponse<T> (record): Generic response wrapper
  - success(), created(), accepted(), noContent()
  - Consistent error/success format across all services
- ✅ PageResponse<T> (class with Builder): Pagination wrapper
  - fromPage(Page<T>), of(List<T>, metadata)
  - Integration with Spring Data Page

**Security & Authentication**
- ✅ SecurityConfigBase (abstract class): Base security configuration
  - CSRF disabled, HeaderAuthFilter integration
  - Public endpoints: /swagger-ui/**, /v3/api-docs/**, /actuator/**
  - configureAuthorization() hook for service-specific rules
- ✅ HeaderAuthFilter: Gateway header-based authentication
  - Reads X-User-Id, X-User-Name, X-User-Role headers
  - Creates UserPrincipal and sets SecurityContext
- ✅ CustomAccessDeniedHandler: 403 error handling
- ✅ UserPrincipal (record): Authentication principal
  - id, username, role
  - Helper methods: isMaster(), hasRole(), getRoleKey()
- ✅ Role (enum): MASTER, HUB_MANAGER, DELIVERY_MANAGER, COMPANY_MANAGER
  - fromKey(), fromName(), getAuthority()

**Exception Handling** ⭐ **PR #75 Critical Update**
- ✅ CustomException: Base business exception with ErrorCode
- ✅ ErrorCode (enum): Centralized error code definitions
  - Common errors: INTERNAL_SERVER_ERROR, METHOD_NOT_ALLOWED, BAD_REQUEST, NOT_FOUND
  - Auth errors: INVALID_TOKEN, EMPTY_TOKEN, EXPIRED_TOKEN, FORBIDDEN_ACCESS
  - Service-specific sections (Hub, Company, Product, Delivery, etc.)
- ✅ **GlobalExceptionHandler (@RestControllerAdvice) - PR #75 개선**
  - **ResponseEntity<ApiResponse<Void>> 반환** - HTTP 상태 코드가 실제 Response에 반영
  - **FeignClient 호출 시 HTTP 상태 코드 기반 예외 처리 가능**
  - CustomException, MethodArgumentNotValidException, BindException
  - HttpRequestMethodNotSupportedException, NoHandlerFoundException
  - AccessDeniedException, generic Exception
  - **변경 영향**: 전체 MSA 통신의 기반이 되는 중요한 개선

**JPA & Auditing**
- ✅ BaseEntity (@MappedSuperclass): Audit fields for all entities
  - createdAt, createdBy, updatedAt, updatedBy
  - deletedAt, deletedBy, deleted (soft delete support)
  - markAsDeleted(String actor), restore(), isActive()
- ✅ JpaAuditConfig: @EnableJpaAuditing configuration
  - AuditorAware<String> bean for username tracking

**API Documentation**
- ✅ SwaggerConfig: Springdoc OpenAPI configuration
  - Auto-configuration for all services
  - API versioning support

**Dependencies** (build.gradle)
- api: spring-boot-starter-json
- implementation: web, security, springdoc-openapi
- compileOnly: validation, data-jpa, swagger-annotations, lombok

---

### notification-service (My Domain) 📢

**Overall Progress**: 90% (Infrastructure + REST API + Kafka Consumers + Risk Refactoring Complete, Challenge Feature Pending)

#### ✅ Completed Components

**Phase 1: Initial Setup** (#11)
- ✅ Spring Boot Application class + @Import for common configs
- ✅ application.yml (DB connection, Eureka registration, External API configs)
- ✅ build.gradle (common-lib + Spring Security + WebClient dependencies)
- ✅ DDD package structure (presentation, application, domain, infrastructure, global)
- ✅ Dockerfile

**Phase 2: Domain Layer** (#12)
- ✅ Entity classes: Notification, ExternalApiLog (with validation)
- ✅ ENUM types: SenderType, MessageType, MessageStatus, ApiProvider
- ✅ Repository interfaces (domain.repository)
- ✅ Repository implementations (infrastructure.persistence)
- ✅ Soft Delete handling with @SQLRestriction
- ✅ Domain service: NotificationDomainService

**Phase 3: Infrastructure** (#13)
- ✅ External API clients: SlackClient, GeminiApiClient (WebClient + Resilience4j)
- ✅ Wrapper pattern: SlackClientWrapper, GeminiClientWrapper
- ✅ ApiLogDomainService (automatic logging with sensitive data masking)
- ✅ Retry logic: Slack (3 attempts), Gemini (2 attempts with exponential backoff)
- ✅ Environment configuration (application.yml, application-test.yml)

**Phase 4: Common Configuration** (#33)
- ✅ SecurityConfig (SecurityConfigBase inheritance)
- ✅ JpaAuditConfig, SwaggerConfig integration from common-lib
- ✅ HeaderAuthFilter applied
- ✅ Test environment Bean override settings

**Phase 5: Application Layer** (Issue #14) ✅
- ✅ NotificationService (application.service) - business flow orchestration
- ✅ Use case implementations (order notification, manual message)
- ✅ ExternalApiLogService - query methods (getAllApiLogs, getApiLogsByProvider, getApiLogsByMessageId)
- ✅ user-service FeignClient integration (UserServiceClient, UserResponse)
- ✅ SecurityConfig 업데이트 (permitAll for /api/v1/notifications/order endpoint)

**Phase 6: Presentation Layer** (Issue #14) ✅
- ✅ REST API Controllers (NotificationController) - 7 endpoints
- ✅ Request/Response DTOs (record pattern with static from() methods)
- ✅ GlobalExceptionHandler (common-lib 사용)
- ✅ API documentation (@Tag, @Operation for Swagger)
- ✅ Pagination support for GET /api/v1/notifications (page, size, sortBy, direction)

**Phase 7: Query & Statistics APIs** (Issue #16) ✅
- ✅ GET /api/v1/notifications/search - 알림 필터링 조회
  - 다중 조건 필터링 (senderUsername, recipientSlackId, messageType, status)
  - 팀 표준 페이징 패턴 (size 검증, sortBy 화이트리스트)
- ✅ GET /api/v1/notifications/api-logs/stats - API 통계 조회
  - Provider별 통계 집계 (성공률, 평균/최소/최대 응답시간, 총 비용)
- ✅ createPageable() 헬퍼 메서드 (SQL Injection 방지)
- ✅ Unit tests: 10/10 passed
- ✅ Docker cURL tests: 10/10 passed

**Phase 8: Event-Driven Integration** (Issue #35) ✅
- ✅ Kafka Consumer 구현 (2개)
  - OrderCreatedConsumer: order.created 토픽 → Gemini AI → Slack 알림
  - DeliveryStatusChangedConsumer: delivery.status.changed 토픽 → Slack 알림
- ✅ KafkaConsumerConfig: 토픽별 별도 ContainerFactory
- ✅ Event DTOs (record): OrderCreatedEvent, DeliveryStatusChangedEvent
- ✅ TopicProperties: @ConfigurationProperties로 토픽 관리
- ✅ Idempotency: event_id 기반 중복 처리 방지 (DB unique constraint)
- ✅ ErrorHandlingDeserializer + JsonDeserializer 조합
- ✅ DB Schema: MessageType enum에 DELIVERY_STATUS_UPDATE 추가
- ✅ PostgreSQL CHECK constraint 수정
- ✅ Integration Tests: test-kafka-consumer.sh (4/4 통과)

**Phase 9: Risk Refactoring** (Issue #76) ✅
- ✅ **NotificationService 단위 테스트** (5/5 통과)
  - lenient Mock 패턴으로 UnnecessaryStubbingException 방지
  - Entity 전체 Mock으로 JPA 관리 필드 접근 문제 해결
- ✅ **트랜잭션 분리** (DB 저장 + Slack 발송)
  - Propagation.REQUIRES_NEW로 에러 메시지 유실 방지
  - Slack 실패 시 HTTP 500 응답 (CustomException)
- ✅ **FeignClient Fallback** (UserServiceClient)
  - NPE 위험 제거, Circuit Breaker 예외는 throw
- ✅ **Gemini messageId 연계**
  - generateContent()에 messageId 파라미터 추가
- ✅ **도메인 예외 통일**
  - NotificationException 생성 및 적용
- ✅ **통합 테스트 Mock 설정**
  - OrderCreatedConsumerIT, DeliveryStatusChangedConsumerIT
  - @BeforeEach에서 Gemini, Slack Mock 응답 설정
- ✅ **Docker 환경 검증**
  - Kafka Consumer 테스트: 4/4 통과
  - REST API 테스트: 10/10 통과
- ✅ **JWT 환경 변수 설정**
  - .env, .env.docker, .env.example 업데이트
  - user-service, gateway-service application.yml 적용

**Testing**
- ✅ Repository tests: 26 tests (100% pass)
- ✅ Unit tests (MockWebServer): 6 tests (100% pass)
- ✅ Integration tests (real APIs): 3 tests (100% pass)
- ✅ Controller tests: 9 tests (MockMvc)
- ✅ Query/Statistics API tests: 10 tests (100% pass)
- ✅ **Service unit tests: 5 tests (100% pass) - NEW**
- ✅ Kafka Consumer integration tests: 4 scenarios (4/4 pass)
- ✅ Docker environment: Kafka 4/4, REST API 10/10
- ✅ Total: 63+ tests

#### ❌ Pending Components

**Phase 10: Additional REST APIs** (Issue #84)
- ❌ GET /api/v1/notifications/delivery/{deliveryId} - 배송 ID로 알림 조회
- ❌ GET /api/v1/notifications/order/{orderId} - 주문 ID로 알림 조회

**Phase 11: Security & Performance** (Issue #85-88)
- ❌ deletedBy 사용자 정보 자동 수집 (Issue #85)
- ❌ Kafka Consumer 보안 강화 - SASL/SSL (Issue #86)
- ❌ Gemini API 캐싱 (Issue #87)
- ❌ Dead Letter Queue (DLQ) setup (Issue #88)

**Phase 12: Advanced Features (Challenge)** (Issue #36)
- ❌ Naver Maps API client
- ❌ Daily route optimization scheduler

**Phase 13: Testing & QA**
- ❌ E2E integration tests (order → delivery → notification flow)
- ❌ Performance testing
- ❌ API contract tests

---

### hub-service 🏢

**Overall Progress**: 70% (Hub CRUD + Route Management Complete, Integration Testing Pending)

#### ✅ Completed Components

**Domain & Infrastructure**
- ✅ Entity: Hub (with soft delete support)
- ✅ Entity: HubRoute (DIRECT/RELAY RouteType, pathNodes JSON)
- ✅ DDD repository pattern (domain interface + infrastructure JpaRepository + RepositoryImpl)
- ✅ Redis caching (HubCacheService with TTL management)
- ✅ Redis 3-tier caching for routes (HubRouteCacheService)

**Application Layer**
- ✅ HubService: Full CRUD + cache management
- ✅ HubRouteService: Route CRUD + shortest path calculation
- ✅ DijkstraService: Shortest path algorithm implementation
- ✅ HubRouteCacheService: Direct route, graph, shortest path caching
- ✅ Cache operations: save, get by ID, get by name, delete, refresh all
- ✅ Soft delete: markAsDeleted() with username tracking
- ✅ Bulk Hub 조회 최적화 (N+1 방지)

**Presentation Layer (Hub Controller)**
- ✅ POST /api/v1/hubs - Create hub (@PreAuthorize MASTER)
- ✅ PUT /api/v1/hubs/{hubId} - Update hub (@PreAuthorize MASTER)
- ✅ DELETE /api/v1/hubs/{hubId} - Soft delete (@PreAuthorize MASTER)
- ✅ GET /api/v1/hubs/{hubId} - Get by ID (cached)
- ✅ GET /api/v1/hubs/name/{hubName} - Get by name (cached)
- ✅ GET /api/v1/hubs - Paginated list (page, size params)
- ✅ POST /api/v1/hubs/cache/refresh - Manual cache refresh (@PreAuthorize MASTER)

**Presentation Layer (Hub Route Controller)** (PR #54)
- ✅ POST /api/v1/hub-routes - 허브 경로 등록
- ✅ PUT /api/v1/hub-routes/{routeId} - 허브 경로 수정
- ✅ DELETE /api/v1/hub-routes/{routeId} - 허브 경로 삭제
- ✅ GET /api/v1/hub-routes - 허브 경로 전체 조회 (페이징)
- ✅ GET /api/v1/hub-routes/{routeId} - 허브 경로 단일 조회
- ✅ GET /api/v1/hub-routes/direct - 직통 경로 조회
- ✅ GET /api/v1/hub-routes/shortest - 최단 경로 조회 (다익스트라)

**DTOs & Patterns**
- ✅ Request DTOs: HubCreateRequest, HubUpdateRequest, HubRouteRequest (record)
- ✅ Response DTOs: HubResponse, HubRouteResponse, ShortestRouteResponse (record)
- ✅ Swagger documentation: @Tag, @Operation annotations
- ✅ Common config: SecurityConfig, JpaAuditConfig, SwaggerConfig
- ✅ Sample data: 17 hubs + 52 direct routes (hub.sql)

#### ⚠️ Known Issues (from PR #54 review)
- ⚠️ PriorityQueue comparator bug (NullPointerException risk)
- ⚠️ Missing visited Set (duplicate node processing)
- ⚠️ Redis cache TTL not set (memory accumulation risk)
- ⚠️ Transaction boundary issue with cache synchronization

#### ❌ Pending Components
- ❌ Unit tests for DijkstraService
- ❌ Integration tests for shortest path API
- ❌ Performance testing (100+ hubs scenario)
- ❌ Redis cache monitoring and metrics

---

### Infrastructure Services

#### eureka-server (100%)
- ✅ Spring Cloud Netflix Eureka Server
- ✅ @EnableEurekaServer configuration
- ✅ Service registration & discovery
- ✅ Dashboard: http://localhost:8761
- ✅ Peer-awareness disabled (standalone mode)

#### gateway-service (50%)
- ✅ Spring Cloud Gateway
- ✅ @EnableDiscoveryClient for Eureka integration
- ✅ Port: 8000
- ✅ Service routing to Eureka-registered services
- ✅ Management endpoints: health, info, gateway
- ❌ JWT authentication filter (authentication only, not authorization)
- ❌ User context header propagation (X-User-Id, X-User-Role, X-Hub-Id, X-Company-Id)
- ❌ Rate limiting
- ❌ Circuit breaker integration
- ❌ CORS configuration

---

### Other Services

#### user-service (30%) - PR #81

**Overall Progress**: 30% (로그인/회원가입 완료, User CRUD APIs 미구현)

#### ✅ Completed Components (PR #81)

**Domain & Infrastructure**
- ✅ User Entity (BIGINT PK, BaseEntity 상속)
- ✅ Status Enum: PENDING, APPROVED, REJECTED
- ✅ Repository: UserRepository (JpaRepository)

**Application Layer**
- ✅ UserService: signup(), login()
- ✅ JWT 토큰 생성/검증 (JwtUtil)
- ✅ Redis 기반 Refresh Token 관리
- ✅ Blacklist 처리 (JTI 기반)

**Presentation Layer**
- ✅ UserController: 2개 엔드포인트
  - POST /api/v1/users/signup - 회원가입 (첫 사용자는 MASTER 자동 승인, 이후는 PENDING)
  - POST /api/v1/users/login - 로그인 (Access Token + Refresh Token)
- ✅ Request DTOs: UserSignupRequest, UserLoginRequest

**Security**
- ✅ BCrypt 비밀번호 암호화
- ✅ Access Token: 30분 (Header)
- ✅ Refresh Token: 14일 (Redis + HttpOnly 쿠키)

#### ❌ Pending Components (from PR #81 review)

**Critical Issues**
- ❌ **User Entity 필드명 규칙 위반** - slack_id, company_name이 snake_case (slackId, companyName으로 변경 필요)
- ❌ **Gateway JwtUtil WebFlux 혼용 문제** - jakarta.servlet 패키지 사용 불가 (WebFlux 환경)
- ❌ **토큰 무효화 로직 호출 누락** - login() 메서드에서 invalidatePreviousTokens() 미호출

**Missing APIs**
- ❌ GET /api/v1/users/{userId} - 사용자 조회 (UserServiceClient에서 필요)
- ❌ GET /api/v1/users/username/{username} - 사용자명으로 조회
- ❌ POST /api/v1/users/{userId}/approve - 사용자 승인 (MASTER, HUB_MANAGER)
- ❌ POST /api/v1/users/{userId}/reject - 사용자 거부 (MASTER, HUB_MANAGER)
- ❌ User CRUD APIs (수정, 삭제)

**Testing**
- ❌ 단위 테스트 미작성
- ❌ 통합 테스트 미작성

#### order-service (20%)
- ✅ Basic structure
- ✅ application.yml
- ❌ Order & OrderItem entities
- ❌ Order creation orchestration
- ❌ FeignClient integrations (company, product, hub, delivery, notification)
- ❌ Order status management
- ❌ Inventory reduction flow

#### company-service (50%)
- ✅ Basic structure
- ✅ application.yml
- ✅ DDD structure (domain, infrastructure, presentation)
- ✅ Entity: Company (CompanyType enum: SUPPLIER, RECEIVER)
- ✅ Repository: DDD pattern (domain interface + infrastructure impl)
- ✅ Controller: CompanyController with @PreAuthorize
- ✅ CRUD APIs (PR #52):
  - POST /api/v1/companies - Create company
  - GET /api/v1/companies/{companyId} - Get company by ID
  - GET /api/v1/companies - Search companies (name + pagination)
  - PUT /api/v1/companies/{companyId} - Update company (partial update)
  - DELETE /api/v1/companies/{companyId} - Soft delete
- ✅ DTOs: record pattern with from() factory method
- ✅ Pagination helper (size validation: 10/30/50)
- ✅ Soft delete filtering
- ⚠️ sortBy field validation needed (security issue from PR review)
- ❌ Company-Hub validation via FeignClient
- ❌ Common config integration (SecurityConfig from common-lib)

#### product-service (40%) - PR #65

**Overall Progress**: 40% (기본 CRUD 완료, FeignClient 연동 미구현)

#### ✅ Completed Components (PR #65)

**Domain & Infrastructure**
- ✅ Entity: Product (BaseEntity 상속, UUID PK, Soft Delete)
- ✅ Repository: DDD pattern (domain interface + infrastructure JpaRepository + RepositoryImpl)
- ✅ Factory method: `createProduct()`
- ✅ 수정 메서드: `updateName()`, `updateQuantity()`, `updatePrice()`

**Application Layer**
- ✅ ProductService: Full CRUD + 페이징 검증 (size: 10, 30, 50)
- ✅ Request DTOs: `ProductCreateRequest`, `ProductUpdateRequest` (record 타입)
- ✅ Response DTOs: `ProductCreateResponse`, `ProductUpdateResponse`, `ProductDetailResponse`, `ProductSearchResponse`

**Presentation Layer**
- ✅ ProductController: 5개 엔드포인트
  - POST /api/v1/products - Create product (@PreAuthorize MASTER, HUB_MANAGER, COMPANY_MANAGER)
  - PATCH /api/v1/products/{productId} - Update product
  - DELETE /api/v1/products/{productId} - Soft delete
  - GET /api/v1/products/{productId} - Get by ID
  - GET /api/v1/products - Search with pagination (name + page + size)
- ✅ Swagger 문서화 (@Operation, @Tag)

**Configuration**
- ✅ SecurityConfig (SecurityConfigBase 상속)
- ✅ @Transactional(readOnly = true) 클래스 레벨 적용

#### ❌ Pending Components (from PR #65 review)

**Critical Issues**
- ❌ FeignClient 미구현 (Hub/Company 검증 없음) - 존재하지 않는 hubId/companyId로 Product 생성 가능
- ❌ Entity 예외 타입 불일치 (IllegalArgumentException → CustomException)
- ❌ Controller 응답 타입 혼용 (ResponseEntity + ApiResponse 표준화 필요)

**Testing**
- ❌ 단위 테스트 미작성 (ProductService, Product Entity)
- ❌ 통합 테스트 미작성

---

#### delivery-service (40%) - PR #67, #73, #77, #85

**Overall Progress**: 40% (Kafka Consumer + CRUD APIs 완료, BaseEntity/Repository 패턴 미적용)

#### ✅ Completed Components

**Phase 1: Kafka Event-Driven (PR #67)**
- ✅ Entity: Delivery (UUID PK, Unique Constraint on order_id)
- ✅ DeliveryStatus Enum: 7단계 상태 정의 (WAITING_AT_HUB, MOVING_BETWEEN_HUBS, ARRIVED_DEST_HUB, OUT_FOR_DELIVERY, MOVING_TO_COMPANY, COMPLETED, CANCELED)
- ✅ Repository: DeliveryRepository (JpaRepository 직접 상속 - **팀 패턴 불일치**)
- ✅ Kafka Consumer: `OrderCreatedConsumer` - order.created 이벤트 수신
- ✅ DeliveryService: `createIfAbsentFromOrder()` - Idempotency 처리 (DB Unique + 애플리케이션 레벨 중복 체크)
- ✅ TopicProperties: @ConfigurationProperties로 topic 이름 외부 설정
- ✅ 테스트: 단위 테스트 (Idempotency), 통합 테스트 (Embedded Kafka)

**Phase 2: 단건 조회 API (PR #73)**
- ✅ DeliveryResponse DTO (Builder 패턴)
- ✅ DeliveryService: `getOne(UUID deliveryId)` 메서드
- ✅ DeliveryController: GET /api/v1/deliveries/{deliveryId}
- ✅ CustomException 적용 (ErrorCode.DELIVERY_NOT_FOUND)
- ✅ Controller 테스트 작성 (성공/실패 케이스)

**Phase 3: 목록/검색 조회 API (PR #77)**
- ✅ JPA Specification 패턴 (DeliverySpecifications)
- ✅ DeliverySearchCond DTO (record): 5개 검색 조건 (status, receiverName, orderId, fromHubId, toHubId)
- ✅ DeliveryService: `search(DeliverySearchCond, Pageable)` 메서드
- ✅ DeliveryController: GET /api/v1/deliveries - 목록/검색 조회 (페이징)
- ✅ DeliveryResponse: Builder → record 타입 변환
- ✅ Controller 테스트: 5개 테스트 케이스

**Phase 4: 배송 상태 변경 API (PR #85)**
- ✅ DeliveryStatusUpdateRequest DTO
- ✅ DeliveryService: `updateStatus()` 메서드 (상태 전이 검증)
- ✅ DeliveryController: PATCH /api/v1/deliveries/{deliveryId}/status
- ✅ DeliveryStatus 상태 머신 (7개 상태, 엄격한 전이 규칙)

#### ❌ Pending Components (from PR #67, #73 reviews)

**Critical Issues**
- ❌ **BaseEntity 미적용** - 감사 필드 없음 (created_at, created_by, updated_at, updated_by, deleted)
- ❌ **Entity 필드 타입 불일치** - hubId: String (실제 UUID 필요), deliveryStaffId: String (실제 Long 필요)
- ❌ **Repository 패턴 불일치** - domain.repository가 JpaRepository 직접 상속 (infrastructure 분리 필요)
- ❌ **중복 메서드** - `findByDeliveryId()` (기본 `findById()` 사용 권장)
- ❌ Controller ApiResponse 미사용 (팀 표준 불일치)

**Infrastructure**
- ❌ Kafka Configuration (수동 커밋, 재시도 정책 미설정)
- ❌ Consumer 에러 처리 강화 (try-catch, DLQ 미구현)
- ❌ DeliveryService 쿼리 최적화 (`existsByOrderId()` + `findByOrderId()` 중복 호출)

**Business Logic**
- ❌ 배송 담당자 자동 할당 (Round-Robin 미구현)
- ❌ 배송 경로 자동 생성 (HubClient 호출 미구현)
- ❌ Notification Service 연동 (배송 생성 시 Slack 알림 미구현)

**Testing**
- ❌ Service 계층 단위 테스트 미작성

#### zipkin-server (0%)
- ❌ Not started
- Low priority: Monitoring only

---

## Docker Environment 🐳

**docker-compose-team.yml** (Volume mount strategy - Recommended for development)

**Infrastructure Services**:
- ✅ PostgreSQL 17 (port 5432)
  - Container: postgres-ofl
  - Init script: scripts/init-databases.sql
  - Volumes: postgres_data
  - Healthcheck enabled

- ✅ Redis 7-alpine (port 6379)
  - Container: redis-ofl
  - Persistence: AOF (appendonly yes)
  - Volumes: redis_data
  - Healthcheck enabled

**Microservices**:
- All services use openjdk:17.0.1 image
- Volume mount: `./service-name/build/libs:/app`
- JAR execution: `java -jar /app/service-name-0.0.1-SNAPSHOT.jar`
- Network: ofl-net (bridge)
- Environment: .env.docker

**Service Dockerfiles**:
- Base image: eclipse-temurin:17-jre-alpine
- WORKDIR: /app
- COPY build/libs/*.jar app.jar
- EXPOSE: service-specific port
- ENTRYPOINT: ["java", "-jar", "app.jar"]

**Environment Variables** (.env.example):
- POSTGRES_*: DB configuration
- REDIS_*: Cache configuration
- *_DB: Schema names per service
- SLACK_BOT_TOKEN, GEMINI_API_KEY: External API keys

**Development Strategy**:
1. Local build: `./gradlew build` or `./gradlew :service-name:build`
2. Docker compose up: `docker-compose -f docker-compose-team.yml up -d`
3. Hot reload: Volume mount allows JAR replacement without rebuild
4. Logs: `docker-compose logs -f service-name`

---

## Common Config Integration Status

| Service | SecurityConfig | JpaAuditConfig | SwaggerConfig | Status |
|---------|---------------|----------------|---------------|--------|
| hub-service | ✅ | ✅ | ✅ | Complete |
| notification-service | ✅ | ✅ | ✅ | Complete |
| user-service | ❌ | ❌ | ❌ | Pending |
| order-service | ❌ | ❌ | ❌ | Pending |
| company-service | ❌ | ❌ | ❌ | Pending |
| product-service | ❌ | ❌ | ❌ | Pending |
| delivery-service | ❌ | ❌ | ❌ | Not started |

---

## Next Priorities

### High Priority (Week 1-2)
1. **notification-service**: Kafka event consumers (Issue #35)
2. **delivery-service**: Start initial setup and domain modeling
3. **user-service**: Complete user management APIs

### Medium Priority (Week 3-4)
4. **order-service**: Implement order creation orchestration
5. **notification-service**: Query/Statistics APIs (Issue #16)
6. **company-service**, **product-service**: Complete CRUD APIs

### Low Priority (Week 5+)
7. **notification-service**: Challenge features (Issue #36)
8. Common config integration for remaining services
9. Zipkin distributed tracing setup