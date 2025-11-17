# 테이블 명세서 (Table Specifications)

## 개요

본 문서는 14logis 프로젝트의 모든 데이터베이스 테이블 명세를 정의합니다.

### 공통 규칙

**테이블 명명 규칙**:
- 모든 테이블은 `p_` 접두사 사용 (예: `p_users`, `p_hubs`)

**공통 Audit 필드** (모든 테이블 포함):
| 필드명 | 데이터 타입 | 설명 |
|--------|------------|------|
| `created_at` | `TIMESTAMP` | 레코드 생성 시간 |
| `created_by` | `VARCHAR(100)` | 레코드 생성자 (username) |
| `updated_at` | `TIMESTAMP` | 레코드 수정 시간 |
| `updated_by` | `VARCHAR(100)` | 레코드 수정자 (username) |
| `deleted_at` | `TIMESTAMP` | 레코드 삭제 시간 (Soft Delete) |
| `deleted_by` | `VARCHAR(100)` | 레코드 삭제자 (username) |

**ID 타입**:
- 기본: `UUID` (PostgreSQL의 경우)
- 예외: 사용자 테이블은 `VARCHAR` username 사용

---

## 1. auth-service (auth_db 스키마)

### 1.1 사용자 테이블 (`p_users`)

사용자 정보 및 인증을 관리합니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|------------|---------|------|
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
| + Audit 필드 | | | |

**인덱스**:
- `idx_users_role` ON (`role`)
- `idx_users_status` ON (`status`)
- `idx_users_hub_id` ON (`hub_id`)
- `idx_users_company_id` ON (`company_id`)

**비즈니스 규칙**:
- username: 4-10자, 소문자(a-z) + 숫자(0-9)
- password: 8-15자, 대소문자 + 숫자 + 특수문자
- 회원가입 시 status는 PENDING
- MASTER 또는 HUB_MANAGER가 APPROVED로 변경 시 로그인 가능
- deleted_at이 NULL인 레코드만 활성 사용자

---

## 2. hub-service (hub_db 스키마)

### 2.1 허브 테이블 (`p_hubs`)

17개 지역 허브센터 정보를 관리합니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|------------|---------|------|
| `hub_id` | `UUID` | PRIMARY KEY | 허브 고유 ID |
| `hub_name` | `VARCHAR(100)` | NOT NULL, UNIQUE | 허브 이름 (예: "서울특별시 센터") |
| `address` | `VARCHAR(500)` | NOT NULL | 허브 주소 |
| `latitude` | `DECIMAL(10, 8)` | NOT NULL | 위도 |
| `longitude` | `DECIMAL(11, 8)` | NOT NULL | 경도 |
| `hub_type` | `VARCHAR(50)` | DEFAULT 'REGIONAL' | 허브 타입 (ENUM: REGIONAL, CENTRAL) - Hub-and-Spoke 모델용 |
| + Audit 필드 | | | |

**인덱스**:
- `idx_hubs_name` ON (`hub_name`)
- `idx_hubs_type` ON (`hub_type`)
- `idx_hubs_location` ON (`latitude`, `longitude`)

**초기 데이터** (17개 센터):
1. 서울특별시 센터: 서울특별시 송파구 송파대로 55
2. 경기 북부 센터: 경기도 고양시 덕양구 권율대로 570
3. 경기 남부 센터: 경기도 이천시 덕평로 257-21 (CENTRAL)
4. 부산광역시 센터: 부산 동구 중앙대로 206
5. 대구광역시 센터: 대구 북구 태평로 161 (CENTRAL)
6. 인천광역시 센터: 인천 남동구 정각로 29
7. 광주광역시 센터: 광주 서구 내방로 111
8. 대전광역시 센터: 대전 서구 둔산로 100 (CENTRAL)
9. 울산광역시 센터: 울산 남구 중앙로 201
10. 세종특별자치시 센터: 세종특별자치시 한누리대로 2130
11. 강원특별자치도 센터: 강원특별자치도 춘천시 중앙로 1
12. 충청북도 센터: 충북 청주시 상당구 상당로 82
13. 충청남도 센터: 충남 홍성군 홍북읍 충남대로 21
14. 전북특별자치도 센터: 전북특별자치도 전주시 완산구 효자로 225
15. 전라남도 센터: 전남 무안군 삼향읍 오룡길 1
16. 경상북도 센터: 경북 안동시 풍천면 도청대로 455
17. 경상남도 센터: 경남 창원시 의창구 중앙대로 300

