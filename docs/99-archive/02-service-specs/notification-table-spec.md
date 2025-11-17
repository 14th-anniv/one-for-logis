# notification-service 테이블 명세서

**최종 수정일**: 2025-11-11  
**작성자**: notification-service 개발팀

---

## 1. p_notifications (알림 메시지 이력)

### 테이블 개요
- **목적**: 모든 알림 메시지 이력 저장 (Slack 발송 내역)
- **특징**: 발신자 정보 스냅샷 저장 (Snapshot Pattern)
- **Soft Delete**: 지원 (`@SQLRestriction("deleted_at IS NULL")`)
- **BaseEntity 상속**: created_at, created_by, updated_at, updated_by, deleted_at, deleted_by

### 컬럼 정의

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| **message_id** | UUID | NOT NULL | UUID.randomUUID() | PK, 메시지 고유 ID |
| **sender_type** | VARCHAR(20) | NOT NULL | - | 발신자 유형 (USER/SYSTEM) |
| **sender_username** | VARCHAR(100) | NULL | - | 발신자 username (USER일 때만) |
| **sender_slack_id** | VARCHAR(100) | NULL | - | 발신자 Slack ID (USER일 때만) |
| **sender_name** | VARCHAR(100) | NULL | - | 발신자 이름 스냅샷 (USER일 때만) |
| **recipient_slack_id** | VARCHAR(100) | NOT NULL | - | 수신자 Slack ID |
| **recipient_name** | VARCHAR(100) | NOT NULL | - | 수신자 이름 스냅샷 |
| **message_content** | TEXT | NOT NULL | - | 메시지 내용 (Slack 발송 텍스트) |
| **message_type** | VARCHAR(30) | NOT NULL | - | 메시지 유형 ENUM |
| **reference_id** | UUID | NULL | - | 연관 엔티티 ID (주문, 배송 등) |
| **event_id** | VARCHAR(100) | NULL (UNIQUE) | - | Kafka 이벤트 ID (멱등성 보장용) |
| **sent_at** | TIMESTAMP | NULL | - | 실제 발송 완료 시각 |
| **status** | VARCHAR(20) | NOT NULL | 'PENDING' | 발송 상태 ENUM |
| **error_message** | TEXT | NULL | - | 발송 실패 시 에러 메시지 |
| **created_at** | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP | 생성 일시 (BaseEntity) |
| **created_by** | VARCHAR(100) | NULL | - | 생성자 (BaseEntity) |
| **updated_at** | TIMESTAMP | NULL | - | 수정 일시 (BaseEntity) |
| **updated_by** | VARCHAR(100) | NULL | - | 수정자 (BaseEntity) |
| **deleted_at** | TIMESTAMP | NULL | - | 삭제 일시 (Soft Delete, BaseEntity) |
| **deleted_by** | VARCHAR(100) | NULL | - | 삭제자 (BaseEntity) |

### ENUM 정의

**sender_type** (`SenderType`):
- `USER`: 사용자가 발송한 메시지 (수동 메시지)
- `SYSTEM`: 시스템이 자동 발송한 메시지 (주문 알림 등)

**message_type** (`MessageType`):
- `ORDER_NOTIFICATION`: 주문 생성 시 출발 마감시간 알림
- `MANUAL`: 사용자가 직접 작성한 수동 메시지
- `DAILY_ROUTE`: 일일 경로 최적화 알림 (Challenge 기능 - 미구현)

**status** (`MessageStatus`):
- `PENDING`: 발송 대기중
- `SENT`: 발송 완료
- `FAILED`: 발송 실패

### 비즈니스 로직 (Entity 메서드)

```java
// 메시지 발송 성공 처리
public void markAsSent() {
    this.status = MessageStatus.SENT;
    this.sentAt = LocalDateTime.now();
    this.errorMessage = null;
}

// 메시지 발송 실패 처리
public void markAsFailed(String errorMessage) {
    this.status = MessageStatus.FAILED;
    this.errorMessage = errorMessage;
}
```

### 제약조건 (@PrePersist, @PreUpdate)

```java
// USER 타입일 경우 sender 정보 필수
if (senderType == SenderType.USER) {
    if (senderUsername == null || senderSlackId == null || senderName == null) {
        throw new IllegalStateException("USER 타입 메시지는 sender 정보가 필수입니다.");
    }
}

// SYSTEM 타입일 경우 sender 정보는 null
if (senderType == SenderType.SYSTEM) {
    if (senderUsername != null || senderSlackId != null || senderName != null) {
        throw new IllegalStateException("SYSTEM 타입 메시지는 sender 정보가 null이어야 합니다.");
    }
}

// 수신자 정보 필수
if (recipientSlackId == null || recipientName == null || messageContent == null) {
    throw new IllegalStateException("필수 필드가 누락되었습니다.");
}
```

### 인덱스

