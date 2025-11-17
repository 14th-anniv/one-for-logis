# [최종] 테이블 명세서

# 주문

## 1. 주문 테이블 (p_order)

| 필드 이름 | 데이터 타입 | 설명 |
| --- | --- | --- |
| id | UUID | 주문 ID, Primary Key |
| order_no | VARCHAR(30) | 주문번호, Unique |
| user_id | BIGINT | 주문 생성 사용자 ID (p_users 참조) |
| supplier_company_id | UUID | 공급업체 ID |
| receiver_company_id | UUID | 수령업체 ID |
| status | VARCHAR(20) | 주문 상태 
  - 주문접수 : PENDING 
  - 결제완료 : PAID 
  - 출고준비 : PACKING 
  - 배송중 : SHIPPED
  - 배송완료 : DELIVERED
  - 주문취소 : CANCELED |
| delivery_id | UUID | 배송 ID, NULL 가능  - |
| items_count | INTEGER | 주문 상품 개수 |
| total_amount | NUMERIC(18,2) | 주문 총금액 |
| request_note | TEXT | 요청사항, NULL 가능 |
| created_at | TIMESTAMP | 레코드 생성 시간 |
| created_by | VARCHAR(100) | 레코드 생성자 (username) |
| updated_at | TIMESTAMP | 레코드 수정 시간 |
| updated_by | VARCHAR(100) | 레코드 수정자 (username) |
| deleted_at | TIMESTAMP | 레코드 삭제 시간 (논리삭제) |
| deleted_by | VARCHAR(100) | 레코드 삭제자 (username) |

## 2. 주문 항목 테이블 (p_order_item)

| 필드 이름 | 데이터 타입 | 설명 |
| --- | --- | --- |
| id | UUID | 주문 항목 ID, Primary Key |
| order_id | UUID | 주문 ID (p_order 참조, FK) |
| product_id | UUID | 상품 ID (상품 서비스 참조) |
| product_name | VARCHAR(255) | 상품 이름(스냅샷) |
| unit_price | NUMERIC(18,2) | 단가 |
| quantity | INTEGER | 수량 |
| line_total | NUMERIC(18,2) | 항목 합계(단가×수량) |
| created_at | TIMESTAMP | 레코드 생성 시간 |
| created_by | VARCHAR(100) | 레코드 생성자 (username) |
| updated_at | TIMESTAMP | 레코드 수정 시간 |
| updated_by | VARCHAR(100) | 레코드 수정자 (username) |
| deleted_at | TIMESTAMP | 레코드 삭제 시간 (논리삭제) |
| deleted_by | VARCHAR(100) | 레코드 삭제자 (username) |

## 3. 주문 상태 이력 테이블 (p_order_status_history)

| 필드 이름 | 데이터 타입 | 설명 |
| --- | --- | --- |
| id | UUID | 상태 이력 ID, Primary Key |
| order_id | UUID | 주문 ID (p_order 참조, FK) |
| from_status | VARCHAR(20) | 변경 전 상태 |
| to_status | VARCHAR(20) | 변경 후 상태 |
| reason | TEXT | 상태 변경 사유, NULL 가능 |
| created_at | TIMESTAMP | 레코드 생성 시간 |
| created_by | VARCHAR(100) | 레코드 생성자 (username) |
| updated_at | TIMESTAMP | 레코드 수정 시간 |
| updated_by | VARCHAR(100) | 레코드 수정자 (username) |
| deleted_at | TIMESTAMP | 레코드 삭제 시간 (논리삭제) |
| deleted_by | VARCHAR(100) | 레코드 삭제자 (username) |

# 업체

## company-service (company_db 스키마)

### 업체 테이블 (`p_companies`)

공급업체 및 수령업체 정보를 관리합니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `id` | `UUID` | PRIMARY KEY | 업체 고유 ID |
| `name` | `VARCHAR(255)` | NOT NULL | 업체명 |
| `type` | `ENUM` | NOT NULL | 업체 타입
(생산`SUPPLIER`, 
수령 `RECEIVER`) |
| `hub_id` | `UUID` | NOT NULL | 소속 허브 ID |
| `address` | `VARCHAR(255)` | NOT NULL | 업체 주소 |