**비즈니스 규칙**:
- 허브 정보는 캐싱 대상 (TTL: 24시간)
- hub_type: Hub-and-Spoke 모델에서 경기남부, 대전, 대구는 CENTRAL

### 2.2 허브 간 이동 정보 테이블 (`p_hub_routes`)

허브 간 이동 경로 및 소요 시간/거리를 관리합니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|------------|---------|------|
| `route_id` | `UUID` | PRIMARY KEY | 경로 고유 ID |
| `origin_hub_id` | `UUID` | NOT NULL, FK → p_hubs | 출발 허브 ID |
| `destination_hub_id` | `UUID` | NOT NULL, FK → p_hubs | 도착 허브 ID |
| `distance_km` | `DECIMAL(10, 2)` | NOT NULL | 이동 거리 (km) |
| `duration_minutes` | `INTEGER` | NOT NULL | 예상 소요 시간 (분) |
| `route_order` | `INTEGER` | DEFAULT 1 | 경로 순서 (Hub-to-Hub Relay 모델용) |
| + Audit 필드 | | | |

**인덱스**:
- `idx_hub_routes_origin` ON (`origin_hub_id`)
- `idx_hub_routes_destination` ON (`destination_hub_id`)
- `idx_hub_routes_pair` ON (`origin_hub_id`, `destination_hub_id`) UNIQUE

**비즈니스 규칙**:
- 허브 간 이동 정보는 캐싱 대상 (TTL: 24시간)
- Hub Route Model에 따라 데이터 구성 달라짐:
  - **P2P**: 모든 허브 간 직접 연결 (17×16 = 272개 경로)
  - **Hub-and-Spoke**: 중앙허브(경기남부, 대전, 대구)를 통한 경로
  - **P2P + Relay**: 인접 허브 직접 연결 + 200km 이상은 중간 경유지
  - **Hub-to-Hub Relay**: 연결된 허브만 이동 가능 (다익스트라 알고리즘)

---

## 3. company-service (company_db 스키마)

### 3.1 업체 테이블 (`p_companies`)

공급업체 및 수령업체 정보를 관리합니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|------------|---------|------|
| `company_id` | `UUID` | PRIMARY KEY | 업체 고유 ID |
| `company_name` | `VARCHAR(200)` | NOT NULL | 업체명 |
| `company_type` | `VARCHAR(50)` | NOT NULL | 업체 타입 (ENUM: SUPPLIER, RECEIVER) |
| `hub_id` | `UUID` | NOT NULL | 소속 허브 ID (FK → hub-service) |
| `address` | `VARCHAR(500)` | NOT NULL | 업체 주소 |
| `latitude` | `DECIMAL(10, 8)` | NULLABLE | 위도 (도전 과제용) |
| `longitude` | `DECIMAL(11, 8)` | NULLABLE | 경도 (도전 과제용) |
| `contact_name` | `VARCHAR(100)` | NULLABLE | 담당자명 |
| `contact_phone` | `VARCHAR(50)` | NULLABLE | 연락처 |
| + Audit 필드 | | | |

**인덱스**:
- `idx_companies_hub_id` ON (`hub_id`)
- `idx_companies_type` ON (`company_type`)
- `idx_companies_name` ON (`company_name`)

**비즈니스 규칙**:
- company_type: SUPPLIER (공급업체) 또는 RECEIVER (수령업체)
- 모든 업체는 특정 허브에 소속
- hub_id는 hub-service API 호출로 검증

---

## 4. product-service (product_db 스키마)

### 4.1 상품 테이블 (`p_products`)

업체의 상품 정보를 관리합니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|------------|---------|------|
| `product_id` | `UUID` | PRIMARY KEY | 상품 고유 ID |
| `product_name` | `VARCHAR(200)` | NOT NULL | 상품명 |
| `company_id` | `UUID` | NOT NULL | 소속 업체 ID (FK → company-service) |
| `hub_id` | `UUID` | NOT NULL | 상품 관리 허브 ID (FK → hub-service) |
| `description` | `TEXT` | NULLABLE | 상품 설명 |
| `unit` | `VARCHAR(50)` | DEFAULT '개' | 단위 (예: 개, 박스, kg) |
| `quantity` | `INTEGER` | DEFAULT 0 | 재고 수량 |
| `price` | `DECIMAL(15, 2)` | NULLABLE | 단가 (선택) |
| + Audit 필드 | | | |

