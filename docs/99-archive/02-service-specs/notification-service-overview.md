# Notification Service Overview

## 1. 서비스 개요

**notification-service**는 14logis 물류 시스템의 알림 및 AI 기반 배송 정보 생성을 담당하는 마이크로서비스입니다.

### 1.1 주요 역할

- **실시간 알림 발송**: Slack API를 통한 배송 관련 알림 전송
- **AI 기반 배송 시간 계산**: Gemini AI를 활용한 최적 발송 시한 계산
- **메시지 이력 관리**: 발송된 모든 Slack 메시지 기록 및 상태 관리
- **외부 API 호출 모니터링**: Slack, Gemini AI, Naver Maps API 호출 이력 추적
- **일일 경로 최적화 알림** (도전 과제): AI 기반 배송 순서 최적화 및 자동 알림

### 1.2 서비스 위치

| 항목 | 값 |
|------|-----|
| 포트 | 8087 |
| 데이터베이스 스키마 | `notification_db` |
| 관리 테이블 | `p_notifications`, `p_external_api_logs` |
| 서비스 디스커버리 | Eureka Client |
| API Gateway 경로 | `/api/v1/notifications/*` |
| API 버전 | v1 |

---

## 2. 핵심 기능

### 2.1 주문 생성 시 자동 알림 (필수 기능)

**트리거**: 주문 생성 시 order-service에서 호출

**처리 흐름**:
1. order-service로부터 주문 정보 수신
2. Gemini AI에 배송 정보 전달하여 최종 발송 시한 계산
3. AI 응답 기반으로 알림 메시지 생성
4. 발송 허브 담당자의 Slack ID로 메시지 발송
5. 발송 결과를 `p_notifications` 테이블에 저장

**AI 입력 데이터**:
- 상품명 및 수량
- 주문 요청 사항 (납기 일자 및 시간)
- 발송지, 경유지, 도착지 허브 정보
- 배송 담당자 근무 시간 (09:00 - 18:00)

**AI 출력 데이터**:
- 최종 발송 시한 (예: "12월 10일 오전 9시")
- (선택) 추가 물류 정보

**알림 메시지 예시**:
```
주문 번호: ORD-2025-001
주문자 정보: 김말숙 / U01234ABC (Slack ID)
주문 시간: 2025-12-08 10:00:00
상품 정보: 마른 오징어 50박스
요청 사항: 12월 12일 3시까지는 보내주세요!
발송지: 경기 북부 센터
경유지: 대전광역시 센터, 부산광역시 센터
도착지: 부산시 사하구 낙동대로 1번길 1 해산물월드
배송담당자: 고길동 / U05678DEF

📦 AI 분석 결과
위 내용을 기반으로 도출된 최종 발송 시한은 12월 10일 오전 9시입니다.
```

### 2.2 수동 메시지 발송

**권한**: 모든 로그인 사용자

**기능**:
- 사용자가 직접 Slack 메시지 발송 가능
- 메시지 타입: `MANUAL`
- 용도: 긴급 공지, 수동 알림 등

### 2.3 일일 경로 최적화 알림 (도전 과제)

**트리거**: 매일 06:00 (스케줄러)

**처리 흐름**:
1. 당일 배송 예정인 업체 배송 담당자별 배송 목록 조회
2. Gemini AI에 배송 주소(위경도) 전달하여 최적 방문 순서 계산
3. 네이버 Maps Directions 5 API로 경로 및 소요 시간 계산
4. AI를 통해 메시지 생성
5. 각 업체 배송 담당자의 Slack ID로 발송
6. `p_company_delivery_routes` 테이블에 경로 정보 저장

**구현 고려사항**:
- 스케줄러 발송 시각을 `application.yml`에서 설정 가능하도록 구성
- AI 기반 TSP(Traveling Salesman Problem) 해결
- Naver Maps API waypoints 파라미터 활용

---

## 3. 외부 API 통합

### 3.1 Slack API

**사용 API**: Slack Web API - `chat.postMessage`

**주요 설정**:
```yaml
slack:
  token: ${SLACK_BOT_TOKEN}  # xoxb-로 시작하는 Bot User OAuth Token
  workspace-url: https://app.slack.com/client/{WORKSPACE_ID}
```

**구현 방법**:
- Slack SDK for Java 또는 RestTemplate/WebClient 사용
- Bot Token Scopes 필요 권한: `chat:write`, `users:read`

**에러 처리**:
- 발송 실패 시 `p_slack_messages.status = 'FAILED'`
- 재시도 로직 구현 (최대 3회, Exponential Backoff)
- 실패 사유를 `error_message` 필드에 저장

### 3.2 Gemini AI API

**사용 모델**: `gemini-pro` 또는 `gemini-1.5-flash`

**주요 설정**:
```yaml
gemini:
  api-key: ${GEMINI_API_KEY}
  model: gemini-1.5-flash
  base-url: https://generativelanguage.googleapis.com/v1beta
```

**프롬프트 설계 예시**:

**1) 주문 생성 시 최종 발송 시한 계산**
```
당신은 물류 전문가입니다. 다음 배송 정보를 분석하여 최종 발송 시한을 계산해주세요.

[배송 정보]
- 상품: 마른 오징어 50박스
- 납기 기한: 2025-12-12 15:00
- 경로:
  * 출발: 경기 북부 센터 (37.6584, 126.8320)
  * 경유1: 대전광역시 센터 (36.3504, 127.3845) - 예상 3시간
  * 경유2: 부산광역시 센터 (35.1796, 129.0756) - 예상 2시간 30분
  * 도착: 부산 사하구 (35.0956, 128.9740) - 예상 40분
- 배송 담당자 근무 시간: 09:00 - 18:00
- 허브 간 대기 시간: 각 허브당 1시간

위 정보를 바탕으로:
1. 총 소요 시간 계산
2. 역산하여 최종 발송 시한 도출
3. 결과를 "YYYY-MM-DD HH:mm" 형식으로 반환

답변 형식:
최종 발송 시한: YYYY-MM-DD HH:mm
근거: [계산 과정 요약]
```