**비즈니스 규칙**:

- **업체 소속**
    - **모든 업체는 특정 허브에 소속되어 있습니다.**
    - 상품 관리 허브 ID를 확인하여 존재하는지 확인합니다.
- **업체 타입** :
    - 업체는 **생산업체**와 **수령업체**로 구분됩니다.
- company_type: SUPPLIER (생산업체) 또는 RECEIVER (수령업체)
- hub_id는 hub-service API 호출로 검증

|  | 생성 | 수정 | 삭제 | 조회 및 검색 |
| --- | --- | --- | --- | --- |
| `마스터 관리자` | O | O | O | O |
| `허브 관리자`  | O (담당 허브) | O (담당 허브) | O (담당 허브) | O |
| `배송 담당자` | X | X | X | O |
| `업체 담당자` | X | O (본인 업체) | X | O |

---

# 상품

<aside>
😇

상품은 **담당 허브가** 관리합니다

</aside>

## product-service (product_db 스키마)

### 상품 테이블 (`p_products`)

업체의 상품 정보를 관리합니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `id` | `UUID` | PRIMARY KEY | 상품 고유 ID |
| `name` | `VARCHAR(255)` | NOT NULL | 상품명 |
| `quantity` | `Interger` | NOT NULL (디폴트 값 고민) | 재고 수량 |
| `price` | `DECIMAL` | NOT NULL | 단가 |
| `hub_id` | `UUID` | NOT NULL | 상품 관리 허브 ID |
| `company_id` | `UUID` | NOT NULL | 소속 업체 ID |
| + Audit 필드 |  |  |  |

**비즈니스 규칙**:

- **모든 상품은 특정 업체와 허브에 소속**
    - 상품 업체가 존재하는지 확인합니다.
    - 상품 관리 허브 ID를 확인하여 존재하는지 확인합니다.
- company_id는 company-service API로 검증
- hub_id는 hub-service API로 검증
- 주문 생성 시 quantity 감소, 주문 취소 시 quantity 증가
- quantity < 0 불가

|  | 생성 | 수정 | 삭제 | 조회 및 검색 |
| --- | --- | --- | --- | --- |
| `마스터 관리자` | O | O | O | O |
| `허브 관리자` | O (담당 허브) | O (담당 허브) | O (담당 허브) | O (담당 허브) |
| `배송 담당자` | X | X | X | O |
| `업체 담당자` | O (본인 업체) | O (본인 업체) | X | O |

# 회원

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `username` | `VARCHAR(100)` | PRIMARY KEY | 사용자 ID (4-10자, 소문자+숫자) |
| `password` | `VARCHAR(255)` | NOT NULL | 암호화된 비밀번호 (BCrypt) |
| `name` | `VARCHAR(100)` | NOT NULL | 사용자 실명 |
| `email` | `VARCHAR(255)` | UNIQUE | 사용자 이메일 (선택) |
| `slack_id` | `VARCHAR(100)` | NOT NULL | Slack ID (알림용) |
| `role` | `VARCHAR(50)` | NOT NULL | 사용자 역할 (ENUM: MASTER, HUB_MANAGER, DELIVERY_MANAGER, COMPANY_MANAGER) |
| `status` | `VARCHAR(50)` | NOT NULL, DEFAULT 'PENDING' | 승인 상태 (ENUM: PENDING, APPROVED, REJECTED) |
| `hub_id` | `UUID` | NULLABLE | 소속 허브 ID (HUB_MANAGER, DELIVERY_MANAGER의 경우) |
| `company_id` | `UUID` | NULLABLE | 소속 업체 ID (COMPANY_MANAGER, DELIVERY_MANAGER의 경우) |
| `is_public` | `BOOLEAN` | DEFAULT TRUE | 사용자 정보 공개 여부 |
| + Audit 필드 |  |  |  |

# 허브

## **1. 허브 테이블 (p_hub)**

