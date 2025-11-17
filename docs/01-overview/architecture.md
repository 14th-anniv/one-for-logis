# 아키텍처 설계

## MSA (Microservices Architecture) 개요

one-for-logis는 마이크로서비스 아키텍처를 기반으로 설계된 분산 시스템입니다.

### 설계 원칙
1. **서비스 독립성**: 각 서비스는 독립적으로 배포 및 확장 가능
2. **데이터 독립성**: PostgreSQL 스키마 분리 (서비스 간 DB 직접 접근 금지)
3. **최종 일관성**: 분산 트랜잭션 대신 이벤트 기반 일관성 보장
4. **장애 격리**: Circuit Breaker 패턴으로 장애 전파 방지

## 서비스 구성

### Infrastructure Services

#### 1. eureka-server (8761)
- **역할**: 서비스 디스커버리 및 등록
- **기술**: Spring Cloud Netflix Eureka
- **기능**:
  - 서비스 자동 등록/해제
  - 헬스 체크
  - 로드 밸런싱 정보 제공

#### 2. gateway-service (8000)
- **역할**: API Gateway, 인증
- **기술**: Spring Cloud Gateway
- **기능**:
  - JWT 인증 (Authorization 헤더 검증)
  - 사용자 컨텍스트 헤더 추가 (X-User-Id, X-User-Role, X-Hub-Id, X-Company-Id)
  - 라우팅 및 로드 밸런싱
  - CORS 처리
- **중요**: 권한 검사는 각 서비스에서 수행 (Gateway는 인증만)

#### 3. zipkin-server (9411)
- **역할**: 분산 추적
- **기술**: Zipkin
- **기능**:
  - 서비스 간 호출 추적
  - 성능 모니터링
  - 병목 지점 분석

### Business Services

#### 1. user-service (8100)
**담당 도메인**: 사용자 관리, 인증/인가

**주요 기능**:
- 회원가입 (PENDING 상태)
- 로그인 (JWT 발급)
- 사용자 승인 (MASTER/HUB_MANAGER)
- 마이페이지 조회

**의존성**:
- Redis (Refresh Token 저장)

**제공 API**:
- `POST /api/v1/auth/signup` - 회원가입
- `POST /api/v1/auth/login` - 로그인
- `GET /api/v1/users/me` - 마이페이지 조회
- `GET /api/v1/users/{userId}` - 사용자 조회

#### 2. hub-service (8200)
**담당 도메인**: 허브 및 경로 관리

**주요 기능**:
- 허브 CRUD
- 허브 경로 CRUD
- 다익스트라 알고리즘 최단 경로 계산
- Redis 3단계 캐싱

**의존성**:
- Redis (경로 캐싱)

**제공 API**:
- `POST /api/v1/hubs` - 허브 생성
- `GET /api/v1/hubs/{hubId}` - 허브 조회
- `PUT /api/v1/hubs/{hubId}` - 허브 수정
- `DELETE /api/v1/hubs/{hubId}` - 허브 삭제 (Soft Delete)
- `POST /api/v1/hub-routes` - 경로 생성
- `GET /api/v1/hub-routes/shortest-path` - 최단 경로 계산

**Redis 캐싱 전략**:
1. **Direct Route Cache**: `hub:route:from:{fromId}:to:{toId}` (TTL: 24h)
2. **Hub Graph Cache**: `hub:graph:{hubId}` (TTL: 24h)
3. **Shortest Path Cache**: `hub:path:from:{fromId}:to:{toId}` (TTL: 24h)

#### 3. company-service (8300)
**담당 도메인**: 업체 관리

**주요 기능**:
- 업체 CRUD
- 허브별 업체 조회/검색
- 페이징 지원

**의존성**:
- hub-service (허브 검증)

**제공 API**:
- `POST /api/v1/companies` - 업체 생성
- `GET /api/v1/companies/{companyId}` - 업체 조회
- `GET /api/v1/companies` - 업체 목록 조회 (페이징)

#### 4. product-service (8500)
**담당 도메인**: 상품 및 재고 관리