**2) 일일 경로 최적화 (도전 과제)**
```
당신은 배송 경로 최적화 전문가입니다. 다음 배송지들의 최적 방문 순서를 계산해주세요.

[출발지]
경기 남부 센터 (37.2724, 127.4357)

[배송지 목록]
1. A업체: (37.5665, 126.9780)
2. B업체: (37.4563, 127.1357)
3. C업체: (37.2936, 127.0089)
4. D업체: (37.4012, 127.1086)

[제약 조건]
- 09:00 출발, 18:00까지 복귀
- 각 배송지 체류 시간: 20분
- 최단 거리 우선

최적 방문 순서를 번호로만 반환해주세요 (예: 3,1,4,2)
```

**응답 파싱**:
- JSON 응답에서 텍스트 추출
- 정규표현식으로 날짜/시간 파싱
- 에러 처리 및 기본값 설정

### 3.3 Naver Maps Directions 5 API (도전 과제)

**엔드포인트**: `https://naveropenapi.apigw.ntruss.com/map-direction/v1/driving`

**주요 설정**:
```yaml
naver:
  maps:
    client-id: ${NAVER_MAPS_CLIENT_ID}
    client-secret: ${NAVER_MAPS_CLIENT_SECRET}
```

**요청 파라미터**:
- `start`: 출발지 위경도 (경도,위도)
- `goal`: 도착지 위경도
- `waypoints`: 경유지 (최대 5개, 형식: "경도,위도|경도,위도")
- `option`: `traoptimal` (실시간 교통 최적)

**응답 데이터 활용**:
- `route.traoptimal[0].summary.duration`: 총 소요 시간 (ms)
- `route.traoptimal[0].summary.distance`: 총 거리 (m)
- 각 구간별 거리/시간 정보

---

## 4. 데이터베이스 설계

### 4.1 p_notifications 테이블

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|------------|---------|------|
| `message_id` | `UUID` | PRIMARY KEY | 메시지 고유 ID |
| `sender_type` | `VARCHAR(50)` | NOT NULL | 발신자 타입 (ENUM: USER, SYSTEM) |
| `sender_username` | `VARCHAR(100)` | NULLABLE | 발신자 사용자명 (USER인 경우만, 스냅샷) |
| `sender_slack_id` | `VARCHAR(100)` | NULLABLE | 발신자 Slack ID (USER인 경우만, 스냅샷) |
| `sender_name` | `VARCHAR(100)` | NULLABLE | 발신자 이름 (USER인 경우만, 스냅샷) |
| `recipient_slack_id` | `VARCHAR(100)` | NOT NULL | 수신자 Slack ID |
| `recipient_name` | `VARCHAR(100)` | NULLABLE | 수신자 이름 |
| `message_content` | `TEXT` | NOT NULL | 메시지 내용 |
| `message_type` | `VARCHAR(50)` | NOT NULL | 메시지 타입 (ENUM) |
| `reference_id` | `UUID` | NULLABLE | 참조 ID (주문 ID, 배송 ID 등) |
| `sent_at` | `TIMESTAMP` | NOT NULL | 발송 시간 |
| `status` | `VARCHAR(50)` | DEFAULT 'PENDING' | 발송 상태 (ENUM) |
| `error_message` | `TEXT` | NULLABLE | 오류 메시지 |
| + Audit 필드 | | | created_at, created_by, updated_at, updated_by, deleted_at, deleted_by |

**인덱스**:
- `idx_notifications_sender_username` ON (`sender_username`)
- `idx_notifications_sender_slack_id` ON (`sender_slack_id`)
- `idx_notifications_sender_type` ON (`sender_type`)
- `idx_notifications_recipient` ON (`recipient_slack_id`)
- `idx_notifications_type` ON (`message_type`)
- `idx_notifications_reference` ON (`reference_id`)
- `idx_notifications_sent_at` ON (`sent_at`)
- `idx_notifications_status` ON (`status`)

**ENUM 정의**:

**sender_type**:
- `USER`: 사용자가 수동으로 발송 (sender_username 필수)
- `SYSTEM`: 시스템 자동 발송 (주문 알림, 일일 경로 알림 등)

**message_type**:
- `ORDER_NOTIFICATION`: 주문 생성 알림 (SYSTEM)
- `DAILY_ROUTE`: 일일 경로 알림 (SYSTEM, 도전 과제)
- `MANUAL`: 수동 발송 (USER)

**status**:
- `PENDING`: 발송 대기
- `SENT`: 발송 완료
- `FAILED`: 발송 실패

**비즈니스 규칙**:
- `sender_type = SYSTEM`인 경우:
  - `sender_username = NULL`
  - `sender_slack_id = NULL`
  - `sender_name = NULL`
  - `message_type = ORDER_NOTIFICATION` 또는 `DAILY_ROUTE`
  - 시스템 자동 발송이므로 발신자 정보 불필요

- `sender_type = USER`인 경우:
  - `sender_username` 필수 (Gateway에서 전달)
  - `sender_slack_id` 필수 (auth-service에서 조회하여 스냅샷 저장)
  - `sender_name` 필수 (auth-service에서 조회하여 스냅샷 저장)
  - `message_type = MANUAL`
  - **스냅샷 저장 이유**:
    - 메시지 발송 시점의 사용자 정보 영구 보존 (감사 로그)
    - 사용자 정보 변경되어도 과거 메시지는 원래 정보 유지
    - 사용자 삭제되어도 메시지 이력 조회 가능
    - Slack 답장/멘션 기능 구현 가능

**JPA 엔티티 예시**:
```java
@Entity
@Table(name = "p_notifications")
@Where(clause = "deleted_at IS NULL")  // Soft Delete 필터
public class Notification {

    @Id
    @GeneratedValue
    private UUID messageId;

    // 발신자 정보
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SenderType senderType;

    @Column(length = 100)
    private String senderUsername;  // USER인 경우만 저장 (스냅샷)

    @Column(length = 100)
    private String senderSlackId;   // USER인 경우만 저장 (스냅샷)

    @Column(length = 100)
    private String senderName;      // USER인 경우만 저장 (스냅샷)

    // 수신자 정보
    @Column(nullable = false, length = 100)
    private String recipientSlackId;

    @Column(length = 100)
    private String recipientName;

    // 메시지 정보
    @Column(nullable = false, columnDefinition = "TEXT")
    private String messageContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MessageType messageType;

    private UUID referenceId;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MessageStatus status = MessageStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    // Audit 필드
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private LocalDateTime deletedAt;
    private String deletedBy;

    // 발신자 정보 설정 헬퍼 메서드
    public void setSenderAsSystem() {
        this.senderType = SenderType.SYSTEM;
        this.senderUsername = null;
        this.senderSlackId = null;
        this.senderName = null;
    }

    public void setSenderAsUser(String username, String slackId, String name) {
        this.senderType = SenderType.USER;
        this.senderUsername = username;
        this.senderSlackId = slackId;
        this.senderName = name;
    }

    // 검증 메서드
    @PrePersist
    @PreUpdate
    public void validateSenderInfo() {
        if (senderType == SenderType.USER) {
            if (senderUsername == null || senderSlackId == null || senderName == null) {
                throw new IllegalStateException(
                    "USER 타입 메시지는 sender_username, sender_slack_id, sender_name이 필수입니다."
                );
            }
        }
    }
}
```