| **필드 이름** | **데이터 타입** | **설명** |
| --- | --- | --- |
| **id** | UUID | **허브 ID, Primary Key** 17개 센터가 초기 데이터로 고정됨 다른 도메인에서 허브를 참조할 때 사용하는 기준 키 |
| **name** | VARCHAR(255) | **허브명** 예: 서울허브, 대전허브 UI 표기 및 검색용 중복 방지를 위해 **UNIQUE** |
| **address** | VARCHAR(255) | **허브 주소(도로명)** 지도/배송 라벨 표기용 |
| **lat** | DECIMAL(10,6) | **위도** 예: 37.566500 지도 매핑, 거리 계산의 기준 값 |
| **lon** | DECIMAL(10,6) | **경도** 예: 126.978000 |
| **created_at** | TIMESTAMP | **레코드 생성 시각** |
| **created_by** | VARCHAR(100) | **레코드 생성자(username)** 예: system/admin |
| **updated_at** | TIMESTAMP | **레코드 수정 시각** |
| **updated_by** | VARCHAR(100) | **레코드 수정자(username)** |
| **deleted_at** | TIMESTAMP | **논리삭제 시각** 기본은 NULL(활성) 삭제 시 연관 경로 비활성화 |
| **deleted_by** | VARCHAR(100) | **논리삭제 수행자(username)** |

## **2. 허브 간 경로 테이블 (p_hub_route)**

| **필드 이름** | **데이터 타입** | **설명** |
| --- | --- | --- |
| **id** | BIGINT | **허브 경로 ID, Primary Key** 내부 식별자(자동 증가). |
| **from_hub_id** | UUID | **출발 허브 ID**. p_hub.id FK 방향성이 있으므로 서울→대전과 대전→서울은 별도 레코드 |
| **to_hub_id** | UUID | **도착 허브 ID**. p_hub.id FK |
| **route_time** | INTEGER | **예상 소요 시간(분)** 예: 120 → 2시간 실시간 최단경로 계산의 “가중치”로 사용됨 |
| **route_distance** | DECIMAL(8,2) | **허브 간 거리(km)**. 예: 150.50. 지도/운임 계산, SLA 지표에 활용 |
| **route_type** | ENUM('DIRECT','RELAY') | **경로 유형**. DIRECT=직통 연결, RELAY=중계 허브를 거치는 유형(운영 정책 표기용)
최단경로 탐색 자체는 **그래프 계산**으로 처리 |
| **created_at** | TIMESTAMP | **레코드 생성 시각** |
| **created_by** | VARCHAR(100) | **레코드 생성자(username)** |
| **updated_at** | TIMESTAMP | **레코드 수정 시각**. 거리/시간 보정 시 갱신 |
| **updated_by** | VARCHAR(100) | **레코드 수정자(username)** |
| **deleted_at** | TIMESTAMP | **논리삭제 시각** 경로 비활성화(운영 중 일시 차단 등) 조회 시 IS NULL 필터 |
| **deleted_by** | VARCHAR(100) | **논리삭제 수행자(username)** |

# 알림

## 1. p_notifications (알림 메시지 이력)

### 테이블 개요

- **목적**: 모든 알림 메시지 이력 저장 (Slack 발송 내역)
- **특징**: 발신자 정보 스냅샷 저장 (Snapshot Pattern)
- **Soft Delete**: 지원