```sql
CREATE INDEX idx_notifications_recipient ON p_notifications(recipient_slack_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_notifications_type ON p_notifications(message_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_notifications_status ON p_notifications(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_notifications_created_at ON p_notifications(created_at DESC);
CREATE INDEX idx_notifications_reference ON p_notifications(reference_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX idx_notifications_event_id ON p_notifications(event_id) WHERE event_id IS NOT NULL;
```

### DDL

```sql
CREATE TABLE p_notifications (
    message_id UUID PRIMARY KEY,
    sender_type VARCHAR(20) NOT NULL CHECK (sender_type IN ('USER', 'SYSTEM')),
    sender_username VARCHAR(100),
    sender_slack_id VARCHAR(100),
    sender_name VARCHAR(100),
    recipient_slack_id VARCHAR(100) NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    message_content TEXT NOT NULL,
    message_type VARCHAR(30) NOT NULL CHECK (message_type IN ('ORDER_NOTIFICATION', 'MANUAL', 'DAILY_ROUTE')),
    reference_id UUID,
    event_id VARCHAR(100) UNIQUE,
    sent_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
);

CREATE INDEX idx_notifications_recipient ON p_notifications(recipient_slack_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_notifications_type ON p_notifications(message_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_notifications_status ON p_notifications(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_notifications_created_at ON p_notifications(created_at DESC);
CREATE INDEX idx_notifications_reference ON p_notifications(reference_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX idx_notifications_event_id ON p_notifications(event_id) WHERE event_id IS NOT NULL;
```

---

## 2. p_external_api_logs (외부 API 호출 로그)

### 테이블 개요
- **목적**: 모든 외부 API 호출 이력 추적 (Slack, Gemini, Naver Maps)
- **특징**: 비용, 성능, 에러 모니터링
- **Soft Delete**: 미지원 (로그성 데이터, BaseEntity 미상속)
- **JSONB 저장**: Hibernate `@JdbcTypeCode(SqlTypes.JSON)` 사용

### 컬럼 정의

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| **log_id** | UUID | NOT NULL | UUID.randomUUID() | PK, 로그 고유 ID |
| **api_provider** | VARCHAR(20) | NOT NULL | - | API 제공자 ENUM |
| **api_method** | VARCHAR(100) | NOT NULL | - | API 메서드/엔드포인트 |
| **request_data** | TEXT (JSONB) | NULL | - | 요청 데이터 (JSON) |
| **response_data** | TEXT (JSONB) | NULL | - | 응답 데이터 (JSON) |
| **http_status** | INTEGER | NULL | - | HTTP 상태 코드 |
| **is_success** | BOOLEAN | NOT NULL | false | 성공 여부 |
| **error_code** | VARCHAR(50) | NULL | - | 에러 코드 (실패 시) |
| **error_message** | TEXT | NULL | - | 에러 메시지 (실패 시) |
| **duration_ms** | BIGINT | NULL | - | 응답 시간 (밀리초) |
| **cost** | DECIMAL(10,4) | NULL | - | API 호출 비용 (USD) |
| **called_at** | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP | 호출 일시 |
| **message_id** | UUID | NULL | - | 연관된 알림 메시지 ID (논리적 FK) |

### ENUM 정의

**api_provider** (`ApiProvider`):
- `SLACK`: Slack API (chat.postMessage)
- `GEMINI`: Google Gemini API (generateContent) - **ChatGPT에서 변경됨**
- `NAVER_MAPS`: Naver Maps Directions 5 API (Challenge용)

### 비즈니스 로직 (Entity 메서드)

```java
// API 호출 성공 처리
public void recordSuccess(Map<String, Object> responseData, Integer httpStatus, Long durationMs) {
    this.isSuccess = true;
    this.responseData = responseData;
    this.httpStatus = httpStatus;
    this.durationMs = durationMs;
    this.errorCode = null;
    this.errorMessage = null;
}

// API 호출 실패 처리
public void recordFailure(String errorCode, String errorMessage, Integer httpStatus, Long durationMs) {
    this.isSuccess = false;
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
    this.httpStatus = httpStatus;
    this.durationMs = durationMs;
}

// API 호출 비용 설정
public void setCost(BigDecimal cost) {
    this.cost = cost;
}
```

### 제약조건 (@PrePersist, @PreUpdate)

```java
if (apiProvider == null || apiMethod == null || calledAt == null || isSuccess == null) {
    throw new IllegalStateException("필수 필드가 누락되었습니다.");
}
```

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
    log_id UUID PRIMARY KEY,
    api_provider VARCHAR(20) NOT NULL CHECK (api_provider IN ('SLACK', 'GEMINI', 'NAVER_MAPS')),
    api_method VARCHAR(100) NOT NULL,
    request_data TEXT,  -- PostgreSQL에서 JSONB로 자동 변환
    response_data TEXT,
    http_status INTEGER,
    is_success BOOLEAN NOT NULL DEFAULT false,
    error_code VARCHAR(50),
    error_message TEXT,
    duration_ms BIGINT,
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

## 테이블 관계도

