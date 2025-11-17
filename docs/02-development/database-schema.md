# Database Schema

This document describes the database architecture and key table schemas for the 14logis project.

**Last Updated**: 2025-11-10

---

## Database Architecture

### Schema Separation
- **PostgreSQL** with separate schemas per service (not separate DB instances)
- All schemas exist in the same PostgreSQL database: `oneforlogis`
- No cross-service DB access - all data exchange via REST API only

### Schema Naming Convention
```
user_db          → user-service
hub_db           → hub-service
company_db       → company-service
product_db       → product-service
order_db         → order-service
delivery_db      → delivery-service
notification_db  → notification-service
```

### Table Naming Convention
- All tables have `p_` prefix (e.g., `p_users`, `p_hubs`, `p_notifications`)
- Consistent naming: singular form for domain entities

### Primary Key Strategy
- **Default**: UUID
- **Exceptions**:
  - **`p_user`**: **BIGINT (username as VARCHAR PRIMARY KEY)** - IMPORTANT: User primary key is username (VARCHAR), user_id is internal BIGINT field
  - **`p_hub_route`**: BIGINT (id)
  - **`p_delivery_staff`**: BIGINT (staff_id)

---

## Common Patterns

### Audit Fields (All Tables)

Every table includes these audit fields:

```sql
created_at   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
created_by   VARCHAR(100)    NOT NULL
updated_at   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
updated_by   VARCHAR(100)    NOT NULL
deleted_at   TIMESTAMP       NULL                              -- Soft delete
deleted_by   VARCHAR(100)    NULL                              -- Soft delete
```

### Soft Delete Pattern

- DELETE operations set `deleted_at` and `deleted_by` (never physical deletion)
- All queries must filter `WHERE deleted_at IS NULL`
- Use `@SQLRestriction("deleted_at IS NULL")` in JPA entities
- Soft delete preserves referential integrity and audit trails

### Pagination & Search

- All entities support CRUD + Search operations
- Default sort fields: `created_at`, `updated_at` (ASC/DESC)
- Page size options: 10, 30, 50 (default: 10)
- Use Spring Data Pageable for consistency

---

## Table Schemas by Service

### user_db (user-service)

#### p_user
사용자 인증 및 권한 관리.

```sql
CREATE TABLE p_user (
    username    VARCHAR(100) PRIMARY KEY,     -- 사용자 ID (4-10자, 소문자+숫자)
    password    VARCHAR(255) NOT NULL,        -- BCrypt 암호화
    name        VARCHAR(100) NOT NULL,        -- 실명
    email       VARCHAR(255) UNIQUE,          -- 이메일 (선택)
    slack_id    VARCHAR(100) NOT NULL,        -- Slack ID (알림용)
    role        VARCHAR(50) NOT NULL,         -- ENUM: MASTER, HUB_MANAGER, DELIVERY_MANAGER, COMPANY_MANAGER
    status      VARCHAR(50) NOT NULL DEFAULT 'PENDING',  -- ENUM: PENDING, APPROVED, REJECTED
    hub_id      UUID,                         -- 소속 허브 ID (HUB_MANAGER, DELIVERY_MANAGER)
    company_id  UUID,                         -- 소속 업체 ID (COMPANY_MANAGER, DELIVERY_MANAGER)
    is_public   BOOLEAN DEFAULT TRUE,         -- 사용자 정보 공개 여부
    -- Audit fields
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(100) NOT NULL,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by  VARCHAR(100) NOT NULL,
    deleted_at  TIMESTAMP,
    deleted_by  VARCHAR(100)
);

CREATE INDEX idx_user_role ON p_user(role, deleted_at);
CREATE INDEX idx_user_status ON p_user(status, deleted_at);
CREATE INDEX idx_user_hub ON p_user(hub_id, deleted_at);
CREATE INDEX idx_user_company ON p_user(company_id, deleted_at);
```