### 컬럼 정의

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| **message_id** | UUID | NOT NULL | gen_random_uuid() | PK, 메시지 고유 ID |
| **sender_type** | VARCHAR(20) | NOT NULL | - | 발신자 유형 (USER/SYSTEM) |
| **sender_username** | VARCHAR(50) | NULL | - | 발신자 username (USER일 때만) |
| **sender_slack_id** | VARCHAR(50) | NULL | - | 발신자 Slack ID (USER일 때만) |
| **sender_name** | VARCHAR(100) | NULL | - | 발신자 이름 스냅샷 (USER일 때만) |
| **recipient_slack_id** | VARCHAR(50) | NOT NULL | - | 수신자 Slack ID |
| **recipient_name** | VARCHAR(100) | NOT NULL | - | 수신자 이름 스냅샷 |
| **message_content** | TEXT | NOT NULL | - | 메시지 내용 (Slack 발송 텍스트) |
| **message_type** | VARCHAR(30) | NOT NULL | - | 메시지 유형 ENUM |
| **reference_id** | UUID | NULL | - | 연관 엔티티 ID (주문, 배송 등) |
| **sent_at** | TIMESTAMP | NULL | - | 실제 발송 완료 시각 |
| **status** | VARCHAR(20) | NOT NULL | 'PENDING' | 발송 상태 ENUM |
| **error_message** | TEXT | NULL | - | 발송 실패 시 에러 메시지 |
| **created_at** | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP | 생성 일시 |
| **created_by** | VARCHAR(100) | NULL | - | 생성자 |
| **updated_at** | TIMESTAMP | NULL | - | 수정 일시 |
| **updated_by** | VARCHAR(100) | NULL | - | 수정자 |
| **deleted_at** | TIMESTAMP | NULL | - | 삭제 일시 (Soft Delete) |
| **deleted_by** | VARCHAR(100) | NULL | - | 삭제자 |

### ENUM 정의

**sender_type**:

- `USER`: 사용자가 발송한 메시지
- `SYSTEM`: 시스템이 자동 발송한 메시지

**message_type**:

- `ORDER_NOTIFICATION`: 주문 생성 시 출발 마감시간 알림
- `DAILY_ROUTE`: 일일 경로 최적화 알림 (Challenge 기능)
- `MANUAL`: 사용자가 직접 작성한 수동 메시지

**status**:

- `PENDING`: 발송 대기중
- `SENT`: 발송 완료
- `FAILED`: 발송 실패

### 인덱스

```sql
CREATE INDEX idx_notifications_recipient ON p_notifications(recipient_slack_id, deleted_at);
CREATE INDEX idx_notifications_type ON p_notifications(message_type, deleted_at);
CREATE INDEX idx_notifications_status ON p_notifications(status, deleted_at);
CREATE INDEX idx_notifications_created_at ON p_notifications(created_at DESC);
CREATE INDEX idx_notifications_reference ON p_notifications(reference_id, deleted_at);

```

### 제약조건

```sql
-- sender_type이 SYSTEM이면 sender 필드들은 NULL, USER이면 NOT NULL
ALTER TABLE p_notifications ADD CONSTRAINT chk_sender_info
CHECK (
  (sender_type = 'SYSTEM' AND sender_username IS NULL) OR
  (sender_type = 'USER' AND sender_username IS NOT NULL)
);

-- status가 SENT이면 sent_at은 NOT NULL
ALTER TABLE p_notifications ADD CONSTRAINT chk_sent_at
CHECK (
  (status = 'SENT' AND sent_at IS NOT NULL) OR
  (status != 'SENT')
);

```

### DDL