### 4.2 p_external_api_logs 테이블

외부 API(Slack, Gemini AI, Naver Maps) 호출 이력을 추적합니다.

| 필드명 | 데이터 타입 | 제약조건 | 설명 |
|--------|------------|---------|------|
| `log_id` | `UUID` | PRIMARY KEY | 로그 고유 ID |
| `api_provider` | `VARCHAR(50)` | NOT NULL | API 제공자 (ENUM: SLACK, GEMINI, NAVER_MAPS) |
| `api_method` | `VARCHAR(100)` | NOT NULL | API 메서드명 (예: chat.postMessage, generateContent) |
| `request_data` | `JSONB` | NULLABLE | 요청 데이터 (JSON 형식) |
| `response_data` | `JSONB` | NULLABLE | 응답 데이터 (JSON 형식) |
| `http_status` | `INTEGER` | NULLABLE | HTTP 상태 코드 |
| `is_success` | `BOOLEAN` | NOT NULL | 성공 여부 |
| `error_code` | `VARCHAR(50)` | NULLABLE | 에러 코드 |
| `error_message` | `TEXT` | NULLABLE | 에러 메시지 |
| `duration_ms` | `INTEGER` | NULLABLE | 응답 소요 시간 (밀리초) |
| `cost` | `DECIMAL(10, 4)` | NULLABLE | API 호출 비용 (달러) |
| `called_at` | `TIMESTAMP` | NOT NULL | 호출 시간 |
| `message_id` | `UUID` | NULLABLE | 연관된 메시지 ID (FK → p_slack_messages) |
| + Audit 필드 | | | created_at, created_by |

**인덱스**:
- `idx_api_logs_provider` ON (`api_provider`)
- `idx_api_logs_called_at` ON (`called_at`)
- `idx_api_logs_success` ON (`is_success`)
- `idx_api_logs_message_id` ON (`message_id`)
- `idx_api_logs_provider_date` ON (`api_provider`, `called_at`)

**ENUM 정의**:

**api_provider**:
- `SLACK`: Slack API 호출
- `GEMINI`: Google Gemini AI API 호출
- `NAVER_MAPS`: Naver Maps Directions 5 API 호출

**비즈니스 규칙**:
- 모든 외부 API 호출 시 자동으로 로그 생성
- 성공/실패 여부와 관계없이 모든 호출 기록
- `request_data`, `response_data`는 민감 정보 마스킹 후 저장 (API Key 등)
- 비용 추적을 위해 API 제공자별 요금 정보 저장 (Gemini API는 무료 할당량 추적 가능)
- 성능 모니터링을 위해 `duration_ms` 기록
- 30일 이상 지난 로그는 아카이빙 또는 삭제 정책 적용 (선택)

**FK(Foreign Key) 정책**:
- **물리적 FK 사용하지 않음**: MSA 환경에서 서비스 간 독립성 유지
- `message_id`는 `p_notifications`를 참조하지만 **논리적 FK**로만 관리
- Soft Delete된 메시지도 감사 목적으로 API 로그는 유지
- 애플리케이션 레벨에서 참조 무결성 검증

**용도**:
- API 호출 실패 디버깅 및 재시도 로직 개선
- Gemini AI 사용량 모니터링 (일일 할당량 추적)
- API 응답 시간 분석 및 성능 최적화
- 비용 관리 및 예산 추적
- 감사 로그 (컴플라이언스)

**JPA 엔티티 예시**:
```java
@Entity
@Table(name = "p_external_api_logs")
public class ExternalApiLog {

    @Id
    @GeneratedValue
    private UUID logId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApiProvider apiProvider;

    @Column(nullable = false, length = 100)
    private String apiMethod;

    @Column(columnDefinition = "jsonb")
    private String requestData;

    @Column(columnDefinition = "jsonb")
    private String responseData;

    private Integer httpStatus;

    @Column(nullable = false)
    private Boolean isSuccess;

    private String errorCode;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Integer durationMs;

    @Column(precision = 10, scale = 4)
    private BigDecimal cost;

    @Column(nullable = false)
    private LocalDateTime calledAt;

    // FK로 설정하지 않음 (논리적 참조만)
    private UUID messageId;

    // Audit 필드
    private LocalDateTime createdAt;
    private String createdBy;

    // Note: updated_at, deleted_at은 API 로그 특성상 불필요
    // (한번 기록되면 수정/삭제되지 않음)
}
```

### 4.3 스냅샷 저장 정책 (Snapshot Pattern)

**발신자 정보를 스냅샷으로 저장하는 이유**:

notification-service는 **감사 로그(Audit Log)** 성격을 가지므로, 메시지 발송 시점의 정보를 영구 보존해야 합니다.

**장점**:
1. **시점 정보 보존**: 메시지 발송 당시의 사용자 정보를 정확히 기록
2. **독립성**: auth-service 장애 시에도 메시지 이력 조회 가능
3. **성능**: 매번 auth-service 조회 불필요, 빠른 조회
4. **데이터 일관성**: 사용자 정보 변경되어도 과거 메시지는 원본 유지
5. **삭제 대응**: 사용자 삭제되어도 메시지 이력은 계속 유지

**예시 시나리오**:
```
2025-01-01: 홍길동(user123, U99988ABC) -> "긴급 공지" 발송
2025-06-01: 사용자가 이름을 "홍길동" -> "홍두께"로 변경
2025-12-01: 2025-01-01 메시지 조회 시 여전히 "홍길동"으로 표시 (스냅샷)
```