**인덱스**:
- `idx_products_company_id` ON (`company_id`)
- `idx_products_hub_id` ON (`hub_id`)
- `idx_products_name` ON (`product_name`)

**비즈니스 규칙**:
- 모든 상품은 특정 업체와 허브에 소속
- company_id는 company-service API로 검증
- hub_id는 hub-service API로 검증
- 주문 생성 시 quantity 감소, 주문 취소 시 quantity 증가
- quantity < 0 불가

### 4.2 재고 내역 테이블 (`p_inventory_histories`) - 선택 사항

재고 변동 내역을 추적합니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|------------|---------|------|
| `history_id` | `UUID` | PRIMARY KEY | 내역 고유 ID |
| `product_id` | `UUID` | NOT NULL, FK → p_products | 상품 ID |
| `quantity_change` | `INTEGER` | NOT NULL | 수량 변동 (+증가, -감소) |
| `quantity_after` | `INTEGER` | NOT NULL | 변동 후 수량 |
| `reason` | `VARCHAR(100)` | NOT NULL | 사유 (ENUM: ORDER_CREATE, ORDER_CANCEL, MANUAL_ADJUST) |
| `reference_id` | `UUID` | NULLABLE | 참조 ID (주문 ID 등) |
| + Audit 필드 | | | |

**인덱스**:
- `idx_inventory_histories_product` ON (`product_id`)
- `idx_inventory_histories_date` ON (`created_at`)

---

## 5. order-service (order_db 스키마)

### 5.1 주문 테이블 (`p_orders`)

주문 정보를 관리합니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|------------|---------|------|
| `order_id` | `UUID` | PRIMARY KEY | 주문 고유 ID |
| `supplier_company_id` | `UUID` | NOT NULL | 공급 업체 ID (FK → company-service) |
| `receiver_company_id` | `UUID` | NOT NULL | 수령 업체 ID (FK → company-service) |
| `product_id` | `UUID` | NOT NULL | 상품 ID (FK → product-service) |
| `quantity` | `INTEGER` | NOT NULL | 주문 수량 |
| `delivery_id` | `UUID` | NULLABLE | 배송 ID (FK → delivery-service) |
| `request_notes` | `TEXT` | NULLABLE | 요청 사항 (납품 기한, 특이사항 등) |
| `delivery_deadline` | `TIMESTAMP` | NULLABLE | 납품 기한 일시 |
| `order_status` | `VARCHAR(50)` | DEFAULT 'PENDING' | 주문 상태 (ENUM: PENDING, CONFIRMED, IN_DELIVERY, COMPLETED, CANCELLED) |
| + Audit 필드 | | | |

**인덱스**:
- `idx_orders_supplier` ON (`supplier_company_id`)
- `idx_orders_receiver` ON (`receiver_company_id`)
- `idx_orders_product` ON (`product_id`)
- `idx_orders_delivery` ON (`delivery_id`)
- `idx_orders_status` ON (`order_status`)
- `idx_orders_deadline` ON (`delivery_deadline`)

**비즈니스 규칙**:
- 주문 생성 시 product-service에서 재고 확인 및 차감
- 주문 생성 시 delivery-service 호출하여 배송 생성
- 주문 취소 시 재고 복원 및 배송 취소 처리
- 주문 삭제는 soft delete (deleted_at 설정)

---

## 6. delivery-service (delivery_db 스키마)

### 6.1 배송 담당자 테이블 (`p_delivery_personnel`)

배송 담당자 정보를 관리합니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|------------|---------|------|
| `personnel_id` | `VARCHAR(100)` | PRIMARY KEY | 배송 담당자 ID (username과 동일) |
| `personnel_type` | `VARCHAR(50)` | NOT NULL | 담당자 타입 (ENUM: HUB_DELIVERY, COMPANY_DELIVERY) |
| `hub_id` | `UUID` | NULLABLE | 소속 허브 ID (COMPANY_DELIVERY의 경우 필수) |
| `delivery_sequence` | `INTEGER` | NOT NULL | 배송 순번 (0-9, round-robin 할당용) |
| `slack_id` | `VARCHAR(100)` | NOT NULL | Slack ID (알림용) |
| `name` | `VARCHAR(100)` | NOT NULL | 담당자명 |
| + Audit 필드 | | | |