```sql
CREATE TABLE p_notifications (
    message_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_type VARCHAR(20) NOT NULL CHECK (sender_type IN ('USER', 'SYSTEM')),
    sender_username VARCHAR(50),
    sender_slack_id VARCHAR(50),
    sender_name VARCHAR(100),
    recipient_slack_id VARCHAR(50) NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    message_content TEXT NOT NULL,
    message_type VARCHAR(30) NOT NULL CHECK (message_type IN ('ORDER_NOTIFICATION', 'DAILY_ROUTE', 'MANUAL')),
    reference_id UUID,
    sent_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),

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

---

## 2. p_external_api_logs (외부 API 호출 로그)

### 테이블 개요

- **목적**: 모든 외부 API 호출 이력 추적 (Slack, Gemini, Naver Maps)
- **특징**: 비용, 성능, 에러 모니터링
- **Soft Delete**: 미지원 (로그성 데이터)

### 컬럼 정의

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| **log_id** | UUID | NOT NULL | gen_random_uuid() | PK, 로그 고유 ID |
| **api_provider** | VARCHAR(30) | NOT NULL | - | API 제공자 ENUM |
| **api_method** | VARCHAR(100) | NOT NULL | - | API 메서드/엔드포인트 |
| **request_data** | JSONB | NULL | - | 요청 데이터 (JSON) |
| **response_data** | JSONB | NULL | - | 응답 데이터 (JSON) |
| **http_status** | INTEGER | NULL | - | HTTP 상태 코드 |
| **is_success** | BOOLEAN | NOT NULL | false | 성공 여부 |
| **error_code** | VARCHAR(50) | NULL | - | 에러 코드 (실패 시) |
| **error_message** | TEXT | NULL | - | 에러 메시지 (실패 시) |
| **duration_ms** | INTEGER | NULL | - | 응답 시간 (밀리초) |
| **cost** | DECIMAL(10,4) | NULL | - | API 호출 비용 (USD) |
| **called_at** | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP | 호출 일시 |
| **message_id** | UUID | NULL | - | 연관된 알림 메시지 ID (FK, 논리적) |

### ENUM 정의

**api_provider**:

- `SLACK`: Slack API (chat.postMessage)
- `GEMINI`: Google Gemini API (chat/completions)
- `NAVER_MAPS`: Naver Maps Directions 5 API

### 인덱스

```sql
CREATE INDEX idx_api_logs_provider ON p_external_api_logs(api_provider, called_at DESC);
CREATE INDEX idx_api_logs_success ON p_external_api_logs(is_success, called_at DESC);
CREATE INDEX idx_api_logs_called_at ON p_external_api_logs(called_at DESC);
CREATE INDEX idx_api_logs_message_id ON p_external_api_logs(message_id);
CREATE INDEX idx_api_logs_duration ON p_external_api_logs(duration_ms);

```

### DDL

```sql
CREATE TABLE p_external_api_logs (
    log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    api_provider VARCHAR(30) NOT NULL CHECK (api_provider IN ('SLACK', 'GEMINI', 'NAVER_MAPS')),
    api_method VARCHAR(100) NOT NULL,
    request_data JSONB,
    response_data JSONB,
    http_status INTEGER,
    is_success BOOLEAN NOT NULL DEFAULT false,
    error_code VARCHAR(50),
    error_message TEXT,
    duration_ms INTEGER,
    cost DECIMAL(10,4),
    called_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    message_id UUID
);

CREATE INDEX idx_api_logs_provider ON p_external_api_logs(api_provider, called_at DESC);
CREATE INDEX idx_api_logs_success ON p_external_api_logs(is_success, called_at DESC);
CREATE INDEX idx_api_logs_called_at ON p_external_api_logs(called_at DESC);
CREATE INDEX idx_api_logs_message_id ON p_external_api_logs(message_id);
CREATE INDEX idx_api_logs_duration ON p_external_api_logs(duration_ms);