**스냅샷 대상**:
- ✅ `sender_username`: 사용자 ID (변경 가능성 낮음)
- ✅ `sender_slack_id`: Slack ID (변경 가능성 있음)
- ✅ `sender_name`: 사용자 이름 (변경 가능성 높음)
- ✅ `recipient_name`: 수신자 이름 (스냅샷)

**스냅샷 미적용 대상**:
- ❌ `reference_id`: 주문/배송 ID (불변 참조)
- ❌ Audit 필드: 생성/수정자 정보 (불변)

### 4.4 p_company_delivery_routes 테이블 (도전 과제)

테이블 명세는 `table-specifications.md` 참조 (8.1절)

---

## 5. API 명세

### 5.1 주문 알림 생성 (Internal API)

**목적**: order-service에서 주문 생성 시 호출

| 메서드 | 요청 URL |
|--------|----------|
| POST | `http://localhost:8087/api/v1/notifications/order` |

**권한**: Internal Service Only (order-service)

**Request Header**:
```
X-User-Id: order-service
Content-Type: application/json
```

**Request Body**:
```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "orderTime": "2025-12-08T10:00:00",
  "product": {
    "name": "마른 오징어",
    "quantity": 50,
    "unit": "박스"
  },
  "requestNotes": "12월 12일 3시까지는 보내주세요!",
  "deadline": "2025-12-12T15:00:00",
  "route": {
    "origin": {
      "hubId": "hub-001",
      "hubName": "경기 북부 센터"
    },
    "waypoints": [
      {
        "hubId": "hub-008",
        "hubName": "대전광역시 센터",
        "expectedDuration": 180
      },
      {
        "hubId": "hub-004",
        "hubName": "부산광역시 센터",
        "expectedDuration": 150
      }
    ],
    "destination": {
      "address": "부산시 사하구 낙동대로 1번길 1",
      "companyName": "해산물월드"
    }
  },
  "deliveryPersonnel": {
    "name": "고길동",
    "slackId": "U05678DEF"
  },
  "hubManager": {
    "name": "김허브",
    "slackId": "U01234ABC"
  }
}
```

**Response**:
```json
{
  "messageId": "msg-550e8400-e29b-41d4-a716-446655440000",
  "status": "SENT",
  "sentAt": "2025-12-08T10:05:32",
  "aiCalculatedDeadline": "2025-12-10T09:00:00"
}
```

### 5.2 수동 메시지 발송

| 메서드 | 요청 URL |
|--------|----------|
| POST | `http://localhost:8080/api/v1/notifications/messages` |

**권한**: ALL (모든 로그인 사용자)
- `MASTER`
- `HUB_MANAGER`
- `DELIVERY_MANAGER`
- `COMPANY_MANAGER`

**Request Header**:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6InVzZXIxMjMiLCJyb2xlIjoiSFVCX01BTkFHRVIifQ...
Content-Type: application/json
```

**Request Body**:
```json
{
  "recipientSlackId": "U01234ABC",
  "recipientName": "김허브",
  "message": "긴급 공지: 오늘 배송 일정이 변경되었습니다."
}
```

**처리 로직**:
1. Gateway에서 전달된 `X-User-Id` 헤더로 발신자 username 확인
2. auth-service 호출하여 발신자 전체 정보 조회 (FeignClient):
   ```java
   UserResponse user = authServiceClient.getUser(username);
   // user.getUsername(), user.getSlackId(), user.getName()
   ```
3. 발신자 정보를 **스냅샷으로 저장**:
   ```java
   message.setSenderAsUser(
       user.getUsername(),
       user.getSlackId(),  // ⭐ Slack ID도 함께 저장
       user.getName()
   );
   ```
4. `message_type = MANUAL` 설정
5. Slack API로 메시지 발송
6. DB에 저장 (발신자 정보 스냅샷 포함)

**Response**:
```json
{
  "messageId": "msg-660e8400-e29b-41d4-a716-446655440001",
  "senderType": "USER",
  "senderUsername": "user123",
  "senderSlackId": "U99988ABC",
  "senderName": "홍길동",
  "status": "SENT",
  "sentAt": "2025-12-08T14:30:00"
}
```

### 5.3 메시지 이력 조회

| 메서드 | 요청 URL |
|--------|----------|
| GET | `http://localhost:8080/api/v1/notifications/messages` |

**권한**: `MASTER` (마스터 관리자만 전체 조회 가능)

