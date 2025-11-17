# Completed Work History

This file tracks all completed work for the 14logis project, organized by date and priority.

---

## 2025-11-13

### Issue #109: Notification Service Swagger Test & FeignException Fix ✅ ⭐⭐⭐

**Type**: Bug Fix + Refactoring
**Service**: notification-service
**Branch**: fix/#109-notification-service-swagger-fix
**Status**: Completed (Ready for PR)
**Issue**: #109

**핵심 개선 사항**:
- Swagger 테스트 오류 수정 (Slack ID 통일)
- FeignException 처리 로직 추가
- user-service 마이페이지 API 연동 개선

**Completed Items**:
- ✅ **Swagger 테스트 수정**:
  1. Slack ID 통일 - 모든 테스트/DTO에서 C09QY22AMEE 사용
  2. NotificationControllerTest 수정 - 26개 테스트 케이스 업데이트
  3. Request/Response DTO Slack ID 필드 일관성 확보

- ✅ **FeignException 처리**:
  1. NotificationExceptionHandler에 FeignException 핸들러 추가
  2. HTTP 상태 코드 매핑 (400, 401, 403, 404, 500, 502, 503, default)
  3. 사용자 친화적 에러 메시지 제공
  4. Java 17 switch expression 활용

- ✅ **user-service 연동 개선**:
  1. UserServiceClient - getMyInfo() API 사용으로 변경
  2. FeignClient 응답 null 체크 강화
  3. NotificationController - userApiResponse 검증 로직 추가

**Test Results**:
- ✅ Swagger 테스트: 정상 동작 확인
- ✅ FeignException 처리: 7가지 HTTP 에러 케이스 처리
- ✅ user-service 연동: 마이페이지 API 정상 동작

**Technical Highlights**:
- FeignException → HTTP 상태 코드 추출 및 적절한 응답 반환
- Slack ID 통일로 테스트/운영 환경 일관성 확보
- user-service getUserByUsername() → getMyInfo() 변경 (보안 강화)

**영향 범위**: ⭐⭐⭐
- notification-service의 안정성 향상
- 외부 서비스(user-service) 연동 오류 처리 개선
- Swagger 문서/테스트 정확성 확보

**Commits**:
- 07a28ca: fix: 스웨거 테스트 코드 설정 및 오류 코드 설정 수정
- 0ba3b72: fix: update user information retrieval to use user-service my page API

**Next Steps**:
- PR 생성 및 코드 리뷰 요청
- Issue #85-86: 보안 강화 (예상 1.5일)
- Issue #36: Challenge (예상 3-4일)

---

### PR #105: 배송 상태 변경 알림 REST API 추가 ✅ ⭐⭐⭐

**Type**: Feature
**Service**: notification-service
**Branch**: feature/#84-delivery-status-rest-api
**Status**: Merged to dev
**Issue**: #84

**핵심 개선 사항**:
- 배송 상태 변경 알림 REST API 추가
- Kafka Event와 동일한 기능을 REST로 제공

**Completed Items**:
- ✅ POST /api/v1/notifications/delivery-status 엔드포인트 추가
- ✅ DeliveryStatusNotificationRequest DTO 작성
- ✅ NotificationService.sendDeliveryStatusNotification() 구현
- ✅ DeliveryStatusChangedConsumer 로직 재사용
- ✅ 권한 검증: ALL (MASTER, HUB_MANAGER, DELIVERY_MANAGER, COMPANY_MANAGER)
- ✅ API 문서화 (Swagger)

**Technical Highlights**:
- REST + Kafka 이중 지원으로 일관성 확보
- Slack 발송 실패 시 수동 재전송 가능
- Kafka 장애 시 대체 수단 제공

**영향 범위**: ⭐⭐⭐
- notification-service REST API 완성도 향상
- 운영 안정성 확보 (장애 대응 수단 추가)

**Commit**:
- b755707: feat: 알림 서비스: 배송 상태 변경 rest api

---

## 2025-11-12

### Issue #76: Notification Service Risk Refactoring ✅ ⭐⭐⭐⭐