```
p_notifications (1) ─────< (0..N) p_external_api_logs
    │                              [message_id]
    │
    └── reference_id (논리적 FK)
        ├─> p_order.order_id (주문 알림)
        └─> p_delivery.delivery_id (배송 관련)

p_external_api_logs
    ├── message_id ─────> p_notifications.message_id (논리적 FK)
    └── (독립적 API 호출 로그도 존재 가능, message_id = NULL)
```

**참고**:
- 모든 FK는 **논리적 FK**로 관리 (물리적 FK 제약 없음)
- MSA 아키텍처 특성상 서비스 간 직접 참조 불가
- `reference_id`는 다양한 엔티티를 참조 가능 (polymorphic association)

---

## 주요 변경사항 (2025-11-11)

### 1. p_notifications 테이블
- ✅ **event_id 필드 추가**: Kafka 이벤트 멱등성 보장 (UNIQUE 제약)
- ✅ **컬럼 길이 조정**: VARCHAR(50) → VARCHAR(100) (sender/recipient 필드)
- ✅ **제약조건 강화**: Entity 레벨 @PrePersist/@PreUpdate 검증 추가
- ✅ **인덱스 최적화**: Partial index 사용 (WHERE deleted_at IS NULL)

### 2. p_external_api_logs 테이블
- ✅ **API 제공자 변경**: CHATGPT → GEMINI (Google Gemini API 사용)
- ✅ **duration_ms 타입 변경**: INTEGER → BIGINT
- ✅ **JSONB 처리**: Hibernate @JdbcTypeCode 사용 (TEXT 컬럼 → JSONB 매핑)
- ✅ **BaseEntity 미상속**: 로그성 데이터 특성상 생략

### 3. p_company_delivery_routes 테이블
- ❌ **제거**: Challenge 기능 미구현으로 제외

---

## 데이터 예시

### p_notifications 예시

```sql
-- SYSTEM 메시지 (주문 알림)
INSERT INTO p_notifications VALUES (
    '550e8400-e29b-41d4-a716-446655440000',
    'SYSTEM',
    NULL,
    NULL,
    NULL,
    'U01234ABCDE',
    '김관리',
    '🚚 *신규 주문 배송 알림*

📦 주문 정보:
- 주문자: 김말숙 / msk@seafood.world
- 공급업체: 건조 식품 가공 업체

⏰ *최종 발송 시한: 2025-12-11 14:30*',
    'ORDER_NOTIFICATION',
    '650e8400-e29b-41d4-a716-446655440000',
    NULL,
    '2025-11-07 10:30:00',
    'SENT',
    NULL,
    '2025-11-07 10:25:00',
    'system',
    '2025-11-07 10:30:00',
    'system',
    NULL, NULL
);

-- USER 메시지 (수동 발송)
INSERT INTO p_notifications VALUES (
    '750e8400-e29b-41d4-a716-446655440000',
    'USER',
    'user1',
    'U98765ZYXWV',
    '김발신',
    'U01234ABCDE',
    '김담당',
    '긴급 배송 건이 추가되었습니다. 확인 부탁드립니다.',
    'MANUAL',
    NULL,
    NULL,
    '2025-11-07 11:00:00',
    'SENT',
    NULL,
    '2025-11-07 11:00:00',
    'user1',
    '2025-11-07 11:00:00',
    'user1',
    NULL, NULL
);
```

### p_external_api_logs 예시

```sql
-- Gemini API 호출 (발송 시한 계산)
INSERT INTO p_external_api_logs VALUES (
    '850e8400-e29b-41d4-a716-446655440000',
    'GEMINI',
    'generateContent',
    '{"contents":[{"parts":[{"text":"배송 시한 계산..."}]}],"generationConfig":{"temperature":0.2}}'::jsonb,
    '{"candidates":[{"content":{"parts":[{"text":"2025-12-11 14:30"}]}}],"usageMetadata":{"promptTokenCount":150,"candidatesTokenCount":10,"totalTokenCount":160}}'::jsonb,
    200,
    true,
    NULL,
    NULL,
    3456,
    0.0000030,
    '2025-11-07 10:29:55',
    '550e8400-e29b-41d4-a716-446655440000'
);

-- Slack API 호출 (메시지 발송)
INSERT INTO p_external_api_logs VALUES (
    '950e8400-e29b-41d4-a716-446655440000',
    'SLACK',
    'chat.postMessage',
    '{"channel":"U01234ABCDE","text":"..."}'::jsonb,
    '{"ok":true,"ts":"1699084800.123456"}'::jsonb,
    200,
    true,
    NULL,
    NULL,
    1250,
    NULL,
    '2025-11-07 10:30:00',
    '550e8400-e29b-41d4-a716-446655440000'
);
```

---

## 변경 이력

| 날짜 | 작성자 | 변경 내용 |
|------|--------|----------|
| 2025-11-04 | Team | 초안 작성 |
| 2025-11-11 | notification-service | Entity 기반 최신화 (event_id 추가, GEMINI 변경, 제약조건 강화) |