**인덱스**:
- `idx_delivery_personnel_type` ON (`personnel_type`)
- `idx_delivery_personnel_hub` ON (`hub_id`)
- `idx_delivery_personnel_sequence` ON (`hub_id`, `delivery_sequence`)

**비즈니스 규칙**:
- personnel_type:
  - `HUB_DELIVERY`: 허브 간 배송 담당 (전체 10명, hub_id는 NULL)
  - `COMPANY_DELIVERY`: 허브→업체 배송 담당 (각 허브당 10명)
- delivery_sequence: 0-9 순번으로 순차 배정 (round-robin)
- 새 담당자 추가 시 해당 그룹 내 max(delivery_sequence) + 1
- 삭제된 담당자 순번은 재배열하지 않음

### 6.2 배송 테이블 (`p_deliveries`)

주문에 대한 배송 전반의 상태를 관리합니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|------------|---------|------|
| `delivery_id` | `UUID` | PRIMARY KEY | 배송 고유 ID |
| `order_id` | `UUID` | NOT NULL | 주문 ID (FK → order-service) |
| `origin_hub_id` | `UUID` | NOT NULL | 출발 허브 ID (FK → hub-service) |
| `destination_hub_id` | `UUID` | NOT NULL | 목적지 허브 ID (FK → hub-service) |
| `delivery_address` | `VARCHAR(500)` | NOT NULL | 최종 배송지 주소 |
| `recipient_name` | `VARCHAR(100)` | NOT NULL | 수령인 이름 |
| `recipient_slack_id` | `VARCHAR(100)` | NOT NULL | 수령인 Slack ID |
| `company_personnel_id` | `VARCHAR(100)` | NULLABLE | 업체 배송 담당자 ID (FK → p_delivery_personnel) |
| `delivery_status` | `VARCHAR(50)` | DEFAULT 'HUB_WAITING' | 배송 상태 (ENUM: HUB_WAITING, HUB_MOVING, HUB_ARRIVED, COMPANY_MOVING, COMPLETED) |
| `started_at` | `TIMESTAMP` | NULLABLE | 배송 시작 시간 |
| `completed_at` | `TIMESTAMP` | NULLABLE | 배송 완료 시간 |
| + Audit 필드 | | | |

**인덱스**:
- `idx_deliveries_order` ON (`order_id`)
- `idx_deliveries_origin` ON (`origin_hub_id`)
- `idx_deliveries_destination` ON (`destination_hub_id`)
- `idx_deliveries_status` ON (`delivery_status`)
- `idx_deliveries_company_personnel` ON (`company_personnel_id`)

**배송 상태**:
- `HUB_WAITING`: 허브 대기 중
- `HUB_MOVING`: 허브 간 이동 중
- `HUB_ARRIVED`: 목적지 허브 도착
- `COMPANY_MOVING`: 업체 이동 중
- `COMPLETED`: 배송 완료

### 6.3 배송 경로 기록 테이블 (`p_delivery_routes`)

허브 간 배송 경로의 각 구간을 추적합니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|------------|---------|------|
| `route_record_id` | `UUID` | PRIMARY KEY | 경로 기록 고유 ID |
| `delivery_id` | `UUID` | NOT NULL, FK → p_deliveries | 배송 ID |
| `sequence` | `INTEGER` | NOT NULL | 경로 순번 (1부터 시작) |
| `origin_hub_id` | `UUID` | NOT NULL | 출발 허브 ID (FK → hub-service) |
| `destination_hub_id` | `UUID` | NOT NULL | 도착 허브 ID (FK → hub-service) |
| `hub_personnel_id` | `VARCHAR(100)` | NULLABLE | 허브 배송 담당자 ID (FK → p_delivery_personnel) |
| `expected_distance_km` | `DECIMAL(10, 2)` | NOT NULL | 예상 거리 (km) |
| `expected_duration_minutes` | `INTEGER` | NOT NULL | 예상 소요 시간 (분) |
| `actual_distance_km` | `DECIMAL(10, 2)` | NULLABLE | 실제 거리 (km) |
| `actual_duration_minutes` | `INTEGER` | NULLABLE | 실제 소요 시간 (분) |
| `route_status` | `VARCHAR(50)` | DEFAULT 'WAITING' | 구간 상태 (ENUM: WAITING, IN_PROGRESS, COMPLETED) |
| `started_at` | `TIMESTAMP` | NULLABLE | 구간 시작 시간 |
| `completed_at` | `TIMESTAMP` | NULLABLE | 구간 완료 시간 |
| + Audit 필드 | | | |