**Type**: Refactoring + Testing
**Service**: notification-service
**Branch**: refactor/#76-notification-risk-refatoring
**Status**: Completed (Ready for PR)
**Issue**: #76

**핵심 개선 사항**:
- Codex 리뷰에서 식별된 7개 리스크 항목 모두 개선
- 단위 테스트, 통합 테스트, Docker 환경 검증 완료

**Completed Items**:
- ✅ **Priority 1 (Critical)**:
  1. 통합 테스트 분리 - OrderCreatedConsumerIT, DeliveryStatusChangedConsumerIT Mock 설정
  2. user-service NPE 위험 제거 - FeignClient Fallback 구현
  3. Slack 실패 시 HTTP 응답 개선 - 500 Internal Server Error 반환

- ✅ **Priority 2 (High)**:
  4. Gemini messageId 연계 - generateContent()에 messageId 파라미터 추가
  5. Slack error 메시지 유실 방지 - 트랜잭션 분리 (DB 저장 + Slack 발송)
  6. NotificationService 단위 테스트 - 5/5 통과 (lenient Mock 패턴)
  7. Entity 예외 타입 통일 - NotificationException 도메인 예외 생성

**Test Results**:
- ✅ Unit Tests: 5/5 (NotificationServiceTest)
- ✅ Integration Tests: 4/4 (Kafka Consumers)
- ✅ Docker Environment: Kafka 4/4, REST API 10/10

**Technical Highlights**:
- lenient Mock 패턴으로 UnnecessaryStubbingException 방지
- 트랜잭션 분리 (Propagation.REQUIRES_NEW)로 에러 메시지 유실 방지
- FeignClient Fallback으로 NPE 위험 제거
- Slack 실패 시 HTTP 500 응답으로 명확한 에러 전달

**영향 범위**: ⭐⭐⭐⭐
- notification-service의 안정성 및 테스트 커버리지 크게 향상
- 트랜잭션 분리 패턴은 다른 서비스에도 적용 가능

**Next Steps**:
- Issue #84: 배송 상태 REST API (예상 1일)
- Issue #85-86: 보안 강화 (예상 1.5일)
- Issue #36: Challenge (예상 3-4일)

---

### PR #85: 배송 상태 변경 기능 구현 ✅

**Type**: Feature
**Service**: delivery-service
**Branch**: feature/#72-delivery-status-update
**Status**: Merged to dev
**Issue**: #72

**Completed Items**:
- ✅ DeliveryStatusUpdateRequest DTO
- ✅ DeliveryService.updateStatus() - 상태 전이 검증
- ✅ DeliveryController: PATCH /api/v1/deliveries/{deliveryId}/status
- ✅ DeliveryStatus 상태 머신 (7개 상태, 엄격한 전이 규칙)

**영향 범위**: ⭐⭐⭐
- delivery-service 핵심 기능 완성 (배송 상태 관리)

---

### PR #83: 알림 서비스 Kafka 연동 ✅ ⭐⭐⭐⭐

**Type**: Feature (Event-Driven Architecture)
**Service**: notification-service
**Branch**: feature/#35-notification-service-kafka-challenge
**Status**: Merged to dev
**Issue**: #35

**Completed Items**:
- ✅ OrderCreatedConsumer: order.created → Gemini AI → Slack 알림
- ✅ DeliveryStatusChangedConsumer: delivery.status.changed → Slack 알림
- ✅ KafkaConsumerConfig: 토픽별 별도 ContainerFactory
- ✅ Event DTOs (record): OrderCreatedEvent, DeliveryStatusChangedEvent
- ✅ TopicProperties: @ConfigurationProperties로 토픽 관리
- ✅ Idempotency: event_id 기반 중복 처리 방지 (DB unique constraint)
- ✅ DB Schema: MessageType enum에 DELIVERY_STATUS_UPDATE 추가
- ✅ PostgreSQL CHECK constraint 수정
- ✅ Integration Tests: 4/4 통과

**영향 범위**: ⭐⭐⭐⭐
- notification-service의 핵심 기능 완성 (이벤트 기반 알림)
- order-service, delivery-service와의 비동기 통합 완성

---