**Role ENUM**:
- `MASTER`: 마스터 관리자 (전체 CRUD 권한)
- `HUB_MANAGER`: 허브 관리자 (담당 허브 CRUD 권한)
- `DELIVERY_MANAGER`: 배송 담당자 (본인 배송 수정, 읽기 권한)
- `COMPANY_MANAGER`: 업체 담당자 (본인 업체 CRUD, 주문 생성 권한)

**Status ENUM**:
- `PENDING`: 승인 대기 (기본값)
- `APPROVED`: 승인 완료 (로그인 가능)
- `REJECTED`: 승인 거부

**Business Rules**:
- 회원가입 시 status는 PENDING
- MASTER 또는 HUB_MANAGER가 승인해야 APPROVED로 변경
- APPROVED 상태만 로그인 가능
- JWT에 username, role, hub_id, company_id 포함

---

### hub_db (hub-service)

#### p_hub
허브 정보 관리 (17개 센터).

```sql
CREATE TABLE p_hub (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL UNIQUE,    -- 허브명 (예: 서울허브, 대전허브)
    address     VARCHAR(255) NOT NULL,           -- 도로명 주소
    lat         DECIMAL(10,6) NOT NULL,          -- 위도
    lon         DECIMAL(10,6) NOT NULL,          -- 경도
    -- Audit fields
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(100) NOT NULL,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by  VARCHAR(100) NOT NULL,
    deleted_at  TIMESTAMP,
    deleted_by  VARCHAR(100)
);

CREATE INDEX idx_hub_name ON p_hub(name, deleted_at);
```

**Key Points**:
- 17개 허브 고정 (초기 데이터)
- 위도/경도: 지도 매핑, 거리 계산 기준
- Redis 캐싱 적용 (TTL: 24시간)

#### p_hub_route
허브 간 경로 정보 (직통 경로 + 최단 경로 계산).

```sql
CREATE TABLE p_hub_route (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_hub_id     UUID NOT NULL,               -- 출발 허브 (p_hub.id FK)
    to_hub_id       UUID NOT NULL,               -- 도착 허브 (p_hub.id FK)
    route_time      INTEGER NOT NULL,            -- 예상 소요 시간 (분)
    route_distance  DECIMAL(8,2) NOT NULL,       -- 거리 (km)
    route_type      VARCHAR(20) NOT NULL,        -- ENUM: DIRECT, RELAY
    -- Audit fields
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100) NOT NULL,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP,
    deleted_by      VARCHAR(100)
);

CREATE INDEX idx_route_from_to ON p_hub_route(from_hub_id, to_hub_id, deleted_at);
CREATE INDEX idx_route_type ON p_hub_route(route_type, deleted_at);
```

**Route Type ENUM**:
- `DIRECT`: 직통 연결 경로
- `RELAY`: 중계 허브를 거치는 경로 (다익스트라 계산 결과)