**주요 기능**:
- 상품 CRUD
- 재고 관리
- 업체별 상품 조회

**의존성**:
- hub-service (허브 검증)
- company-service (업체 검증)

**제공 API**:
- `POST /api/v1/products` - 상품 생성
- `GET /api/v1/products/{productId}` - 상품 조회
- `PUT /api/v1/products/{productId}` - 상품 수정
- `DELETE /api/v1/products/{productId}` - 상품 삭제

#### 5. order-service (8400)
**담당 도메인**: 주문 관리

**주요 기능**:
- 주문 생성 (Orchestration)
- 공급업체/수신업체/상품 검증
- 재고 가용성 체크
- 배송 생성 트리거

**의존성**:
- company-service (업체 검증)
- product-service (상품/재고 검증)
- delivery-service (배송 생성)
- notification-service (알림 발송)

**제공 API**:
- `POST /api/v1/orders` - 주문 생성

**주문 생성 Flow**:
```
1. company-service: 공급업체, 수신업체 검증
2. product-service: 상품 존재 및 재고 확인
3. order DB: 주문 저장
4. delivery-service: 배송 생성 (담당자 할당)
5. notification-service: 알림 발송 (AI 출발 시한 계산)
```

#### 6. delivery-service (8600)
**담당 도메인**: 배송 추적 및 담당자 관리

**주요 기능**:
- 배송 생성 (Kafka 이벤트: order.created)
- 배송 담당자 라운드로빈 할당
- 배송 상태 변경 (Kafka 이벤트 발행)
- 배송 조회/검색 (JPA Specification)

**의존성**:
- hub-service (경로 계산)
- Kafka (이벤트 수신/발행)

**제공 API**:
- `GET /api/v1/deliveries/{deliveryId}` - 배송 조회
- `GET /api/v1/deliveries` - 배송 목록 조회
- `PATCH /api/v1/deliveries/{deliveryId}/status` - 배송 상태 변경

**Kafka Events**:
- **Consumer**: `order.created` (주문 생성 → 배송 생성)
- **Producer**: `delivery.status.changed` (배송 상태 변경 → 알림)

#### 7. notification-service (8700)
**담당 도메인**: 알림 및 AI 통합

**주요 기능**:
- AI 기반 출발 시한 계산 (Google Gemini API)
- Slack 메시지 발송
- Kafka 이벤트 기반 자동 알림
- 외부 API 호출 로그 및 통계
- **Challenge**: 일일 배송 경로 최적화 (Naver Maps API + TSP)

**의존성**:
- user-service (발신자 정보 조회)
- Kafka (이벤트 수신)
- Slack API, Gemini API, Naver Maps API

**제공 API**:
- `POST /api/v1/notifications/order` - 주문 알림 (내부 API)
- `POST /api/v1/notifications/manual` - 수동 메시지
- `POST /api/v1/notifications/delivery-status` - 배송 상태 알림
- `GET /api/v1/notifications` - 알림 목록 조회
- `GET /api/v1/notifications/api-logs` - 외부 API 로그 조회
- `GET /api/v1/notifications/api-logs/stats` - API 통계 조회

**Kafka Events**:
- **Consumer**: `order.created` (주문 생성 → 알림)
- **Consumer**: `delivery.status.changed` (배송 상태 → 알림)

**외부 API 통합**:
1. **Slack API**: `chat.postMessage` (실시간 알림)
2. **Google Gemini API**: 출발 시한 계산, 경로 최적화 (TSP)
3. **Naver Maps API**: 경유지 기반 경로 계산 (Challenge 기능)

## 서비스 간 통신 패턴

### 1. 동기 통신 (FeignClient)
```
order-service → company-service (업체 검증)
order-service → product-service (상품 검증)
order-service → delivery-service (배송 생성)
notification-service → user-service (사용자 정보 조회)
company-service → hub-service (허브 검증)
```

**특징**:
- Spring Cloud OpenFeign 사용
- Circuit Breaker 패턴 (Fallback)
- Retry 로직 (Exponential Backoff)
- 타임아웃 설정