### PR #81: 로그인 기능 구현 ✅ ⭐⭐⭐

**Type**: Feature
**Service**: user-service, gateway-service
**Branch**: feature/#7-user-login
**Status**: Merged to dev
**Issue**: #7

**Completed Items**:
- ✅ User Entity (BIGINT PK, Status enum: PENDING/APPROVED/REJECTED)
- ✅ UserService: signup(), login()
- ✅ JWT 토큰 생성/검증 (JwtUtil)
- ✅ Redis 기반 Refresh Token 관리
- ✅ Blacklist 처리 (JTI 기반)
- ✅ Gateway JWT Authentication Filter
- ✅ POST /api/v1/users/signup, /api/v1/users/login

**Critical Issues** (from review):
- ⚠️ User Entity 필드명 규칙 위반 (slack_id → slackId)
- ⚠️ Gateway WebFlux vs Servlet 혼용 문제
- ⚠️ 토큰 무효화 로직 호출 누락

**영향 범위**: ⭐⭐⭐
- 사용자 인증 기반 구축

---

### PR #79: 허브/업체/상품 서비스 통신 처리 ✅ ⭐⭐⭐

**Type**: Feature (FeignClient Integration)
**Service**: product-service
**Branch**: feature/#66-hub-company-product-communication
**Status**: Merged to dev
**Issue**: #66

**Completed Items**:
- ✅ HubClient, CompanyClient FeignClient 구현
- ✅ ProductService: Hub/Company 검증 로직 추가
- ✅ HubResponse, CompanyResponse DTO (record)
- ✅ Sample data: company.sql (업체 5개)

**Critical Issues** (from review):
- ⚠️ Product Entity 검증 로직 완전 삭제 (CustomException으로 복원 필요)
- ⚠️ SecurityConfig permitAll 설정 (프로덕션 위험)
- ⚠️ FeignClient 예외 처리 불완전 (타임아웃 등 미처리)

**영향 범위**: ⭐⭐⭐
- 서비스 간 통신 검증 구현

---

### PR #77: 배송 목록/검색 조회 기능 구현 ✅ ⭐⭐⭐

**Type**: Feature
**Service**: delivery-service
**Branch**: feature/#71-read-delivery-list-search
**Status**: Merged to dev
**Issue**: #71

**Completed Items**:
- ✅ JPA Specification 패턴 (DeliverySpecifications)
- ✅ DeliverySearchCond DTO (record): 5개 검색 조건
- ✅ DeliveryService.search() - 동적 쿼리
- ✅ GET /api/v1/deliveries - 목록/검색 조회 (페이징)
- ✅ DeliveryResponse: Builder → record 변환
- ✅ Controller 테스트: 5개 테스트 케이스

**영향 범위**: ⭐⭐⭐
- delivery-service 조회 기능 완성

---

## 2025-11-11

### Issue #35: Notification Service Kafka Consumer 구현 ✅ ⭐⭐⭐⭐

**Type**: Feature
**Service**: notification-service
**Branch**: feature/#35-notification-service-kafka-challenge
**Status**: Completed (Ready for PR)
**Issue**: #35

**핵심 구현 사항**:
- Kafka Event Consumer 2개 구현 (order.created, delivery.status.changed)
- 멱등성 보장 메커니즘 (event_id 기반 중복 검증)
- DB CHECK 제약조건 수정 (MessageType enum 확장)

**Completed Items**:
- ✅ OrderCreatedConsumer: 주문 생성 이벤트 수신 → Gemini AI → Slack 알림
- ✅ DeliveryStatusChangedConsumer: 배송 상태 변경 이벤트 수신 → Slack 알림
- ✅ KafkaConsumerConfig: 토픽별 별도 ContainerFactory 설정
- ✅ Event DTOs (record): OrderCreatedEvent, DeliveryStatusChangedEvent
- ✅ TopicProperties: @ConfigurationProperties로 토픽 관리
- ✅ Idempotency: event_id 기반 중복 처리 방지 (DB unique constraint)
- ✅ DB Schema: MessageType enum에 DELIVERY_STATUS_UPDATE 추가
- ✅ PostgreSQL CHECK constraint 수정: p_notifications_message_type_check
- ✅ Integration Tests: test-kafka-consumer.sh (4 scenarios, 4/4 통과)