**Request Header**:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6ImFkbWluIiwicm9sZSI6Ik1BU1RFUiJ9...
```

**Query Parameters**:
| 파라미터 | 타입 | 필수여부 | 설명 |
|---------|------|---------|------|
| messageType | String | 선택 | 메시지 타입 필터 |
| status | String | 선택 | 발송 상태 필터 |
| startDate | String | 선택 | 조회 시작일 (YYYY-MM-DD) |
| endDate | String | 선택 | 조회 종료일 (YYYY-MM-DD) |
| page | Integer | 선택 | 페이지 번호 (기본: 0) |
| size | Integer | 선택 | 페이지 크기 (10, 30, 50, 기본: 10) |
| sort | String | 선택 | 정렬 (sent_at,desc 또는 created_at,asc) |

**Response**:
```json
{
  "content": [
    {
      "messageId": "msg-550e8400-e29b-41d4-a716-446655440000",
      "senderType": "SYSTEM",
      "senderUsername": null,
      "senderSlackId": null,
      "senderName": null,
      "recipientSlackId": "U01234ABC",
      "recipientName": "김허브",
      "messageType": "ORDER_NOTIFICATION",
      "status": "SENT",
      "sentAt": "2025-12-08T10:05:32",
      "referenceId": "order-550e8400-e29b-41d4-a716-446655440000"
    },
    {
      "messageId": "msg-660e8400-e29b-41d4-a716-446655440001",
      "senderType": "USER",
      "senderUsername": "user123",
      "senderSlackId": "U99988ABC",
      "senderName": "홍길동",
      "recipientSlackId": "U05678DEF",
      "recipientName": "김배송",
      "messageType": "MANUAL",
      "status": "SENT",
      "sentAt": "2025-12-08T14:30:00",
      "referenceId": null
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 45,
  "totalPages": 5
}
```

### 5.4 API 호출 로그 조회

| 메서드 | 요청 URL |
|--------|----------|
| GET | `http://localhost:8080/api/v1/notifications/api-logs` |

**권한**: `MASTER` (마스터 관리자만 조회 가능)

**Request Header**:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6ImFkbWluIiwicm9sZSI6Ik1BU1RFUiJ9...
```

**Query Parameters**:
| 파라미터 | 타입 | 필수여부 | 설명 |
|---------|------|---------|------|
| apiProvider | String | 선택 | API 제공자 (SLACK, GEMINI, NAVER_MAPS) |
| isSuccess | Boolean | 선택 | 성공 여부 필터 |
| startDate | String | 선택 | 조회 시작일 (YYYY-MM-DD) |
| endDate | String | 선택 | 조회 종료일 (YYYY-MM-DD) |
| page | Integer | 선택 | 페이지 번호 (기본: 0) |
| size | Integer | 선택 | 페이지 크기 (10, 30, 50, 기본: 10) |
| sort | String | 선택 | 정렬 (called_at,desc 또는 duration_ms,asc) |

**Response**:
```json
{
  "content": [
    {
      "logId": "log-550e8400-e29b-41d4-a716-446655440000",
      "apiProvider": "SLACK",
      "apiMethod": "chat.postMessage",
      "httpStatus": 200,
      "isSuccess": true,
      "durationMs": 245,
      "cost": 0.0000,
      "calledAt": "2025-12-08T10:05:32",
      "messageId": "msg-550e8400-e29b-41d4-a716-446655440000"
    },
    {
      "logId": "log-660e8400-e29b-41d4-a716-446655440001",
      "apiProvider": "GEMINI",
      "apiMethod": "generateContent",
      "httpStatus": 200,
      "isSuccess": true,
      "durationMs": 1823,
      "cost": 0.0025,
      "calledAt": "2025-12-08T10:05:30",
      "messageId": "msg-550e8400-e29b-41d4-a716-446655440000"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 150,
  "totalPages": 15
}
```

### 5.5 API 통계 조회

| 메서드 | 요청 URL |
|--------|----------|
| GET | `http://localhost:8080/api/v1/notifications/api-logs/stats` |

**권한**: `MASTER` (마스터 관리자만 조회 가능)

**Request Header**:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6ImFkbWluIiwicm9sZSI6Ik1BU1RFUiJ9...
```

**Query Parameters**:
| 파라미터 | 타입 | 필수여부 | 설명 |
|---------|------|---------|------|
| apiProvider | String | 필수 | API 제공자 (SLACK, GEMINI, NAVER_MAPS) |
| date | String | 필수 | 조회 일자 (YYYY-MM-DD) |

**Response**:
```json
{
  "apiProvider": "GEMINI",
  "date": "2025-12-08",
  "totalCalls": 45,
  "successCalls": 43,
  "failedCalls": 2,
  "successRate": 95.56,
  "averageDurationMs": 1532,
  "totalCost": 0.1125,
  "errorSummary": [
    {
      "errorCode": "QUOTA_EXCEEDED",
      "count": 1
    },
    {
      "errorCode": "TIMEOUT",
      "count": 1
    }
  ]
}
```

### 5.6 일일 경로 알림 (도전 과제 - Internal Scheduler)

**트리거**: Spring Scheduler (@Scheduled)

**권한**: Internal System Only

**처리 로직**:
1. 당일 배송 목록 조회 (delivery-service 호출)
2. 업체 배송 담당자별 그룹핑
3. Gemini AI로 최적 방문 순서 계산
4. Naver Maps API로 경로 및 시간 계산
5. AI로 메시지 생성 후 Slack 발송
6. `p_company_delivery_routes`에 저장

---

## 6. 권한 관리

| 역할 | 생성 | 수정 | 삭제 | 조회 및 검색 |
|------|------|------|------|-------------|
| `마스터 관리자` | O | O | O | O |
| `허브 관리자` | O | X | X | X |
| `배송 담당자` | O | X | X | X |
| `업체 담당자` | O | X | X | X |

**설명**:
- **생성**: 모든 로그인 사용자 및 내부 시스템 (order-service, delivery-service 등)
- **수정, 삭제**: 마스터 관리자만 가능 (메시지 재발송, 이력 관리 용도)
- **조회 및 검색**: 마스터 관리자만 가능 (감사 및 모니터링 용도)

---

## 7. 비즈니스 플로우

### 7.1 주문 생성 시 알림 플로우

```
[order-service]
    ↓ (1) 주문 생성 완료
    ↓ POST /api/v1/notifications/order
[notification-service]
    ↓ (2) Gemini AI 호출
    ↓ (발송 시한 계산)
[Gemini AI]
    ↓ (3) 응답: "2025-12-10 09:00"
[notification-service]
    ↓ (4) 메시지 생성
    ↓ (5) Slack API 호출
[Slack]
    ↓ (6) 허브 담당자에게 메시지 전송
[notification-service]
    ↓ (7) p_notifications에 저장
    ↓ (status: SENT)
[order-service]
    ← (8) 응답 반환
```

### 7.2 일일 경로 알림 플로우 (도전 과제)

```
[Spring Scheduler]
    ↓ (1) 매일 06:00 실행
[notification-service]
    ↓ (2) delivery-service 호출
    ↓ (당일 배송 목록 조회)
[delivery-service]
    ↓ (3) 배송 목록 반환
[notification-service]
    ↓ (4) 업체 배송 담당자별 그룹핑
    ↓ (5) Gemini AI 호출 (최적 순서 계산)
[Gemini AI]
    ↓ (6) 방문 순서: "3,1,4,2"
[notification-service]
    ↓ (7) Naver Maps API 호출
    ↓ (waypoints 파라미터 사용)
[Naver Maps API]
    ↓ (8) 경로 및 시간 반환
[notification-service]
    ↓ (9) p_company_delivery_routes에 저장
    ↓ (10) Gemini AI로 메시지 생성
    ↓ (11) Slack API 발송
[Slack]
    ↓ (12) 각 배송 담당자에게 메시지 전송
[notification-service]
    ↓ (13) p_notifications에 저장
```

---

## 8. 기술 스택

### 8.1 프레임워크 및 라이브러리

| 기술 | 용도 |
|------|------|
| Spring Boot 3.x | 서비스 기반 |
| Spring Cloud Eureka Client | 서비스 디스커버리 |
| Spring Cloud OpenFeign | 서비스 간 통신 (delivery-service, order-service) |
| Spring Web | REST API |
| Spring Data JPA | 데이터 접근 계층 |
| PostgreSQL | 데이터베이스 |
| Spring Scheduler | 일일 알림 스케줄링 |

### 8.2 외부 API 클라이언트

| API | 구현 방법 | 라이브러리 |
|-----|---------|-----------|
| Slack API | WebClient 또는 RestTemplate | Spring WebFlux 또는 Slack Java SDK |
| Gemini AI | RestTemplate/WebClient | Spring Web |
| Naver Maps | RestTemplate/WebClient | Spring Web |

**의존성 예시** (build.gradle):
```gradle
dependencies {
    // Spring Boot & Cloud
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
    implementation 'org.springframework.cloud:spring-cloud-starter-openfeign'

    // Database
    implementation 'org.postgresql:postgresql'

    // HTTP Client
    implementation 'org.springframework.boot:spring-boot-starter-webflux'

    // Slack (선택)
    implementation 'com.slack.api:slack-api-client:1.x.x'

    // JSON Processing
    implementation 'com.fasterxml.jackson.core:jackson-databind'
}
```

---

## 9. 구현 고려사항

### 9.1 에러 처리 및 재시도

**Slack API 호출 실패 시**:
- 재시도 로직: 최대 3회, Exponential Backoff (1초 → 2초 → 4초)
- 실패 시 `status = 'FAILED'`, `error_message` 저장
- 관리자에게 실패 알림 (별도 로깅 또는 모니터링)

**Gemini AI 호출 실패 시**:
- 기본 발송 시한 계산 로직으로 폴백
- 예: 총 예상 시간의 150% 역산
- 에러 로그 기록

**Resilience4j 적용** (선택):
```yaml
resilience4j:
  retry:
    instances:
      slackApi:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2
      geminiApi:
        max-attempts: 2
        wait-duration: 2s
```

### 9.2 API 호출 로그 자동 기록

**모든 외부 API 호출 시 자동 로깅**:

```java
@Service
public class ApiLogDomainService {

    private final ExternalApiLogRepository apiLogRepository;

    public <T> T executeWithLogging(
        ApiProvider provider,
        String method,
        Supplier<T> apiCall,
        Object request
    ) {
        ExternalApiLog log = new ExternalApiLog();
        log.setApiProvider(provider);
        log.setApiMethod(method);
        log.setRequestData(maskSensitiveData(request)); // API Key 마스킹

        long startTime = System.currentTimeMillis();

        try {
            T response = apiCall.get();
            long duration = System.currentTimeMillis() - startTime;

            log.setSuccess(true);
            log.setResponseData(maskSensitiveData(response));
            log.setDurationMs((int) duration);
            log.setHttpStatus(200);

            apiLogRepository.save(log);
            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            log.setSuccess(false);
            log.setErrorMessage(e.getMessage());
            log.setDurationMs((int) duration);

            apiLogRepository.save(log);
            throw e;
        }
    }

    private Object maskSensitiveData(Object data) {
        // API Key, Token 등 민감 정보 마스킹
        // 예: "xoxb-123456789" -> "xoxb-****"
        return data;
    }
}
```

**사용 예시**:
```java
// Slack API 호출 시
SlackResponse response = apiLogService.executeWithLogging(
    ApiProvider.SLACK,
    "chat.postMessage",
    () -> slackApiClient.sendMessage(message),
    message
);

// Gemini AI 호출 시
GeminiResponse response = apiLogService.executeWithLogging(
    ApiProvider.GEMINI,
    "generateContent",
    () -> geminiApiClient.generate(prompt),
    prompt
);
```

**로그 조회 API** (MASTER 전용):
```java
@GetMapping("/api/v1/notifications/api-logs")
@PreAuthorize("hasRole('MASTER')")
public Page<ApiLogResponse> getApiLogs(
    @RequestParam(required = false) ApiProvider provider,
    @RequestParam(required = false) Boolean isSuccess,
    @RequestParam(required = false) LocalDate startDate,
    @RequestParam(required = false) LocalDate endDate,
    Pageable pageable
) {
    // API 로그 조회 및 통계
}
```

**통계 API 예시**:
```java
@GetMapping("/api/v1/notifications/api-logs/stats")
@PreAuthorize("hasRole('MASTER')")
public ApiStatistics getApiStatistics(
    @RequestParam ApiProvider provider,
    @RequestParam LocalDate date
) {
    // 일일 호출 횟수, 성공률, 평균 응답 시간, 총 비용 등
}
```

### 9.3 비동기 처리

**메시지 발송 비동기화**:
- `@Async` 어노테이션 사용
- 발송 대기 상태로 먼저 DB 저장 후 비동기 발송
- 사용자 응답 시간 단축

```java
@Async
public CompletableFuture<SlackMessageResponse> sendSlackMessageAsync(SlackMessageRequest request) {
    // Slack API 호출 (로그 자동 기록)
    // 결과를 DB에 업데이트
}
```

### 9.4 스케줄러 설정 관리

**application.yml**:
```yaml
scheduler:
  daily-route-notification:
    cron: "0 0 6 * * ?"  # 매일 06:00
    enabled: true
    timezone: Asia/Seoul
```

**동적 설정 변경**:
- Spring Cloud Config 또는 환경 변수 사용
- 테스트 시 cron 표현식 변경 가능

### 9.5 메시지 템플릿 관리

**템플릿화**:
- 메시지 포맷을 별도 설정 파일 또는 DB에 저장
- Thymeleaf 또는 MessageFormat 사용
- 다국어 지원 가능성 고려

**예시**:
```java
public class SlackMessageTemplate {
    public static String ORDER_NOTIFICATION = """
        주문 번호: {0}
        주문자 정보: {1} / {2}
        주문 시간: {3}
        상품 정보: {4}
        요청 사항: {5}
        발송지: {6}
        경유지: {7}
        도착지: {8}
        배송담당자: {9} / {10}

        📦 AI 분석 결과
        위 내용을 기반으로 도출된 최종 발송 시한은 {11}입니다.
        """;
}
```

### 9.6 보안

**API Key 관리**:
- 환경 변수 또는 Spring Cloud Config 사용
- `.env` 파일을 `.gitignore`에 추가
- Docker Secrets 또는 Kubernetes Secrets 활용

**민감 정보 로깅 방지**:
- Slack Token, API Key는 로그 및 DB에 저장 시 마스킹
- `p_external_api_logs` 테이블의 `request_data`, `response_data`에 민감 정보 제거
- 마스킹 처리 예시: `"token": "xoxb-****"`

**API 로그 접근 제어**:
- API 로그 조회는 MASTER 권한만 허용
- 민감한 요청/응답 데이터는 암호화 저장 고려

### 9.7 테스트

**단위 테스트**:
- Slack API, Gemini AI 호출을 MockWebServer로 모킹
- 메시지 생성 로직 테스트
- AI 응답 파싱 로직 테스트
- API 로그 자동 기록 로직 테스트

**통합 테스트**:
- TestContainers로 PostgreSQL 컨테이너 실행
- 실제 Slack/Gemini API 대신 WireMock 사용
- 스케줄러 동작 확인 (AwaitilityTest)
- API 호출 시 로그가 정상적으로 DB에 저장되는지 확인

**API 로그 테스트 예시**:
```java
@Test
void whenSlackApiCalled_thenLogShouldBeSaved() {
    // given
    SlackMessageRequest request = createTestRequest();

    // when
    slackService.sendMessage(request);

    // then
    List<ExternalApiLog> logs = apiLogRepository.findByApiProvider(ApiProvider.SLACK);
    assertThat(logs).hasSize(1);
    assertThat(logs.get(0).getApiMethod()).isEqualTo("chat.postMessage");
    assertThat(logs.get(0).isSuccess()).isTrue();
    assertThat(logs.get(0).getDurationMs()).isGreaterThan(0);
}
```

---

## 10. 도전 과제 구현 가이드

### 10.1 TSP (Traveling Salesman Problem) 해결

**Gemini AI 프롬프트 최적화**:
```
당신은 배송 경로 최적화 전문가입니다.

[제약 조건]
- 출발지: {hub_lat}, {hub_lng}
- 배송지 목록 (총 {count}곳):
  1. {company1_name}: ({lat1}, {lng1})
  2. {company2_name}: ({lat2}, {lng2})
  ...
- 근무 시간: 09:00 - 18:00
- 각 배송지 체류 시간: 20분
- 점심 시간: 12:00 - 13:00 (배송 불가)

[목표]
1. 총 이동 거리 최소화
2. 근무 시간 내 모든 배송 완료
3. 효율적인 경로 선택

최적 방문 순서를 번호로만 반환 (예: 3,1,4,2,5)
```

**대안 알고리즘** (AI 실패 시):
- Nearest Neighbor (가장 가까운 다음 지점)
- 2-opt 알고리즘
- Google OR-Tools 라이브러리 사용

### 10.2 Naver Maps API 활용

**Waypoints 파라미터 구성**:
```java
String waypoints = optimizedOrder.stream()
    .map(order -> companies.get(order).getLongitude() + "," + companies.get(order).getLatitude())
    .collect(Collectors.joining("|"));

String url = String.format(
    "https://naveropenapi.apigw.ntruss.com/map-direction/v1/driving?" +
    "start=%s,%s&goal=%s,%s&waypoints=%s&option=traoptimal",
    startLng, startLat, goalLng, goalLat, waypoints
);
```

**응답 파싱**:
```json
{
  "route": {
    "traoptimal": [
      {
        "summary": {
          "duration": 5400000,  // ms
          "distance": 42500     // m
        },
        "path": [[lng, lat], ...],
        "section": [...]
      }
    ]
  }
}
```

### 10.3 일일 알림 메시지 생성

**Gemini AI 프롬프트**:
```
다음 배송 정보를 기반으로 업체 배송 담당자에게 보낼 알림 메시지를 작성해주세요.

[배송 정보]
배송 담당자: {name}
출발 허브: {hub_name}
배송 일자: {date}
총 배송 건수: {count}건

[최적 경로]
{route_details}

총 예상 소요 시간: {total_duration}
총 이동 거리: {total_distance}

친절하고 명확한 메시지로 작성해주세요.
```

---

## 11. 패키지 구조 (DDD)

```
com.sparta.notification
├── application/
│   ├── service/
│   │   ├── NotificationService.java          # 메시지 발송 orchestration
│   │   ├── OrderNotificationService.java     # 주문 알림 생성
│   │   ├── DailyRouteNotificationService.java # 일일 경로 알림
│   │   └── ApiLogService.java                # API 호출 이력 관리
│   └── dto/
│       ├── OrderNotificationRequest.java
│       ├── NotificationResponse.java
│       ├── DailyRouteRequest.java
│       └── ApiLogResponse.java
├── domain/
│   ├── model/
│   │   ├── Notification.java                 # 메시지 엔티티
│   │   ├── ExternalApiLog.java               # API 로그 엔티티
│   │   ├── SenderType.java                   # ENUM (USER, SYSTEM)
│   │   ├── MessageType.java                  # ENUM (ORDER_NOTIFICATION, DAILY_ROUTE, MANUAL)
│   │   ├── MessageStatus.java                # ENUM (PENDING, SENT, FAILED)
│   │   └── ApiProvider.java                  # ENUM (SLACK, GEMINI, NAVER_MAPS)
│   ├── repository/
│   │   ├── NotificationRepository.java       # 인터페이스
│   │   └── ExternalApiLogRepository.java     # 인터페이스
│   └── service/
│       ├── GeminiAIService.java              # AI 호출 도메인 로직
│       ├── SlackApiService.java              # Slack API 호출 로직
│       └── ApiLogDomainService.java          # API 로그 생성 로직
├── infrastructure/
│   ├── repository/
│   │   ├── JpaNotificationRepository.java
│   │   ├── NotificationRepositoryImpl.java
│   │   ├── JpaExternalApiLogRepository.java
│   │   └── ExternalApiLogRepositoryImpl.java
│   ├── client/
│   │   ├── DeliveryServiceClient.java        # FeignClient
│   │   ├── OrderServiceClient.java
│   │   ├── AuthServiceClient.java            # 발신자 정보 조회용
│   │   ├── GeminiApiClient.java              # Gemini AI 호출
│   │   ├── SlackApiClient.java               # Slack API 호출
│   │   └── NaverMapsApiClient.java           # Naver Maps 호출
│   ├── configuration/
│   │   ├── FeignConfig.java
│   │   ├── AsyncConfig.java
│   │   └── SchedulerConfig.java
│   └── scheduler/
│       └── DailyRouteScheduler.java          # @Scheduled
└── presentation/
    ├── controller/
    │   ├── NotificationController.java
    │   └── ApiLogController.java             # API 로그 조회 (MASTER 전용)
    └── request/
        └── ManualMessageRequest.java
```

---

## 12. 개발 우선순위

### Phase 1: 필수 기능 (1주차)
1. ✅ 엔티티 구현 (SlackMessage, ExternalApiLog)
2. ✅ Repository 구현 (SlackMessageRepository, ExternalApiLogRepository)
3. ✅ Slack API 연동 (수동 메시지 발송)
4. ✅ API 호출 로그 자동 기록 구현
5. ✅ 메시지 이력 CRUD 구현
6. ✅ Swagger API 문서화

### Phase 2: AI 연동 (2주차)
1. ✅ Gemini AI 클라이언트 구현
2. ✅ Gemini AI 호출 시 로그 자동 기록
3. ✅ 주문 알림 API 구현
4. ✅ AI 기반 발송 시한 계산 로직
5. ✅ order-service 연동 테스트
6. ✅ API 로그 조회 API 구현 (MASTER 전용)

### Phase 3: 도전 과제 (3주차)
1. ⏳ 일일 경로 스케줄러 구현
2. ⏳ Gemini AI TSP 해결
3. ⏳ Naver Maps API 연동 (로그 자동 기록)
4. ⏳ p_company_delivery_routes 테이블 구현
5. ⏳ API 사용량 통계 대시보드 (선택)

### Phase 4: 최적화 (4주차)
1. ⏳ 비동기 처리 적용
2. ⏳ 재시도 로직 및 에러 처리
3. ⏳ 테스트 코드 작성
4. ⏳ API 로그 아카이빙 정책 구현 (30일)
5. ⏳ 성능 모니터링 및 개선 (duration_ms 분석)

---

## 13. 참고 자료

### 13.1 API 문서
- [Slack API - chat.postMessage](https://api.slack.com/methods/chat.postMessage)
- [Google Gemini API](https://ai.google.dev/docs)
- [Naver Maps Directions 5](https://api.ncloud-docs.com/docs/ai-naver-mapsdirections-driving)

### 13.2 라이브러리
- [Slack Java SDK](https://github.com/slackapi/java-slack-sdk)
- [Spring Cloud OpenFeign](https://spring.io/projects/spring-cloud-openfeign)
- [Resilience4j](https://resilience4j.readme.io/)

### 13.3 프로젝트 내부 문서
- `table-specifications.md`: 전체 테이블 명세
- `planning.md`: 프로젝트 전체 계획
- `CLAUDE.md`: 프로젝트 가이드

---

## 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2025-11-03 | Team | 초안 작성 (slack-service) |
| 1.1 | 2025-11-03 | Team | `p_external_api_logs` 테이블 추가, API 호출 모니터링 기능 추가 |
| 1.2 | 2025-11-03 | Team | `p_notifications` 테이블에 발신자 정보 추가 (sender_username, sender_name, sender_type) |
| 1.3 | 2025-11-03 | Team | 발신자 Slack ID 추가 (sender_slack_id), 스냅샷 저장 정책 명시 |
| 2.0 | 2025-11-03 | Team | **서비스명 변경: slack-service → notification-service** (비즈니스 도메인 중심 설계) |

---

## 요약

**notification-service의 주요 구성 요소**:

1. **데이터베이스 테이블** (2개):
   - `p_notifications`: 알림 메시지 발송 이력 (발신자/수신자 정보 포함)
   - `p_external_api_logs`: 외부 API 호출 이력 (Slack, Gemini AI, Naver Maps)

2. **핵심 기능**:
   - 주문 생성 시 Gemini AI 기반 발송 시한 계산 및 Slack 알림 (시스템 자동 발송)
   - 사용자 수동 메시지 발송 (발신자 정보 자동 기록)
   - 모든 외부 API 호출 자동 로깅 (성공/실패, 응답 시간, 비용 추적)
   - API 사용량 통계 조회 (MASTER 전용)
   - (도전 과제) 일일 경로 최적화 및 자동 알림

3. **외부 API 연동**:
   - Slack API: 메시지 발송
   - Gemini AI: 발송 시한 계산, 경로 최적화
   - Naver Maps API: 경로 및 시간 계산 (도전 과제)

4. **보안 및 모니터링**:
   - 민감 정보 마스킹 (API Key, Token)
   - API 호출 성공률, 응답 시간, 비용 추적
   - MASTER 권한으로만 API 로그 조회 가능

5. **스냅샷 저장 패턴**:
   - 발신자 정보(username, slack_id, name)를 메시지 발송 시점에 스냅샷으로 저장
   - 감사 로그 특성: 시점 정보 영구 보존, 사용자 삭제/변경 시에도 이력 유지
   - 성능: auth-service 조회 없이 빠른 메시지 이력 조회

---

## 주요 설계 결정 사항

### ✅ 왜 발신자 Slack ID를 추가했나?
1. **Slack 기능 확장**: 답장, 멘션, 스레드 기능 구현 가능
2. **완전한 발신자 정보**: username + slack_id + name 세트로 보존
3. **Slack ID 변경 대응**: 사용자가 Slack ID 변경해도 과거 메시지는 원본 유지

### ✅ 왜 중복 저장(스냅샷)하나?
1. **감사 로그**: 메시지 발송 시점의 정확한 정보 기록 필요
2. **독립성**: auth-service 장애/삭제 시에도 메시지 이력 조회 가능
3. **성능**: 매번 auth-service FeignClient 호출 불필요

### ✅ SYSTEM vs USER 구분
- **SYSTEM**: 주문 알림, 일일 경로 알림 등 자동 발송 (발신자 정보 NULL)
- **USER**: 사용자 수동 발송 (발신자 정보 필수, 스냅샷 저장)

**문서 작성 완료**. 구현 시 추가 질문이나 설계 변경이 필요하면 언제든 요청해주세요.