**인덱스**:
- `idx_delivery_routes_delivery` ON (`delivery_id`)
- `idx_delivery_routes_sequence` ON (`delivery_id`, `sequence`) UNIQUE
- `idx_delivery_routes_hub_personnel` ON (`hub_personnel_id`)
- `idx_delivery_routes_status` ON (`route_status`)

**비즈니스 규칙**:
- 주문 생성 시 전체 경로가 미리 생성됨
- sequence는 1부터 시작하여 순차 증가
- 각 구간마다 허브 배송 담당자가 순차 배정됨
- route_status: WAITING → IN_PROGRESS → COMPLETED

---

## 7. slack-service (slack_db 스키마)

### 7.1 Slack 메시지 테이블 (`p_slack_messages`)

발송된 Slack 메시지 내역을 저장합니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|------------|---------|------|
| `message_id` | `UUID` | PRIMARY KEY | 메시지 고유 ID |
| `recipient_slack_id` | `VARCHAR(100)` | NOT NULL | 수신자 Slack ID |
| `recipient_name` | `VARCHAR(100)` | NULLABLE | 수신자 이름 |
| `message_content` | `TEXT` | NOT NULL | 메시지 내용 |
| `message_type` | `VARCHAR(50)` | NOT NULL | 메시지 타입 (ENUM: ORDER_NOTIFICATION, DAILY_ROUTE, MANUAL) |
| `reference_id` | `UUID` | NULLABLE | 참조 ID (주문 ID, 배송 ID 등) |
| `sent_at` | `TIMESTAMP` | NOT NULL | 발송 시간 |
| `status` | `VARCHAR(50)` | DEFAULT 'PENDING' | 발송 상태 (ENUM: PENDING, SENT, FAILED) |
| `error_message` | `TEXT` | NULLABLE | 오류 메시지 (실패 시) |
| + Audit 필드 | | | |

**인덱스**:
- `idx_slack_messages_recipient` ON (`recipient_slack_id`)
- `idx_slack_messages_type` ON (`message_type`)
- `idx_slack_messages_reference` ON (`reference_id`)
- `idx_slack_messages_sent_at` ON (`sent_at`)
- `idx_slack_messages_status` ON (`status`)

**메시지 타입**:
- `ORDER_NOTIFICATION`: 주문 생성 시 발송 허브 담당자에게 알림
- `DAILY_ROUTE`: 매일 06:00 업체 배송 담당자에게 경로 알림 (도전 과제)
- `MANUAL`: 수동 발송

---

## 8. 도전 과제: 업체 배송 경로 기록 테이블 (선택)

### 8.1 업체 배송 경로 기록 테이블 (`p_company_delivery_routes`)

업체 배송 담당자의 하루 배송 경로를 관리합니다. (도전 과제)

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|------------|---------|------|
| `company_route_id` | `UUID` | PRIMARY KEY | 경로 고유 ID |
| `delivery_id` | `UUID` | NOT NULL | 배송 ID (FK → p_deliveries) |
| `origin_hub_id` | `UUID` | NOT NULL | 출발 허브 ID |
| `receiver_company_id` | `UUID` | NOT NULL | 수령 업체 ID |
| `company_personnel_id` | `VARCHAR(100)` | NOT NULL | 업체 배송 담당자 ID |
| `delivery_sequence` | `INTEGER` | NOT NULL | 배송 순서 (AI로 최적화) |
| `expected_distance_km` | `DECIMAL(10, 2)` | NULLABLE | 예상 거리 |
| `expected_duration_minutes` | `INTEGER` | NULLABLE | 예상 소요 시간 |
| `actual_distance_km` | `DECIMAL(10, 2)` | NULLABLE | 실제 거리 |
| `actual_duration_minutes` | `INTEGER` | NULLABLE | 실제 소요 시간 |
| `route_status` | `VARCHAR(50)` | DEFAULT 'WAITING' | 상태 (ENUM: WAITING, IN_PROGRESS, COMPLETED) |
| `started_at` | `TIMESTAMP` | NULLABLE | 시작 시간 |
| `completed_at` | `TIMESTAMP` | NULLABLE | 완료 시간 |
| `delivery_date` | `DATE` | NOT NULL | 배송 날짜 |
| + Audit 필드 | | | |