**Technical Highlights**:
- ErrorHandlingDeserializer + JsonDeserializer 조합으로 JSON 파싱 에러 처리
- 트랜잭션 분리: DB 저장 (트랜잭션 내부) → Slack 발송 (트랜잭션 외부)
- Kafka 멱등성: Repository.existsByEventId() 체크로 중복 이벤트 skip
- Real Slack integration: C09QY22AMEE 채널 테스트 성공

**Test Results**:
- ✅ TEST 1: order.created 이벤트 처리 (Gemini + Slack)
- ✅ TEST 2: order.created 멱등성 검증 (중복 skip)
- ✅ TEST 3: delivery.status.changed 이벤트 처리 (Slack)
- ✅ TEST 4: delivery.status.changed 멱등성 검증 (중복 skip)

**영향 범위**: ⭐⭐⭐⭐
- notification-service의 핵심 기능 완성 (이벤트 기반 알림)
- order-service, delivery-service와의 비동기 통합 완성

**Next Steps**:
- Issue #76: Codex 리스크 개선 (통합 테스트 분리, NPE 위험 등)
- Issue #36: Daily route optimization (Challenge)

---

### PR #75: FeignClient 상태 코드 오류 해결 및 Hub-Company Client 통신 ✅ ⭐⭐⭐⭐⭐

**Type**: Critical Fix (전체 MSA 통신 기반)
**Service**: common-lib, company-service
**Branch**: feature/#74-feignclient-fix
**Status**: Merged to dev
**Issue**: #74

**핵심 문제 해결**:
- OpenFeign이 HTTP 상태 코드로 성공/실패 판단하는데, 팀 컨벤션 ApiResponse는 항상 200 OK 반환
- NotFound 에러도 HTTP 200 OK로 반환되어 FeignClient가 예외를 잡지 못함

**Completed Items**:
- ✅ GlobalExceptionHandler 수정: ResponseEntity<ApiResponse<Void>> 반환으로 실제 HTTP 상태 코드 반영
- ✅ company-service HubClient 구현: ApiResponse<HubResponse> 반환 타입
- ✅ CompanyService fetchHub() 메서드: Hub 검증 로직 추가 (FeignException.NotFound 처리)
- ✅ HubResponse DTO 추가: record 타입, 필드(id, name, address, lat, lon)

**Technical Decisions**:
- ResponseEntity + ApiResponse 패턴: HTTP 상태 코드 + 응답 바디 모두 활용
- GlobalExceptionHandler 모든 핸들러에 일관되게 적용

**영향 범위**: ⭐⭐⭐⭐⭐
- **전체 MSA 서비스 간 통신의 기반이 되는 중요한 개선**
- 다른 서비스(order, delivery, product 등)에도 동일하게 적용 필요

**Pending Issues** (from review):
- Controller 응답 타입 통일 필요 (팀 전체 표준화 논의)
- HubClientAdapter 패턴 적용 권장 (FeignException 처리 캡슐화)
- FeignClient Configuration 추가 (Timeout, Retry)

---

### PR #73: 배송 단건 조회 API 구현 ✅

**Type**: Feature
**Service**: delivery-service
**Branch**: feature/#70-delivery-single-read
**Status**: Merged to dev
**Issue**: #70

**Completed Items**:
- ✅ DeliveryResponse DTO (Builder 패턴, from() factory method)
- ✅ DeliveryService.getOne() 메서드
- ✅ DeliveryController GET /api/v1/deliveries/{deliveryId}
- ✅ CustomException(ErrorCode.DELIVERY_NOT_FOUND) 적용
- ✅ Controller 테스트 작성 (성공/실패 케이스)

**Critical Issues** (from review):
- Entity 타입 불일치 (String → UUID 변환 오버헤드)
- ApiResponse 미사용 (팀 표준 불일치)
- 중복 메서드 findByDeliveryId() (findById() 사용 권장)

---