**Key Features** (PR #54):
- 직통 경로 우선 조회 → 없으면 다익스트라 최단 경로 계산
- Redis 3단계 캐싱:
  1. Direct route: `hub:route:from:{fromId}:to:{toId}`
  2. Hub graph: `hub:graph:{hubId}` (adjacency list)
  3. Shortest path: `hub:path:from:{fromId}:to:{toId}`
- Sample data: 17 hubs + 52 direct routes

---

### company_db (company-service)

#### p_company
공급업체 및 수령업체 관리.

```sql
CREATE TABLE p_company (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,           -- 업체명
    type        VARCHAR(20) NOT NULL,            -- ENUM: SUPPLIER, RECEIVER
    hub_id      UUID NOT NULL,                   -- 소속 허브 ID (p_hub.id FK, logical)
    address     VARCHAR(255) NOT NULL,           -- 업체 주소
    -- Audit fields
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(100) NOT NULL,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by  VARCHAR(100) NOT NULL,
    deleted_at  TIMESTAMP,
    deleted_by  VARCHAR(100)
);

CREATE INDEX idx_company_name ON p_company(name, deleted_at);
CREATE INDEX idx_company_hub ON p_company(hub_id, deleted_at);
CREATE INDEX idx_company_type ON p_company(type, deleted_at);
```

**Company Type ENUM**:
- `SUPPLIER`: 생산업체 (공급자)
- `RECEIVER`: 수령업체 (수취인)

**Business Rules** (PR #52):
- 모든 업체는 특정 허브에 소속
- hub_id는 hub-service API로 검증 (FeignClient)
- 검색: 업체명 부분 검색 + 페이징 (10/30/50)

**Permissions**:
| Role | Create | Read | Update | Delete |
|------|--------|------|--------|--------|
| MASTER | ✅ | ✅ | ✅ | ✅ |
| HUB_MANAGER | ✅ (담당 허브) | ✅ | ✅ (담당 허브) | ✅ (담당 허브) |
| DELIVERY_MANAGER | ❌ | ✅ | ❌ | ❌ |
| COMPANY_MANAGER | ❌ | ✅ | ✅ (본인 업체) | ❌ |

---

### product_db (product-service)

#### p_product
상품 및 재고 관리.

```sql
CREATE TABLE p_product (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,           -- 상품명
    quantity    INTEGER NOT NULL,                -- 재고 수량 (default 고민)
    price       DECIMAL(15,2) NOT NULL,          -- 단가
    hub_id      UUID NOT NULL,                   -- 상품 관리 허브 ID (p_hub.id FK, logical)
    company_id  UUID NOT NULL,                   -- 소속 업체 ID (p_company.id FK, logical)
    -- Audit fields
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(100) NOT NULL,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by  VARCHAR(100) NOT NULL,
    deleted_at  TIMESTAMP,
    deleted_by  VARCHAR(100)
);

CREATE INDEX idx_product_name ON p_product(name, deleted_at);
CREATE INDEX idx_product_hub ON p_product(hub_id, deleted_at);
CREATE INDEX idx_product_company ON p_product(company_id, deleted_at);
```

**Business Rules**:
- 모든 상품은 특정 업체와 허브에 소속
- company_id는 company-service API로 검증
- hub_id는 hub-service API로 검증
- 주문 생성 시 quantity 감소, 주문 취소 시 quantity 증가
- quantity < 0 불가

**Permissions**:
| Role | Create | Read | Update | Delete |
|------|--------|------|--------|--------|
| MASTER | ✅ | ✅ | ✅ | ✅ |
| HUB_MANAGER | ✅ (담당 허브) | ✅ (담당 허브) | ✅ (담당 허브) | ✅ (담당 허브) |
| DELIVERY_MANAGER | ❌ | ✅ | ❌ | ❌ |
| COMPANY_MANAGER | ✅ (본인 업체) | ✅ | ✅ (본인 업체) | ❌ |

---

### order_db (order-service)

#### p_order
주문 정보 관리.

```sql
CREATE TABLE p_order (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_no              VARCHAR(30) NOT NULL UNIQUE,    -- 주문번호
    user_id               BIGINT NOT NULL,                -- 주문 생성 사용자 (p_user 참조)
    supplier_company_id   UUID NOT NULL,                  -- 공급업체 ID (p_company.id FK, logical)
    receiver_company_id   UUID NOT NULL,                  -- 수령업체 ID (p_company.id FK, logical)
    status                VARCHAR(20) NOT NULL,           -- ENUM: PENDING, PAID, PACKING, SHIPPED, DELIVERED, CANCELED
    delivery_id           UUID,                           -- 배송 ID (nullable)
    items_count           INTEGER NOT NULL,               -- 주문 상품 개수
    total_amount          NUMERIC(18,2) NOT NULL,         -- 주문 총금액
    request_note          TEXT,                           -- 요청사항 (nullable)
    -- Audit fields
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(100) NOT NULL,
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by            VARCHAR(100) NOT NULL,
    deleted_at            TIMESTAMP,
    deleted_by            VARCHAR(100)
);

CREATE INDEX idx_order_user ON p_order(user_id, deleted_at);
CREATE INDEX idx_order_supplier ON p_order(supplier_company_id, deleted_at);
CREATE INDEX idx_order_receiver ON p_order(receiver_company_id, deleted_at);
CREATE INDEX idx_order_status ON p_order(status, deleted_at);
CREATE INDEX idx_order_no ON p_order(order_no, deleted_at);
```

**Order Status ENUM**:
- `PENDING`: 주문 접수
- `PAID`: 결제 완료
- `PACKING`: 출고 준비
- `SHIPPED`: 배송 중
- `DELIVERED`: 배송 완료
- `CANCELED`: 주문 취소

#### p_order_item
주문 항목 (상품별 라인).

```sql
CREATE TABLE p_order_item (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID NOT NULL,                  -- p_order.id FK
    product_id   UUID NOT NULL,                  -- p_product.id (logical FK)
    product_name VARCHAR(255) NOT NULL,          -- 상품명 스냅샷
    unit_price   NUMERIC(18,2) NOT NULL,         -- 단가
    quantity     INTEGER NOT NULL,               -- 수량
    line_total   NUMERIC(18,2) NOT NULL,         -- 항목 합계 (단가 × 수량)
    -- Audit fields
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(100) NOT NULL,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by   VARCHAR(100) NOT NULL,
    deleted_at   TIMESTAMP,
    deleted_by   VARCHAR(100)
);

CREATE INDEX idx_order_item_order ON p_order_item(order_id, deleted_at);
CREATE INDEX idx_order_item_product ON p_order_item(product_id, deleted_at);
```

#### p_order_status_history
주문 상태 변경 이력.

```sql
CREATE TABLE p_order_status_history (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID NOT NULL,                   -- p_order.id FK
    from_status VARCHAR(20),                     -- 변경 전 상태 (nullable)
    to_status   VARCHAR(20) NOT NULL,            -- 변경 후 상태
    reason      TEXT,                            -- 상태 변경 사유 (nullable)
    -- Audit fields
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(100) NOT NULL,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by  VARCHAR(100) NOT NULL,
    deleted_at  TIMESTAMP,
    deleted_by  VARCHAR(100)
);

CREATE INDEX idx_status_history_order ON p_order_status_history(order_id, created_at DESC);
```

**Order Creation Flow**:
1. Validate supplier/receiver companies (company-service)
2. Check product inventory (product-service)
3. Create order + order items
4. Call delivery-service (create delivery + route logs)
5. Delivery-service assigns personnel (round-robin)
6. Call notification-service (AI calculates departure time, sends Slack)

---

### delivery_db (delivery-service)

#### p_delivery_staff
배송 담당자 관리 (Round-robin 할당).

```sql
CREATE TABLE p_delivery_staff (
    staff_id     BIGINT PRIMARY KEY,
    user_id      BIGINT NOT NULL,                -- p_user 참조 (logical FK)
    hub_id       UUID,                           -- 소속 허브 (nullable for hub staff)
    staff_type   VARCHAR(20) NOT NULL,           -- ENUM: HUB_STAFF, COMPANY_STAFF
    slack_id     VARCHAR(100) NOT NULL,          -- Slack ID
    assign_order INTEGER NOT NULL,               -- Round-robin 순서 (0-9)
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,  -- 활성 상태
    delivery_id  UUID,                           -- 현재 배송 ID (nullable)
    -- Audit fields
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(100) NOT NULL,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by   VARCHAR(100) NOT NULL,
    deleted_at   TIMESTAMP,
    deleted_by   VARCHAR(100)
);

CREATE UNIQUE INDEX idx_staff_assign_order ON p_delivery_staff(hub_id, assign_order, deleted_at);
CREATE INDEX idx_staff_user ON p_delivery_staff(user_id, deleted_at);
CREATE INDEX idx_staff_slack ON p_delivery_staff(slack_id, deleted_at);
```

**Staff Type ENUM**:
- `HUB_STAFF`: 허브 직원 (hub_id nullable, 10명 총)
- `COMPANY_STAFF`: 업체 직원 (hub_id required, 허브당 10명)

**Assignment Logic**:
- Sequential assignment using `assign_order` (0-9, wrap to 0)
- New personnel: `max(assign_order) + 1`
- Soft delete preserves ordering (no rearrangement)

#### p_delivery_route_log
배송 경로 단계 추적.

```sql
CREATE TABLE p_delivery_route_log (
    route_id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_id        UUID NOT NULL,              -- 배송 ID (logical FK)
    step_order         INTEGER NOT NULL,           -- 순서 번호 (1, 2, 3, ...)
    route_status       VARCHAR(50) NOT NULL,       -- ENUM: WAITING, IN_TRANSIT, COMPLETED
    hub_id             UUID,                       -- 허브 ID (nullable for non-hub steps)
    delivery_staff_id  BIGINT,                     -- 담당자 ID (nullable for hub-to-hub)
    occurred_at        TIMESTAMP NOT NULL,         -- 발생 시각
    note               TEXT,                       -- 비고 (nullable)
    -- Audit fields
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(100) NOT NULL,
    deleted_at         TIMESTAMP,
    deleted_by         VARCHAR(100)
);

CREATE INDEX idx_route_log_delivery ON p_delivery_route_log(delivery_id, step_order, deleted_at);
CREATE INDEX idx_route_log_hub ON p_delivery_route_log(hub_id, deleted_at);
```

**Route Status ENUM**:
- `WAITING`: 대기
- `IN_TRANSIT`: 배송 중
- `COMPLETED`: 완료

---

### notification_db (notification-service)

#### p_notifications
알림 메시지 이력 (Snapshot Pattern).

```sql
CREATE TABLE p_notifications (
    message_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_type        VARCHAR(20) NOT NULL,       -- ENUM: USER, SYSTEM
    sender_username    VARCHAR(50),                -- nullable for SYSTEM
    sender_slack_id    VARCHAR(50),                -- nullable for SYSTEM
    sender_name        VARCHAR(100),               -- nullable for SYSTEM
    recipient_slack_id VARCHAR(50) NOT NULL,       -- 수신자 Slack ID
    recipient_name     VARCHAR(100) NOT NULL,      -- 수신자 이름 스냅샷
    message_content    TEXT NOT NULL,              -- 메시지 내용
    message_type       VARCHAR(30) NOT NULL,       -- ENUM: ORDER_NOTIFICATION, DAILY_ROUTE, DELIVERY_STATUS_UPDATE, MANUAL
    reference_id       UUID,                       -- 연관 엔티티 ID (nullable)
    sent_at            TIMESTAMP,                  -- 발송 완료 시각 (nullable)
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- ENUM: PENDING, SENT, FAILED
    error_message      TEXT,                       -- 발송 실패 에러 메시지 (nullable)
    -- Audit fields
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(100),
    updated_at         TIMESTAMP,
    updated_by         VARCHAR(100),
    deleted_at         TIMESTAMP,
    deleted_by         VARCHAR(100),

    CONSTRAINT chk_sender_info CHECK (
        (sender_type = 'SYSTEM' AND sender_username IS NULL) OR
        (sender_type = 'USER' AND sender_username IS NOT NULL)
    ),
    CONSTRAINT chk_sent_at CHECK (
        (status = 'SENT' AND sent_at IS NOT NULL) OR
        (status != 'SENT')
    )
);

CREATE INDEX idx_notifications_recipient ON p_notifications(recipient_slack_id, deleted_at);
CREATE INDEX idx_notifications_type ON p_notifications(message_type, deleted_at);
CREATE INDEX idx_notifications_status ON p_notifications(status, deleted_at);
CREATE INDEX idx_notifications_created_at ON p_notifications(created_at DESC);
CREATE INDEX idx_notifications_reference ON p_notifications(reference_id, deleted_at);
```

**Sender Type ENUM**:
- `USER`: 사용자가 발송한 메시지
- `SYSTEM`: 시스템 자동 발송 메시지

**Message Type ENUM**:
- `ORDER_NOTIFICATION`: 주문 생성 시 출발 마감시간 알림
- `DAILY_ROUTE`: 일일 경로 최적화 알림 (Challenge)
- `MANUAL`: 사용자 수동 메시지

**Status ENUM**:
- `PENDING`: 발송 대기중
- `SENT`: 발송 완료
- `FAILED`: 발송 실패

**Key Points**:
- Snapshot pattern: 발신자 정보를 발송 시점에 저장 (불변)
- SYSTEM 메시지: sender 필드들 NULL
- USER 메시지: sender 필드들 NOT NULL (constraint 적용)

#### p_external_api_logs
외부 API 호출 로그 (Slack, Gemini, Naver Maps).

```sql
CREATE TABLE p_external_api_logs (
    log_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    api_provider   VARCHAR(30) NOT NULL,          -- ENUM: SLACK, GEMINI, NAVER_MAPS
    api_method     VARCHAR(100) NOT NULL,         -- API 메서드/엔드포인트
    request_data   JSONB,                         -- 요청 데이터 (JSON, nullable)
    response_data  JSONB,                         -- 응답 데이터 (JSON, nullable)
    http_status    INTEGER,                       -- HTTP 상태 코드 (nullable)
    is_success     BOOLEAN NOT NULL DEFAULT FALSE,-- 성공 여부
    error_code     VARCHAR(50),                   -- 에러 코드 (nullable)
    error_message  TEXT,                          -- 에러 메시지 (nullable)
    duration_ms    INTEGER,                       -- 응답 시간 (밀리초, nullable)
    cost           DECIMAL(10,4),                 -- API 호출 비용 (USD, nullable)
    called_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 호출 일시
    message_id     UUID                           -- 연관 알림 메시지 ID (nullable, logical FK)
);

CREATE INDEX idx_api_logs_provider ON p_external_api_logs(api_provider, called_at DESC);
CREATE INDEX idx_api_logs_success ON p_external_api_logs(is_success, called_at DESC);
CREATE INDEX idx_api_logs_called_at ON p_external_api_logs(called_at DESC);
CREATE INDEX idx_api_logs_message_id ON p_external_api_logs(message_id);
CREATE INDEX idx_api_logs_duration ON p_external_api_logs(duration_ms);
```

**API Provider ENUM**:
- `SLACK`: Slack API (chat.postMessage)
- `GEMINI`: Google Gemini API (chat/completions)
- `NAVER_MAPS`: Naver Maps Directions 5 API

**Key Points**:
- JSONB for flexible request/response storage
- Automatic logging via ExternalApiLogService
- Sensitive data (API keys) masked before storage
- No soft delete (로그성 데이터)

#### p_company_delivery_routes (Challenge 기능)
AI 계산 일일 최적 배송 경로.

```sql
CREATE TABLE p_company_delivery_routes (
    route_id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_id             UUID NOT NULL,              -- 배송 ID (logical FK)
    departure_hub_id        UUID NOT NULL,              -- 출발 허브 ID (logical FK)
    receiver_company_id     UUID NOT NULL,              -- 수취 업체 ID (logical FK)
    estimated_distance_km   DECIMAL(10,2),              -- 예상 거리 (km, nullable)
    estimated_duration_min  INTEGER,                    -- 예상 소요 시간 (분, nullable)
    actual_distance_km      DECIMAL(10,2),              -- 실제 거리 (km, nullable)
    actual_duration_min     INTEGER,                    -- 실제 소요 시간 (분, nullable)
    delivery_sequence       INTEGER NOT NULL,           -- AI 계산 최적 방문 순서 (1, 2, 3...)
    current_status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',  -- ENUM: PENDING, IN_TRANSIT, COMPLETED, FAILED
    delivery_staff_id       BIGINT,                     -- 배송 담당자 ID (nullable, logical FK)
    -- Audit fields
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(100),
    updated_at              TIMESTAMP,
    updated_by              VARCHAR(100),
    deleted_at              TIMESTAMP,
    deleted_by              VARCHAR(100)
);

CREATE INDEX idx_company_routes_delivery ON p_company_delivery_routes(delivery_id, deleted_at);
CREATE INDEX idx_company_routes_hub ON p_company_delivery_routes(departure_hub_id, deleted_at);
CREATE INDEX idx_company_routes_staff ON p_company_delivery_routes(delivery_staff_id, deleted_at);
CREATE INDEX idx_company_routes_sequence ON p_company_delivery_routes(delivery_staff_id, delivery_sequence, deleted_at);
CREATE INDEX idx_company_routes_status ON p_company_delivery_routes(current_status, deleted_at);
```

**Current Status ENUM**:
- `PENDING`: 배송 대기
- `IN_TRANSIT`: 배송 중
- `COMPLETED`: 배송 완료
- `FAILED`: 배송 실패

**Key Features** (Challenge):
- AI (Gemini) TSP 최적화 + Naver Maps 경로 정보
- 일일 06:00 스케줄러 실행
- delivery_sequence: 최적 방문 순서

---

## Table Relationships

### Cross-Service Logical FKs

**IMPORTANT**: All cross-service relationships are **logical foreign keys only** (not enforced by DB constraints). Data integrity is maintained through API validation.

```
p_user (user_db)
  └─> hub_id, company_id (logical FKs)

p_company (company_db)
  └─> hub_id (validated via hub-service FeignClient)

p_product (product_db)
  ├─> hub_id (validated via hub-service)
  └─> company_id (validated via company-service)

p_order (order_db)
  ├─> user_id (logical FK to p_user)
  ├─> supplier_company_id, receiver_company_id (validated via company-service)
  └─> delivery_id (created by delivery-service)

p_order_item (order_db)
  ├─> order_id (FK to p_order)
  └─> product_id (validated via product-service)

p_delivery_staff (delivery_db)
  ├─> user_id (logical FK to p_user)
  └─> hub_id (validated via hub-service)

p_delivery_route_log (delivery_db)
  ├─> delivery_id (logical FK)
  ├─> hub_id (validated via hub-service)
  └─> delivery_staff_id (FK to p_delivery_staff)

p_notifications (notification_db)
  └─> reference_id (logical FK to p_order, p_delivery, etc.)

p_external_api_logs (notification_db)
  └─> message_id (logical FK to p_notifications)

p_company_delivery_routes (notification_db, Challenge)
  ├─> delivery_id (logical FK)
  ├─> departure_hub_id (validated via hub-service)
  ├─> receiver_company_id (validated via company-service)
  └─> delivery_staff_id (logical FK to p_delivery_staff)
```

---

## Database Connection

### Local Development (Docker)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/oneforlogis
    username: root
    password: root1234!
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update  # Use 'validate' in production
    properties:
      hibernate:
        format_sql: true
        show_sql: false
        default_schema: {service}_db  # e.g., notification_db
```

### Docker Compose Configuration

```yaml
postgres:
  image: postgres:17-alpine
  environment:
    POSTGRES_DB: oneforlogis
    POSTGRES_USER: root
    POSTGRES_PASSWORD: root1234!
  ports:
    - "5432:5432"
  volumes:
    - postgres_data:/var/lib/postgresql/data
    - ./scripts/init-databases.sql:/docker-entrypoint-initdb.d/init.sql
```

### Schema Initialization Script

```sql
-- scripts/init-databases.sql
CREATE SCHEMA IF NOT EXISTS user_db;
CREATE SCHEMA IF NOT EXISTS hub_db;
CREATE SCHEMA IF NOT EXISTS company_db;
CREATE SCHEMA IF NOT EXISTS product_db;
CREATE SCHEMA IF NOT EXISTS order_db;
CREATE SCHEMA IF NOT EXISTS delivery_db;
CREATE SCHEMA IF NOT EXISTS notification_db;
```

---

## Migration Strategy

### Current Approach
- JPA `ddl-auto: update` for development
- Manual schema verification before production
- Sample data: SQL scripts in `scripts/` directory

### Future Consideration
- Flyway or Liquibase for versioned migrations
- Separate migration scripts per service
- Rollback strategies for schema changes

---

## Indexing Strategy

### Standard Indexes (All Tables)
1. Primary Key (automatic B-tree index)
2. Foreign Keys (manual, for join performance)
3. `deleted_at` (for soft delete queries with composite indexes)
4. Common query filters (status, type, created_at DESC)

### Composite Indexes
- Most indexes include `deleted_at` for soft delete filtering
- Example: `idx_notifications_recipient(recipient_slack_id, deleted_at)`
- Reason: WHERE clauses typically include `deleted_at IS NULL`

### Performance Monitoring
- Use EXPLAIN ANALYZE for query optimization
- Monitor slow query log in PostgreSQL
- Consider additional indexes based on actual query patterns
- Review index usage with `pg_stat_user_indexes`

---

## Data Examples

### p_notifications Example (SYSTEM)

```sql
INSERT INTO p_notifications VALUES (
    'uuid-1',
    'SYSTEM',
    NULL,
    NULL,
    NULL,
    'U12345678',
    '홍길동',
    '신규 주문이 접수되었습니다. 출발 마감시간: 14:30',
    'ORDER_NOTIFICATION',
    'order-uuid-123',
    '2025-11-04 10:00:00',
    'SENT',
    NULL,
    '2025-11-04 10:00:00',
    'SYSTEM',
    NULL, NULL, NULL, NULL
);
```

### p_notifications Example (USER)

```sql
INSERT INTO p_notifications VALUES (
    'uuid-2',
    'USER',
    'manager01',
    'U87654321',
    '김매니저',
    'U12345678',
    '홍길동',
    '오늘 배송 건 확인 부탁드립니다.',
    'MANUAL',
    NULL,
    '2025-11-04 11:00:00',
    'SENT',
    NULL,
    '2025-11-04 11:00:00',
    'manager01',
    NULL, NULL, NULL, NULL
);
```

### p_external_api_logs Example (Gemini)

```sql
INSERT INTO p_external_api_logs VALUES (
    'log-uuid-1',
    'GEMINI',
    'POST /v1/chat/completions',
    '{"model":"gemini-2.5-flash","messages":[...]}'::jsonb,
    '{"choices":[{"message":{"content":"14:30"}}]}'::jsonb,
    200,
    true,
    NULL,
    NULL,
    1250,
    0.0015,
    '2025-11-04 10:00:00',
    'uuid-1'
);
```

### p_external_api_logs Example (Slack)

```sql
INSERT INTO p_external_api_logs VALUES (
    'log-uuid-2',
    'SLACK',
    'POST /api/chat.postMessage',
    '{"channel":"U12345678","text":"..."}'::jsonb,
    '{"ok":true,"ts":"1699084800.123456"}'::jsonb,
    200,
    true,
    NULL,
    NULL,
    450,
    NULL,
    '2025-11-04 10:00:01',
    'uuid-1'
);
```

---

## References

- **[최종] 테이블 명세서**: `docs/scrum/[최종] 테이블 명세서.md`
- **ERD Diagram**: (To be added)
- **PostgreSQL 17 Documentation**: https://www.postgresql.org/docs/17/