**인덱스**:
- `idx_company_routes_personnel_date` ON (`company_personnel_id`, `delivery_date`)
- `idx_company_routes_delivery` ON (`delivery_id`)
- `idx_company_routes_sequence` ON (`company_personnel_id`, `delivery_date`, `delivery_sequence`)

**비즈니스 규칙**:
- 매일 06:00 스케줄러가 당일 배송 경로를 AI로 최적화
- delivery_sequence는 AI가 계산한 최적 방문 순서
- Naver Maps Directions 5 API로 경로 및 시간 계산

---

## 9. 데이터 타입 정의 (ENUM)

### 9.1 사용자 관련

**role (사용자 역할)**:
- `MASTER`: 마스터 관리자
- `HUB_MANAGER`: 허브 관리자
- `DELIVERY_MANAGER`: 배송 담당자
- `COMPANY_MANAGER`: 업체 담당자

**status (승인 상태)**:
- `PENDING`: 승인 대기
- `APPROVED`: 승인됨
- `REJECTED`: 거절됨

### 9.2 업체 관련

**company_type (업체 타입)**:
- `SUPPLIER`: 공급 업체
- `RECEIVER`: 수령 업체

### 9.3 배송 관련

**personnel_type (배송 담당자 타입)**:
- `HUB_DELIVERY`: 허브 배송 담당자
- `COMPANY_DELIVERY`: 업체 배송 담당자

**delivery_status (배송 상태)**:
- `HUB_WAITING`: 허브 대기 중
- `HUB_MOVING`: 허브 간 이동 중
- `HUB_ARRIVED`: 목적지 허브 도착
- `COMPANY_MOVING`: 업체 이동 중
- `COMPLETED`: 배송 완료

**route_status (경로 상태)**:
- `WAITING`: 대기 중
- `IN_PROGRESS`: 진행 중
- `COMPLETED`: 완료

### 9.4 주문 관련

**order_status (주문 상태)**:
- `PENDING`: 주문 대기
- `CONFIRMED`: 주문 확정
- `IN_DELIVERY`: 배송 중
- `COMPLETED`: 완료
- `CANCELLED`: 취소됨

### 9.5 메시지 관련

**message_type (메시지 타입)**:
- `ORDER_NOTIFICATION`: 주문 알림
- `DAILY_ROUTE`: 일일 경로 알림
- `MANUAL`: 수동 발송

**message_status (메시지 상태)**:
- `PENDING`: 발송 대기
- `SENT`: 발송 완료
- `FAILED`: 발송 실패

### 9.6 허브 관련

**hub_type (허브 타입)**:
- `REGIONAL`: 일반 허브
- `CENTRAL`: 중앙 허브 (Hub-and-Spoke 모델)

---

## 10. 외래키 제약조건 (FK) 정리

### 주의사항
MSA 환경에서는 물리적 FK 제약조건을 사용하지 않습니다. 대신 API 호출로 데이터 무결성을 검증합니다.

### 논리적 FK 관계

**auth-service**:
- `p_users.hub_id` → hub-service의 `p_hubs.hub_id`
- `p_users.company_id` → company-service의 `p_companies.company_id`

**company-service**:
- `p_companies.hub_id` → hub-service의 `p_hubs.hub_id`

**product-service**:
- `p_products.company_id` → company-service의 `p_companies.company_id`
- `p_products.hub_id` → hub-service의 `p_hubs.hub_id`

**order-service**:
- `p_orders.supplier_company_id` → company-service의 `p_companies.company_id`
- `p_orders.receiver_company_id` → company-service의 `p_companies.company_id`
- `p_orders.product_id` → product-service의 `p_products.product_id`
- `p_orders.delivery_id` → delivery-service의 `p_deliveries.delivery_id`