### PR #67: 배송 생성 기능 및 Kafka 이벤트 수신 ✅

**Type**: Feature (Event-Driven Architecture)
**Service**: delivery-service
**Branch**: feature/#63-create-delivery
**Status**: Merged to dev
**Issue**: #63

**Completed Items**:
- ✅ Delivery Entity (UUID PK, Unique Constraint on order_id)
- ✅ DeliveryStatus Enum (7단계 상태 정의)
- ✅ Kafka Consumer: OrderCreatedConsumer - order.created 이벤트 수신
- ✅ DeliveryService.createIfAbsentFromOrder() - Idempotency 처리 (DB Unique + 애플리케이션 레벨 중복 체크)
- ✅ TopicProperties (@ConfigurationProperties로 topic 외부 설정)
- ✅ 테스트: 단위 테스트 (Idempotency), 통합 테스트 (Embedded Kafka)

**Technical Highlights**:
- ⭐⭐⭐⭐⭐ Idempotency 처리 우수: Kafka 메시지 중복 수신 대비
- Event-Driven Architecture 정확한 구현

**Critical Issues** (from review):
- BaseEntity 미적용 (감사 필드 없음)
- Entity 필드 타입 불일치 (hubId: String → UUID, staffId: String → Long)
- Repository 패턴 불일치 (JpaRepository 직접 상속)

---

### PR #65: 상품 기본 CRUD 구현 ✅

**Type**: Feature
**Service**: product-service
**Branch**: feature/#62-product-crud
**Status**: Merged to dev
**Issue**: #62

**Completed Items**:
- ✅ Product Entity (BaseEntity 상속, UUID PK, Soft Delete)
- ✅ Repository: DDD pattern (domain interface + infrastructure JpaRepository + RepositoryImpl)
- ✅ ProductService: Full CRUD + 페이징 검증 (size: 10, 30, 50)
- ✅ Request/Response DTOs (record 타입)
- ✅ ProductController: 5개 엔드포인트 (POST, PATCH, DELETE, GET by ID, GET search)
- ✅ SecurityConfig (SecurityConfigBase 상속)
- ✅ Swagger 문서화

**API Endpoints**:
1. POST /api/v1/products - Create
2. PATCH /api/v1/products/{productId} - Update
3. DELETE /api/v1/products/{productId} - Soft delete
4. GET /api/v1/products/{productId} - Get by ID
5. GET /api/v1/products - Search with pagination

**Critical Issues** (from review):
- FeignClient 미구현 (Hub/Company 검증 없음)
- Entity 예외 타입 불일치 (IllegalArgumentException → CustomException)
- Controller 응답 타입 혼용

---

## 2025-11-10

### PR #54: hub-service 허브 경로 CRUD + 다익스트라 최단 경로 구현 ✅

**Type**: Feature
**Service**: hub-service
**Branch**: feature/#45-create-hub-route
**Status**: Completed (Merged to dev)
**Issues**: #45, #46, #47

**Completed Items**:
- ✅ HubRoute Entity (DIRECT/RELAY RouteType, pathNodes JSON field)
- ✅ 허브 경로 CRUD APIs (등록, 수정, 삭제, 전체 조회, 단일 조회, 직통 경로 조회)
- ✅ 다익스트라 최단 경로 알고리즘 구현 (DijkstraService)
- ✅ Redis 3단계 캐싱 전략:
  - Direct route cache: `hub:route:from:{fromId}:to:{toId}`
  - Hub graph cache: `hub:graph:{hubId}` (adjacency list)
  - Shortest path cache: `hub:path:from:{fromId}:to:{toId}`
- ✅ HubRouteCacheService 분리 (캐싱 로직 전담)
- ✅ Bulk Hub 조회 최적화 (N+1 방지)
- ✅ Sample data: 17 hubs + 52 direct routes (hub.sql)
- ✅ docker-compose.yml 복구