### 2. 비동기 통신 (Kafka)
```
order-service → [order.created] → delivery-service, notification-service
delivery-service → [delivery.status.changed] → notification-service
```

**특징**:
- 이벤트 기반 최종 일관성
- 멱등성 보장 (event_id 중복 체크)
- At-Least-Once Delivery

## 데이터 아키텍처

### PostgreSQL 스키마 분리
```
PostgreSQL Server
├── user_db (p_users)
├── hub_db (p_hubs, p_hub_routes)
├── company_db (p_companies)
├── product_db (p_products)
├── order_db (p_orders, p_order_items)
├── delivery_db (p_deliveries, p_delivery_routes, p_delivery_personnel)
└── notification_db (p_notifications, p_external_api_logs)
```

**설계 원칙**:
1. 서비스별 독립 스키마 (MSA 원칙)
2. 서비스 간 DB 직접 접근 금지 (REST API 통신만 허용)
3. 공통 필드: created_at, created_by, updated_at, updated_by, deleted_at, deleted_by
4. Soft Delete 패턴 (물리 삭제 금지)

### Redis 활용
```
user-service: Refresh Token 저장
hub-service: 경로 캐싱 (3단계)
```

## 보안 아키텍처

### JWT 인증 플로우
```
1. Client → user-service: POST /auth/login
2. user-service → Client: Access Token (15min) + Refresh Token (7days)
3. Client → gateway-service: Authorization: Bearer {token}
4. gateway-service: JWT 검증
5. gateway-service: 사용자 컨텍스트 헤더 추가 (X-User-Id, X-User-Role, etc.)
6. gateway-service → {service}: 헤더 전달
7. {service}: @PreAuthorize 권한 검사
```

### 권한 계층
```
MASTER (최상위)
  └── HUB_MANAGER (허브 관리자)
        └── DELIVERY_MANAGER (배송 담당자)
              └── COMPANY_MANAGER (업체 관리자)
```

## 모니터링 및 관찰성

### Zipkin 분산 추적
- 모든 서비스 간 호출 추적
- Trace ID, Span ID 자동 생성
- 성능 병목 지점 분석

### Actuator 헬스 체크
- `/actuator/health`: 서비스 상태
- `/actuator/info`: 서비스 정보

### 외부 API 로그 (notification-service)
- 모든 외부 API 호출 자동 로깅
- 성공/실패, 응답 시간, 비용 추적
- 통계 API 제공

## 배포 아키텍처

### Docker Compose
```
docker-compose-dev.yml
├── postgres (5432)
├── redis (6379)
├── kafka (9092)
├── zookeeper (2181)
├── zipkin (9411)
├── eureka-server (8761)
├── gateway-service (8000)
├── user-service (8100)
├── hub-service (8200)
├── company-service (8300)
├── order-service (8400)
├── product-service (8500)
├── delivery-service (8600)
└── notification-service (8700)
```

**특징**:
- Volume Mount 방식 (로컬 개발)
- 자동 재시작 (depends_on)
- 헬스 체크
- 네트워크 격리 (logis-network)

## 확장성 고려사항

### 수평 확장 (Horizontal Scaling)
- Eureka를 통한 자동 로드 밸런싱
- Stateless 서비스 설계
- Redis를 통한 세션 공유

### 수직 확장 (Vertical Scaling)
- Docker 리소스 제한 설정
- JVM 힙 메모리 튜닝

### 데이터베이스 확장
- PostgreSQL Read Replica 추가 (읽기 부하 분산)
- 파티셔닝 (시간 기반)

## 장애 복구 전략

### Circuit Breaker
- FeignClient Fallback 패턴
- 장애 격리 (Cascading Failure 방지)

### Retry 로직
- Exponential Backoff
- 최대 재시도 횟수 제한

### Saga 패턴 (Future)
- 주문 생성 실패 시 보상 트랜잭션
- 이벤트 소싱 기반 롤백

## 참고 문서
- [팀 컨벤션](./team-conventions.md)
- [데이터베이스 스키마](../02-development/database-schema.md)
- [비즈니스 규칙](../02-development/business-rules.md)
- [테스트 가이드](../04-testing/testing-guide.md)