**delivery-service**:
- `p_delivery_personnel.hub_id` → hub-service의 `p_hubs.hub_id`
- `p_deliveries.order_id` → order-service의 `p_orders.order_id`
- `p_deliveries.origin_hub_id` → hub-service의 `p_hubs.hub_id`
- `p_deliveries.destination_hub_id` → hub-service의 `p_hubs.hub_id`
- `p_delivery_routes.delivery_id` → `p_deliveries.delivery_id`
- `p_delivery_routes.origin_hub_id` → hub-service의 `p_hubs.hub_id`
- `p_delivery_routes.destination_hub_id` → hub-service의 `p_hubs.hub_id`

---

## 11. 데이터 검증 규칙

### 11.1 API 호출을 통한 검증

각 서비스는 다른 서비스의 데이터를 참조할 때 반드시 API 호출로 검증:

1. **업체 생성 시** (company-service):
   - hub_id 검증: `GET /api/hubs/{hub_id}` 호출 (hub-service)

2. **상품 생성 시** (product-service):
   - company_id 검증: `GET /api/companies/{company_id}` 호출 (company-service)
   - hub_id 검증: `GET /api/hubs/{hub_id}` 호출 (hub-service)

3. **주문 생성 시** (order-service):
   - supplier_company_id 검증: `GET /api/companies/{id}` (company-service)
   - receiver_company_id 검증: `GET /api/companies/{id}` (company-service)
   - product_id 검증 및 재고 확인: `GET /api/products/{id}` (product-service)
   - 재고 차감: `PATCH /api/products/{id}/quantity` (product-service)

4. **배송 생성 시** (delivery-service):
   - order_id 검증: `GET /api/orders/{order_id}` (order-service)
   - origin_hub_id 검증: `GET /api/hubs/{hub_id}` (hub-service)
   - destination_hub_id 검증: `GET /api/hubs/{hub_id}` (hub-service)
   - 경로 조회: `GET /api/hubs/routes?from={origin}&to={destination}` (hub-service)

### 11.2 Soft Delete 필터링

모든 조회 쿼리에서 `deleted_at IS NULL` 조건 추가:

```sql
-- 올바른 조회
SELECT * FROM p_users WHERE deleted_at IS NULL;

-- 특정 허브의 업체 조회
SELECT * FROM p_companies
WHERE hub_id = ? AND deleted_at IS NULL;

-- 재고 있는 상품 조회
SELECT * FROM p_products
WHERE quantity > 0 AND deleted_at IS NULL;
```

### 11.3 트랜잭션 처리

MSA 환경에서는 분산 트랜잭션 대신 보상 트랜잭션(Saga Pattern) 사용:

**주문 생성 플로우**:
1. Order 생성 (order-service)
2. 재고 차감 (product-service) - 실패 시 Order 롤백
3. Delivery 생성 (delivery-service) - 실패 시 재고 복원 + Order 롤백
4. Slack 알림 (slack-service) - 실패해도 Order/Delivery는 유지

---

## 12. 성능 최적화 고려사항

### 12.1 캐싱 대상

- **hub-service**: 허브 정보, 허브 간 이동 경로 (TTL: 24시간)
- **product-service**: 상품 정보 (TTL: 1시간)
- **auth-service**: JWT 토큰 검증 결과 (TTL: 토큰 만료 시간)

### 12.2 인덱스 전략

- 검색 조건이 자주 사용되는 컬럼에 인덱스 생성
- 복합 인덱스: (hub_id, deleted_at), (created_at, deleted_at) 등
- UUID 컬럼의 경우 B-tree 인덱스 사용

### 12.3 페이징

- 기본 페이지 크기: 10건
- 지원 크기: 10, 30, 50건
- 정렬: created_at, updated_at (ASC/DESC)

---

## 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2025-10-31 | Claude | 초안 작성 |

---

## 참고 사항

- 이 명세서는 초기 설계이며, 구현 과정에서 변경될 수 있습니다.
- 실제 구현 시 JPA Entity로 변환하여 사용합니다.
- Hub Route Model 선택에 따라 `p_hub_routes` 테이블 데이터가 달라집니다.
- 도전 과제 구현 시 `p_company_delivery_routes` 테이블을 추가합니다.