**API Endpoints** (7개):
1. POST /api/v1/hub-routes - 허브 경로 등록
2. PUT /api/v1/hub-routes/{routeId} - 허브 경로 수정
3. DELETE /api/v1/hub-routes/{routeId} - 허브 경로 삭제
4. GET /api/v1/hub-routes - 허브 경로 전체 조회 (페이징)
5. GET /api/v1/hub-routes/{routeId} - 허브 경로 단일 조회
6. GET /api/v1/hub-routes/direct - 직통 경로 조회 (fromHubId, toHubId)
7. GET /api/v1/hub-routes/shortest - 최단 경로 조회 (다익스트라)

**Technical Patterns**:
- DDD repository pattern (domain interface + infrastructure impl)
- Factory methods: `createDirectRoute()`, `createRelayRoute()`
- Redis Pipeline for bulk operations
- Cache invalidation: RELAY routes deleted on DIRECT route update

**Review Notes** (from PR #54 review):
- Critical fixes needed: PriorityQueue comparator, visited Set for duplicate prevention
- TTL required for Redis cache to prevent memory accumulation
- Transaction boundary issue with cache synchronization
- Overall design and caching strategy excellent

---

### PR #52: company-service 업체 조회 API 구현 ✅

**Type**: Feature + Refactoring
**Service**: company-service
**Branch**: feature/#43-get-company
**Status**: Completed (Merged to dev)
**Issue**: #43

**Completed Items**:
- ✅ 업체 단건 조회 API (ID 기반)
- ✅ 업체 전체 검색 조회 API (이름 검색 + 페이징)
- ✅ 업체 수정 API (PATCH 방식 - partial update)
- ✅ 업체 삭제 API (soft delete)
- ✅ DTO 위치 이동: presentation.dto → application.dto (DDD 컨벤션)
- ✅ Repository pattern: Domain interface + Infrastructure JpaRepository + RepositoryImpl
- ✅ Response DTOs:
  - `CompanyDetailResponse`: audit 필드 포함
  - `CompanySearchResponse`: 기본 필드만
- ✅ Pagination 헬퍼 메서드 (size validation: 10/30/50)

**API Endpoints**:
1. GET /api/v1/companies/{companyId} - 업체 단건 조회
2. GET /api/v1/companies - 업체 전체 검색 조회 (이름 + 페이징)
3. PUT /api/v1/companies/{companyId} - 업체 수정
4. DELETE /api/v1/companies/{companyId} - 업체 삭제 (soft delete)

**Search Parameters**:
- companyName: String (부분 검색, optional)
- page: int (default 0)
- size: int (10/30/50, default 10)
- sortBy: String (default: createdAt)
- isAsc: boolean (default: false)

**Technical Patterns**:
- Record DTOs with static from() factory methods
- Soft delete filtering: `findByDeletedFalse()` pattern
- Entity partial update methods: `updateName()`, `updateType()`, `updateAddress()`
- Pageable validation and creation helper

**Review Notes** (from PR #52 review):
- Recommend sortBy field whitelist validation (security)
- DELETE response code consistency needed (204 vs 200)
- CompanyUpdateRequest validation enhancement suggested
- Overall: Excellent DDD structure and team convention adherence

---

## 2025-11-07

### Issue #14: notification-service REST API 구현 ✅

**Type**: Feature
**Service**: notification-service
**Branch**: feature/#14-notification-service-API
**Status**: Completed

**Completed Items**:
- ✅ Presentation DTOs: OrderNotificationRequest, ManualNotificationRequest, NotificationResponse, ExternalApiLogResponse (record pattern)
- ✅ Application Service: NotificationService (sendOrderNotification, sendManualNotification, getNotification)
- ✅ Application Service: ExternalApiLogService (getAllApiLogs, getApiLogsByProvider, getApiLogsByMessageId)
- ✅ Presentation Controller: NotificationController (7 API endpoints)
- ✅ User FeignClient: UserServiceClient, UserResponse
- ✅ ErrorCode 추가: NOTIFICATION_NOT_FOUND, NOTIFICATION_SEND_FAILED (common-lib)
- ✅ Swagger 의존성 추가: springdoc-openapi-starter-webmvc-ui
- ✅ Controller 테스트 작성: NotificationControllerTest (9 test cases)
- ✅ All existing tests passing: 35/35

**API Endpoints**:
1. POST /api/v1/notifications/order - 주문 알림 발송 (내부 서비스용)
2. POST /api/v1/notifications/manual - 수동 메시지 발송 (모든 인증 사용자)
3. GET /api/v1/notifications/{id} - 알림 단건 조회
4. GET /api/v1/notifications - 알림 목록 조회 (TODO: 페이징)
5. GET /api/v1/notifications/api-logs - 전체 API 로그 조회 (MASTER)
6. GET /api/v1/notifications/api-logs/provider/{provider} - Provider별 로그 조회 (MASTER)
7. GET /api/v1/notifications/api-logs/message/{messageId} - 메시지 ID별 로그 조회 (MASTER)

**Technical Patterns**:
- Record DTOs with static from() factory methods (hub-service pattern)
- Gemini AI integration for departure deadline calculation
- Slack API integration for message sending
- User snapshot pattern for manual notifications
- @PreAuthorize for role-based access control
- FeignClient for inter-service communication (UserServiceClient)

**Pending Tasks**:
- Pagination implementation (NotificationRepository + Controller)
- Integration tests with real Slack/Gemini APIs
- user-service User API implementation (for FeignClient)

---

### PR #48: notification-service 외부 API 클라이언트 구현 (Service Layer Refactoring) ✅

**Type**: Feature + Refactoring
**Service**: notification-service
**Branch**: feature/#13-external-api-client
**Status**: Ready to merge (All tests passing: 35/35)

**Completed Items**:
- ✅ Service layer refactoring: Moved ApiLogDomainService → ExternalApiLogService (application layer)
- ✅ Updated all 3 wrapper classes (SlackClientWrapper, GeminiClientWrapper, ChatGptClientWrapper)
- ✅ Fixed test environment: .env path correction (../.env) and Eureka disable in tests
- ✅ Docker environment setup: docker-compose-team.yml with Volume mount strategy
- ✅ Service port configuration: Added server.port to all services (eureka, gateway, hub, user, order, notification)
- ✅ Environment separation: .env (local) vs .env.docker (Docker network)

**Technical Decisions**:
- Application service pattern: Technical policies (API logging) belong in application layer, not domain
- Docker Volume mount: Allows hot-reload without rebuilding containers
- DTO pattern decision: Keep external API DTOs as `class` with Builder, use `record` for presentation DTOs

**Test Results**: 35/35 tests passed (100% success rate)
- Unit tests (MockWebServer): 6 tests
- Integration tests (real APIs): 3 tests
- Repository tests: 26 tests

**Key Learning from PR Review**:
- Service layer separation: application (technical orchestration) vs domain (business rules)
- DDD principle: Domain services should not depend on infrastructure concerns
- Team alignment: Follow hub-service patterns (PR #44) for consistency

---

## 2025-11-06

### PR #44: hub-service 허브 조회 API 구현 ✅

**Type**: Feature
**Service**: hub-service
**Merged**: commit 684d683 "feat: 허브 조회 api 추가 (#44)"

**Completed Items**:
- ✅ 허브 ID로 단일 조회 API 구현
- ✅ 허브 이름으로 단일 조회 API 구현
- ✅ 허브 전체 데이터 페이징 조회 API 구현
- ✅ Redis 캐시 로직 추가 (HubCacheService 분리)
- ✅ DDD 원칙에 따라 Repository 계층 분리 (domain/infrastructure)
- ✅ Service 계층을 application으로 이동 (캐시 로직 포함)

**Technical Patterns**:
- Domain repository interface + Infrastructure JpaRepository + RepositoryImpl
- Cache service separation pattern
- Application layer service pattern for business flow control

**Key Learning**:
- DDD repository pattern (domain interface + infrastructure impl) is now standard for all services
- Service layer separation: application (orchestration) vs domain (pure business logic)

---

### Issue #13: notification-service 외부 API 클라이언트 구현 ✅

**Type**: Feature
**Service**: notification-service
**Status**: Completed 2025-11-06

**Completed Items**:
- ✅ Slack API client (WebClient + Resilience4j, 3 retry attempts)
- ✅ Gemini API client (replaced ChatGPT, 2 retry attempts with exponential backoff)
- ✅ ApiLogDomainService (auto-logging with sensitive data masking)
- ✅ Wrapper pattern (SlackClientWrapper, GeminiClientWrapper)
- ✅ WebClient dependency injection refactoring (separate beans for testability)
- ✅ Unit tests with MockWebServer (6 tests, 100% pass)
- ✅ Integration tests with real APIs (3 tests, 100% pass)
- ✅ API key validation (Slack, Gemini)
- ✅ Environment configuration (application.yml, application-test.yml)

**Technical Decisions**:
- WebClient injection over WebClient.Builder for better testability
- Gemini gemini-2.5-flash-lite model for cost efficiency
- Wrapper pattern for automatic API call logging

**Test Results**: 35/35 tests passed (100% success rate)

---

## 2025-11-05

### Issue #33: notification-service 공통 설정 반영 ✅

**Type**: Configuration
**Service**: notification-service
**Merged**: commit 645a354 "fix: common config 적용 및 security 추가 (#34)"

**Completed Items**:
- ✅ JpaAuditConfig, SwaggerConfig를 common-lib로 통합
- ✅ SecurityConfig 구현 (SecurityConfigBase 상속)
- ✅ @Import 어노테이션으로 common-lib Config 등록
- ✅ Spring Security 의존성 추가
- ✅ 테스트 환경 Bean 오버라이드 설정
- ✅ Docker 환경 검증 (26 tests passed, containers healthy)

**Technical Decisions**:
- UserPrincipal-based auditing
- HeaderAuthFilter applied from common-lib

---

### Issue #12: notification-service DB Entity & Repository ✅

**Type**: Feature
**Service**: notification-service
**Merged**: commit 4a3028d "feat: 알림서비스 DB Entity & Repository 구현 (#31)"

**Completed Items**:
- ✅ Notification, ExternalApiLog 엔티티 생성
- ✅ ENUM 타입 정의 (SenderType, MessageType, MessageStatus, ApiProvider)
- ✅ Repository 인터페이스 및 구현체 작성
- ✅ Soft Delete 처리 (@SQLRestriction 적용)
- ✅ 엔티티 검증 메서드 구현 (validate, lifecycle hooks)
- ✅ 테스트 코드 작성 (26 tests, 100% pass rate)

**Key Features**:
- Snapshot pattern for sender information
- JSONB support for flexible data storage
- Domain-driven validation

---

### Issue #11: notification-service 초기 설정 ✅

**Type**: Setup
**Service**: notification-service
**Merged**: commit d3523cb "feat: 알림서비스 초기설정 (#26)"

**Completed Items**:
- ✅ Spring Boot 프로젝트 생성 (포트: 8700)
- ✅ 멀티모듈 settings.gradle에 notification-service 추가
- ✅ 기본 의존성 설정 (Spring Web, JPA, PostgreSQL, Eureka Client, OpenFeign, Security)
- ✅ application.yml 작성 (DB 스키마: notification_db, Eureka 설정)
- ✅ DDD 패키지 구조 생성 (presentation, application, domain, infrastructure, global)
- ✅ Dockerfile 작성
- ✅ README.md 작성

---

### PR #32: UserPrincipal 추가 및 hub-service 공통 설정 ✅

**Type**: Feature
**Service**: common-lib, hub-service
**Merged**: commit 5804cc0 "feat: UserPrincipal 추가 및 신규 허브 생성 API (#32)"

**Completed Items**:
- ✅ UserPrincipal, Role Enum 추가 (common-lib)
- ✅ SecurityConfigBase 추상 클래스 구현 (HeaderAuthFilter 자동 적용)
- ✅ JpaAuditConfig 개선 (SecurityContext 기반 auditing)
- ✅ SwaggerConfig 헤더 등록 (X-User-Id, X-User-Name, X-User-Role)
- ✅ hub-service에 공통 설정 적용

**Team Decision**: All services will use common-lib security configs

---

## Archive Policy

- Recent work (last 2 weeks): Keep in this file
- Older work: Can be archived to `docs/archive/completed-work-YYYY-MM.md` if needed