```

---

- challenge용
    
    ## 3. p_company_delivery_routes (업체 배송 경로, Challenge용)
    
    ### 테이블 개요
    
    - **목적**: AI가 계산한 일일 최적 배송 경로 저장
    - **특징**: ChatGPT TSP 결과 + Naver Maps 경로 정보
    - **Soft Delete**: 지원
    
    ### 컬럼 정의
    
    | 컬럼명 | 타입 | NULL | 기본값 | 설명 |
    | --- | --- | --- | --- | --- |
    | **route_id** | UUID | NOT NULL | gen_random_uuid() | PK, 경로 고유 ID |
    | **delivery_id** | UUID | NOT NULL | - | 배송 ID (FK, 논리적) |
    | **departure_hub_id** | UUID | NOT NULL | - | 출발 허브 ID (FK, 논리적) |
    | **receiver_company_id** | UUID | NOT NULL | - | 수취 업체 ID (FK, 논리적) |
    | **estimated_distance_km** | DECIMAL(10,2) | NULL | - | 예상 거리 (km) |
    | **estimated_duration_min** | INTEGER | NULL | - | 예상 소요 시간 (분) |
    | **actual_distance_km** | DECIMAL(10,2) | NULL | - | 실제 거리 (km) |
    | **actual_duration_min** | INTEGER | NULL | - | 실제 소요 시간 (분) |
    | **delivery_sequence** | INTEGER | NOT NULL | - | AI가 계산한 최적 방문 순서 (1, 2, 3...) |
    | **current_status** | VARCHAR(50) | NOT NULL | 'PENDING' | 경로 상태 ENUM |
    | **delivery_staff_id** | BIGINT | NULL | - | 배송 담당자 ID (FK, 논리적) |
    | **created_at** | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP | 생성 일시 |
    | **created_by** | VARCHAR(100) | NULL | - | 생성자 |
    | **updated_at** | TIMESTAMP | NULL | - | 수정 일시 |
    | **updated_by** | VARCHAR(100) | NULL | - | 수정자 |
    | **deleted_at** | TIMESTAMP | NULL | - | 삭제 일시 (Soft Delete) |
    | **deleted_by** | VARCHAR(100) | NULL | - | 삭제자 |
    
    ### ENUM 정의
    
    **current_status**:
    
    - `PENDING`: 배송 대기
    - `IN_TRANSIT`: 배송 중
    - `COMPLETED`: 배송 완료
    - `FAILED`: 배송 실패
    
    ### 인덱스
    
    ```sql
    CREATE INDEX idx_company_routes_delivery ON p_company_delivery_routes(delivery_id, deleted_at);
    CREATE INDEX idx_company_routes_hub ON p_company_delivery_routes(departure_hub_id, deleted_at);
    CREATE INDEX idx_company_routes_staff ON p_company_delivery_routes(delivery_staff_id, deleted_at);
    CREATE INDEX idx_company_routes_sequence ON p_company_delivery_routes(delivery_staff_id, delivery_sequence, deleted_at);
    CREATE INDEX idx_company_routes_status ON p_company_delivery_routes(current_status, deleted_at);
    
    ```
    
    ### DDL
    
    ```sql
    CREATE TABLE p_company_delivery_routes (
        route_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        delivery_id UUID NOT NULL,
        departure_hub_id UUID NOT NULL,
        receiver_company_id UUID NOT NULL,
        estimated_distance_km DECIMAL(10,2),
        estimated_duration_min INTEGER,
        actual_distance_km DECIMAL(10,2),
        actual_duration_min INTEGER,
        delivery_sequence INTEGER NOT NULL,
        current_status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (current_status IN ('PENDING', 'IN_TRANSIT', 'COMPLETED', 'FAILED')),
        delivery_staff_id BIGINT,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        created_by VARCHAR(100),
        updated_at TIMESTAMP,
        updated_by VARCHAR(100),
        deleted_at TIMESTAMP,
        deleted_by VARCHAR(100)
    );
    
    CREATE INDEX idx_company_routes_delivery ON p_company_delivery_routes(delivery_id, deleted_at);
    CREATE INDEX idx_company_routes_hub ON p_company_delivery_routes(departure_hub_id, deleted_at);
    CREATE INDEX idx_company_routes_staff ON p_company_delivery_routes(delivery_staff_id, deleted_at);
    CREATE INDEX idx_company_routes_sequence ON p_company_delivery_routes(delivery_staff_id, delivery_sequence, deleted_at);
    CREATE INDEX idx_company_routes_status ON p_company_delivery_routes(current_status, deleted_at);
    
    ```
    
    ---
    

### 테이블 관계도

```
p_notifications (1) ─────< (0..N) p_external_api_logs
    │                              [message_id]
    │
    └── reference_id (논리적 FK)
        ├─> p_order.id
        ├─> p_delivery.id
        └─> p_company_delivery_routes.route_id

p_company_delivery_routes(challenge용)
    ├── delivery_id ─────> p_delivery.id (논리적 FK)
    ├── departure_hub_id ─> p_hub.id (논리적 FK)
    ├── receiver_company_id ─> p_company.id (논리적 FK)
    └── delivery_staff_id ─> p_delivery_staff.staff_id (논리적 FK)

```

---

### 데이터 예시

### p_notifications 예시

```sql
-- SYSTEM 메시지 (주문 알림)
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

-- USER 메시지 (수동 발송)
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

### p_external_api_logs 예시

```sql
-- ChatGPT API 호출
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

-- Slack API 호출
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