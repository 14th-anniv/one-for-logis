# Notification Service API 명세서

**서비스명**: notification-service  
**포트**: 8700  
**Base URL**: `/api/v1/notifications`  
**작성일**: 2025-11-13
**작성자**: notification-service 담당자

---

## 목차
1. [공통 사항](#공통-사항)
2. [API 목록](#api-목록)
3. [상세 명세](#상세-명세)
4. [ErrorCode](#errorcode)
5. [Enum 타입](#enum-타입)

---

## 공통 사항

### 인증 방식
- **Gateway JWT 인증**: Gateway에서 JWT 토큰 검증 후 헤더 전달
- **Header 형식**:
  ```
  X-User-Id: 550e8400-e29b-41d4-a716-446655440000
  X-User-Name: user1
  X-User-Role: MASTER
  ```
- **참고**: 클라이언트는 `Authorization: Bearer {JWT}` 형식으로 요청하며, Gateway가 이를 X-User-* 헤더로 변환하여 서비스에 전달합니다.

### 응답 형식
모든 API는 `ApiResponse<T>` 형식으로 응답합니다.

**성공 응답**:
```json
{
  "status": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": { ... }
}
```

**에러 응답**:
```json
{
  "status": "ERROR",
  "message": "에러 메시지",
  "data": null
}
```

### 페이징 파라미터 (공통)
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| page | int | X | 0 | 페이지 번호 (0부터 시작) |
| size | int | X | 10 | 페이지 크기 (10, 30, 50) |
| sortBy | String | X | createdAt | 정렬 기준 필드 |
| isAsc | boolean | X | false | true: 오름차순, false: 내림차순 |

### 페이징 응답 형식
```json
{
  "status": "SUCCESS",
  "message": "조회 성공",
  "data": {
    "content": [ ... ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "sort": { ... }
    },
    "totalElements": 100,
    "totalPages": 10,
    "last": false,
    "first": true,
    "numberOfElements": 10
  }
}
```

---

## API 목록

| No | Method | 기능 | 권한 | URL |
|----|--------|------|------|-----|
| 1 | POST | 주문 알림 발송 | INTERNAL_SERVICE_ONLY (Gateway 인증) | `/api/v1/notifications/order` |
| 2 | POST | 배송 상태 알림 발송 | MASTER, HUB_MANAGER, DELIVERY_MANAGER, COMPANY_MANAGER | `/api/v1/notifications/delivery-status` |
| 3 | POST | 수동 메시지 발송 | ALL | `/api/v1/notifications/manual` |
| 4 | GET | 알림 단일 조회 | ALL | `/api/v1/notifications/{notificationId}` |
| 5 | GET | 알림 목록 조회 (페이징) | MASTER | `/api/v1/notifications` |
| 6 | GET | 알림 필터링 조회 (페이징) | MASTER | `/api/v1/notifications/search` |
| 7 | GET | 외부 API 로그 전체 조회 | MASTER | `/api/v1/notifications/api-logs` |
| 8 | GET | 외부 API 로그 제공자별 조회 | MASTER | `/api/v1/notifications/api-logs/provider/{provider}` |
| 9 | GET | 외부 API 로그 메시지 ID로 조회 | MASTER | `/api/v1/notifications/api-logs/message/{messageId}` |
| 10 | GET | API 통계 조회 | MASTER | `/api/v1/notifications/api-logs/stats` |

---

## 상세 명세

### 1. 주문 알림 발송 (내부 API)

**목적**: order-service에서 주문 생성 시 호출. Gemini AI로 최종 발송 시한을 계산하고 Slack 메시지를 발송합니다.

#### 기본 정보
- **Method**: `POST`
- **URL**: `/api/v1/notifications/order`
- **권한**: `INTERNAL_SERVICE_ONLY` (내부 서비스 간 통신만, Gateway 통과 필요)
- **응답 코드**: `201 CREATED`

#### Headers
```
X-User-Id: (Gateway가 추가)
X-User-Name: (Gateway가 추가)
X-User-Role: (Gateway가 추가)
```

**참고**: 
- Gateway에서 JWT 검증 완료 후 X-* 헤더를 추가하여 notification-service로 전달
- notification-service는 `HeaderAuthFilter`로 X-* 헤더만 읽음
- 실제 구현에서는 `@PreAuthorize` 없지만 Gateway 레벨에서 인증 처리

#### Request Body
```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "ordererInfo": "김말숙 / msk@seafood.world",
  "requestingCompanyName": "건조 식품 가공 업체",
  "receivingCompanyName": "수산물 도매 업체",
  "productInfo": "마른 오징어 50박스",
  "requestDetails": "12월 12일 3시까지는 보내주세요!",
  "departureHub": "경기 북부 센터",
  "waypoints": ["대전광역시 센터", "부산광역시 센터"],
  "destinationHub": "부산광역시 센터",
  "destinationAddress": "부산시 사하구 낙동대로 1번길 1 해산물월드",
  "deliveryPersonInfo": "고길동 / kdk@sparta.world",
  "recipientSlackId": "U01234ABCDE",
  "recipientName": "김관리"
}
```

**필드 설명**:
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| orderId | UUID | O | 주문 ID |
| ordererInfo | String | O | 주문자 정보 (이름 / 이메일) |
| requestingCompanyName | String | O | 공급업체명 |
| receivingCompanyName | String | O | 수령업체명 |
| productInfo | String | O | 상품 정보 (상품명 + 수량) |
| requestDetails | String | X | 요청 사항 (납품 기한 등) |
| departureHub | String | O | 출발 허브명 |
| waypoints | List\<String\> | X | 경유 허브 목록 |
| destinationHub | String | O | 도착 허브명 |
| destinationAddress | String | O | 최종 배송지 주소 |
| deliveryPersonInfo | String | O | 배송 담당자 정보 (이름 / 슬랙ID) |
| recipientSlackId | String | O | 발송 허브 관리자 Slack ID |
| recipientName | String | O | 발송 허브 관리자 이름 |

#### Response Body (성공)
```json
{
  "status": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "id": "750e8400-e29b-41d4-a716-446655440000",
    "senderType": "SYSTEM",
    "senderUsername": null,
    "senderSlackId": null,
    "senderName": null,
    "recipientSlackId": "U01234ABCDE",
    "recipientName": "김관리",
    "messageContent": "🚚 *신규 주문 배송 알림*\n\n📦 주문 정보:\n- 주문자: 김말숙 / msk@seafood.world\n- 공급업체: 건조 식품 가공 업체\n- 수령업체: 수산물 도매 업체\n- 상품: 마른 오징어 50박스\n\n🛣️ 경로 정보:\n- 출발: 경기 북부 센터\n- 경유: 대전광역시 센터 → 부산광역시 센터\n- 도착: 부산광역시 센터\n- 최종 배송지: 부산시 사하구 낙동대로 1번길 1 해산물월드\n\n👤 배송 담당: 고길동 / kdk@sparta.world\n\n⏰ *최종 발송 시한: 2025-12-11 14:30*\n\n💬 요청 사항: 12월 12일 3시까지는 보내주세요!",
    "messageType": "ORDER_NOTIFICATION",
    "referenceId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "SENT",
    "sentAt": "2025-11-07T10:30:00",
    "errorMessage": null,
    "createdBy": "system",
    "createdAt": "2025-11-07T10:25:00",
    "updatedBy": "system",
    "updatedAt": "2025-11-07T10:30:00"
  }
}
```

#### 에러 응답
| HTTP 상태 | ErrorCode | 메시지 |
|-----------|-----------|--------|
| 500 | NOTIFICATION_SEND_FAILED | 알림 발송에 실패했습니다. |

---

### 2. 배송 상태 알림 발송

**목적**: 배송 상태 변경 시 Slack 알림을 발송합니다. Kafka Event 기반 알림(Issue #35)과 별도로 REST API를 제공하여 재발송 및 테스트 용이성을 확보합니다.

#### 기본 정보
- **Method**: `POST`
- **URL**: `/api/v1/notifications/delivery-status`
- **권한**: `MASTER`, `HUB_MANAGER`, `DELIVERY_MANAGER`, `COMPANY_MANAGER`
- **응답 코드**: `201 CREATED`

#### Headers
```
Content-Type: application/json
X-User-Id: 1
X-User-Role: DELIVERY_MANAGER
```

#### Request Body
```json
{
  "deliveryId": "550e8400-e29b-41d4-a716-446655440001",
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "previousStatus": "HUB_WAITING",
  "currentStatus": "HUB_MOVING",
  "recipientSlackId": "C09QY22AMEE",
  "recipientName": "배송담당자"
}
```

**필드 설명**:
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| deliveryId | UUID | O | 배송 ID |
| orderId | UUID | O | 주문 ID |
| previousStatus | String | O | 이전 배송 상태 |
| currentStatus | String | O | 현재 배송 상태 |
| recipientSlackId | String | O | 수신자 Slack ID (채널 또는 사용자) |
| recipientName | String | O | 수신자 이름 |

#### Response Body (성공)
```json
{
  "status": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "id": "7a8b9c0d-1e2f-3g4h-5i6j-7k8l9m0n1o2p",
    "senderType": "SYSTEM",
    "senderUsername": null,
    "senderSlackId": null,
    "senderName": null,
    "recipientSlackId": "C09QY22AMEE",
    "recipientName": "배송담당자",
    "messageContent": "🚚 *배송 상태 업데이트*\n\n배송 ID: `550e8400-e29b-41d4-a716-446655440001`\n주문 ID: `550e8400-e29b-41d4-a716-446655440000`\n이전 상태: `HUB_WAITING`\n현재 상태: `HUB_MOVING`\n\n수령인: 배송담당자\n",
    "messageType": "DELIVERY_STATUS_UPDATE",
    "referenceId": "550e8400-e29b-41d4-a716-446655440001",
    "status": "SENT",
    "sentAt": "2025-11-13T14:30:00",
    "errorMessage": null,
    "createdBy": "system",
    "createdAt": "2025-11-13T14:30:00",
    "updatedBy": "system",
    "updatedAt": "2025-11-13T14:30:00"
  }
}
```

#### 에러 응답
| HTTP 상태 | ErrorCode | 메시지 |
|-----------|-----------|--------|
| 400 | INVALID_INPUT | 필수 필드가 누락되었습니다. |
| 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 403 | FORBIDDEN_ACCESS | 접근 권한이 없습니다. |
| 500 | NOTIFICATION_SEND_FAILED | 알림 발송에 실패했습니다. |

#### 비고
- **Kafka vs REST 차이점**:
  - Kafka Event: eventId 필수 (멱등성 보장, 중복 방지)
  - REST API: eventId = null (중복 허용, 재발송 가능)
- **사용 시나리오**:
  - Slack 발송 실패 시 수동 재전송
  - 테스트 및 디버깅
  - Kafka 장애 시 대체 수단
- **메시지 형식**: DeliveryStatusChangedConsumer(Kafka)와 동일

---

### 3. 수동 메시지 발송

**목적**: 인증된 사용자가 직접 Slack 메시지를 발송합니다. 발신자 정보는 스냅샷으로 저장됩니다.

#### 기본 정보
- **Method**: `POST`
- **URL**: `/api/v1/notifications/manual`
- **권한**: `ALL` (MASTER, HUB_MANAGER, DELIVERY_MANAGER, COMPANY_MANAGER)
- **응답 코드**: `201 CREATED`

#### Headers
```
Content-Type: application/json
X-User-Id: 550e8400-e29b-41d4-a716-446655440000
X-User-Name: user1
X-User-Role: MASTER
```

#### Request Body
```json
{
  "recipientSlackId": "U01234ABCDE",
  "recipientName": "김담당",
  "messageContent": "긴급 배송 건이 추가되었습니다. 확인 부탁드립니다."
}
```

**필드 설명**:
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| recipientSlackId | String | O | 수신자 Slack ID |
| recipientName | String | O | 수신자 이름 |
| messageContent | String | O | 메시지 내용 |

#### Response Body (성공)
```json
{
  "status": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "id": "850e8400-e29b-41d4-a716-446655440000",
    "senderType": "USER",
    "senderUsername": "user1",
    "senderSlackId": "U98765ZYXWV",
    "senderName": "김발신",
    "recipientSlackId": "U01234ABCDE",
    "recipientName": "김담당",
    "messageContent": "긴급 배송 건이 추가되었습니다. 확인 부탁드립니다.",
    "messageType": "MANUAL",
    "referenceId": null,
    "status": "SENT",
    "sentAt": "2025-11-07T11:00:00",
    "errorMessage": null,
    "createdBy": "user1",
    "createdAt": "2025-11-07T11:00:00",
    "updatedBy": "user1",
    "updatedAt": "2025-11-07T11:00:00"
  }
}
```

#### 에러 응답
| HTTP 상태 | ErrorCode | 메시지 |
|-----------|-----------|--------|
| 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 500 | NOTIFICATION_SEND_FAILED | 알림 발송에 실패했습니다. |

---

### 4. 알림 단일 조회

**목적**: 알림 ID로 특정 알림 정보를 조회합니다.

#### 기본 정보
- **Method**: `GET`
- **URL**: `/api/v1/notifications/{notificationId}`
- **권한**: `ALL` (MASTER, HUB_MANAGER, DELIVERY_MANAGER, COMPANY_MANAGER)
- **응답 코드**: `200 OK`

#### Headers
```
X-User-Id: 550e8400-e29b-41d4-a716-446655440000
X-User-Name: user1
X-User-Role: MASTER
```

#### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| notificationId | UUID | O | 알림 ID |

#### Request Example
```
GET /api/v1/notifications/750e8400-e29b-41d4-a716-446655440000
```

#### Response Body (성공)
```json
{
  "status": "SUCCESS",
  "message": "조회 성공",
  "data": {
    "id": "750e8400-e29b-41d4-a716-446655440000",
    "senderType": "SYSTEM",
    "senderUsername": null,
    "senderSlackId": null,
    "senderName": null,
    "recipientSlackId": "U01234ABCDE",
    "recipientName": "김관리",
    "messageContent": "주문 알림 메시지...",
    "messageType": "ORDER_NOTIFICATION",
    "referenceId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "SENT",
    "sentAt": "2025-11-07T10:30:00",
    "errorMessage": null,
    "createdBy": "system",
    "createdAt": "2025-11-07T10:25:00",
    "updatedBy": "system",
    "updatedAt": "2025-11-07T10:30:00"
  }
}
```

#### 에러 응답
| HTTP 상태 | ErrorCode | 메시지 |
|-----------|-----------|--------|
| 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 404 | NOTIFICATION_NOT_FOUND | 알림을 찾을 수 없습니다. |

---

### 5. 알림 목록 조회 (페이징)

**목적**: 알림 목록을 페이징 형태로 조회합니다. 최신순(createdAt DESC) 정렬.

#### 기본 정보
- **Method**: `GET`
- **URL**: `/api/v1/notifications`
- **권한**: `MASTER`
- **응답 코드**: `200 OK`

#### Headers
```
X-User-Id: 550e8400-e29b-41d4-a716-446655440000
X-User-Name: admin
X-User-Role: MASTER
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| page | int | X | 0 | 페이지 번호 |
| size | int | X | 10 | 페이지 크기 |
| sortBy | String | X | createdAt | 정렬 기준 (createdAt, sentAt, status) |
| isAsc | boolean | X | false | 정렬 방향 |

#### Request Example
```
GET /api/v1/notifications?page=0&size=10&sortBy=createdAt&isAsc=false
```

#### Response Body (성공)
```json
{
  "status": "SUCCESS",
  "message": "조회 성공",
  "data": {
    "content": [
      {
        "id": "750e8400-e29b-41d4-a716-446655440000",
        "senderType": "SYSTEM",
        "senderUsername": null,
        "recipientSlackId": "U01234ABCDE",
        "messageType": "ORDER_NOTIFICATION",
        "status": "SENT",
        "createdAt": "2025-11-07T10:25:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10
    },
    "totalElements": 100,
    "totalPages": 10,
    "last": false,
    "first": true
  }
}
```

#### 에러 응답
| HTTP 상태 | ErrorCode | 메시지 |
|-----------|-----------|--------|
| 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 403 | FORBIDDEN_ACCESS | 접근 권한이 없습니다. |

---

### 6. 알림 필터링 조회 (페이징)

**목적**: 알림을 필터 조건에 따라 페이징하여 조회합니다. 발신자, 수신자, 메시지 타입, 상태별 필터링 지원.

#### 기본 정보
- **Method**: `GET`
- **URL**: `/api/v1/notifications/search`
- **권한**: `MASTER`
- **응답 코드**: `200 OK`

#### Headers
```
X-User-Id: 550e8400-e29b-41d4-a716-446655440000
X-User-Name: admin
X-User-Role: MASTER
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| senderUsername | String | X | - | 발신자 사용자명 (부분 일치) |
| recipientSlackId | String | X | - | 수신자 Slack ID (완전 일치) |
| messageType | MessageType | X | - | 메시지 타입 (ORDER_NOTIFICATION, DELIVERY_STATUS_UPDATE, MANUAL, DAILY_ROUTE) |
| status | MessageStatus | X | - | 발송 상태 (PENDING, SENT, FAILED) |
| page | int | X | 0 | 페이지 번호 |
| size | int | X | 10 | 페이지 크기 |
| sortBy | String | X | createdAt | 정렬 기준 |
| isAsc | boolean | X | false | 정렬 방향 |

#### Request Example
```
GET /api/v1/notifications/search?messageType=ORDER_NOTIFICATION&status=SENT&page=0&size=10
```

#### Response Body (성공)
페이징 응답 형식과 동일 (5번 API 참조)

#### 에러 응답
| HTTP 상태 | ErrorCode | 메시지 |
|-----------|-----------|--------|
| 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 403 | FORBIDDEN_ACCESS | 접근 권한이 없습니다. |

---

### 7. 외부 API 로그 전체 조회 (페이징)

**목적**: 외부 API 호출 로그 전체를 페이징하여 조회합니다. (Slack, Gemini, Naver Maps API 호출 이력)

#### 기본 정보
- **Method**: `GET`
- **URL**: `/api/v1/notifications/api-logs`
- **권한**: `MASTER`
- **응답 코드**: `200 OK`

#### Headers
```
X-User-Id: 550e8400-e29b-41d4-a716-446655440000
X-User-Name: admin
X-User-Role: MASTER
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| page | int | X | 0 | 페이지 번호 |
| size | int | X | 10 | 페이지 크기 |
| sortBy | String | X | calledAt | 정렬 기준 (calledAt, durationMs, cost) |
| isAsc | boolean | X | false | 정렬 방향 |

#### Request Example
```
GET /api/v1/notifications/api-logs?page=0&size=10&sortBy=calledAt&isAsc=false
```

#### Response Body (성공)
```json
{
  "status": "SUCCESS",
  "message": "조회 성공",
  "data": {
    "content": [
      {
        "id": "950e8400-e29b-41d4-a716-446655440000",
        "apiProvider": "SLACK",
        "apiMethod": "chat.postMessage",
        "requestData": {
          "channel": "U01234ABCDE",
          "text": "메시지 내용"
        },
        "responseData": {
          "ok": true,
          "ts": "1636363636.123456"
        },
        "httpStatus": 200,
        "isSuccess": true,
        "errorCode": null,
        "errorMessage": null,
        "durationMs": 1250,
        "cost": 0.0,
        "calledAt": "2025-11-07T10:30:15",
        "messageId": "750e8400-e29b-41d4-a716-446655440000"
      }
    ],
    "totalElements": 150,
    "totalPages": 15
  }
}
```

#### 에러 응답
| HTTP 상태 | ErrorCode | 메시지 |
|-----------|-----------|--------|
| 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 403 | FORBIDDEN_ACCESS | 접근 권한이 없습니다. |

---

### 8. 외부 API 로그 제공자별 조회 (페이징)

**목적**: 특정 API 제공자(SLACK, GEMINI, NAVER_MAPS)의 호출 로그를 페이징하여 조회합니다.

#### 기본 정보
- **Method**: `GET`
- **URL**: `/api/v1/notifications/api-logs/provider/{provider}`
- **권한**: `MASTER`
- **응답 코드**: `200 OK`

#### Headers
```
X-User-Id: 550e8400-e29b-41d4-a716-446655440000
X-User-Name: admin
X-User-Role: MASTER
```

#### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| provider | ApiProvider | O | API 제공자 (SLACK, GEMINI, NAVER_MAPS) |

#### Query Parameters
페이징 파라미터 동일 (7번 API 참조)

#### Request Example
```
GET /api/v1/notifications/api-logs/provider/GEMINI?page=0&size=10
```

#### Response Body (성공)
7번 API 응답 형식과 동일

---

### 9. 외부 API 로그 메시지 ID로 조회 (페이징)

**목적**: 특정 메시지와 연관된 외부 API 호출 로그를 페이징하여 조회합니다.

#### 기본 정보
- **Method**: `GET`
- **URL**: `/api/v1/notifications/api-logs/message/{messageId}`
- **권한**: `MASTER`
- **응답 코드**: `200 OK`

#### Headers
```
X-User-Id: 550e8400-e29b-41d4-a716-446655440000
X-User-Name: admin
X-User-Role: MASTER
```

#### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| messageId | UUID | O | 메시지 ID |

#### Query Parameters
페이징 파라미터 동일 (7번 API 참조)

#### Request Example
```
GET /api/v1/notifications/api-logs/message/750e8400-e29b-41d4-a716-446655440000?page=0&size=10
```

#### Response Body (성공)
7번 API 응답 형식과 동일

---

### 10. API 통계 조회

**목적**: 외부 API 호출 통계를 조회합니다. 제공자별 호출 수, 성공률, 평균 응답 시간, 총 비용 포함.

#### 기본 정보
- **Method**: `GET`
- **URL**: `/api/v1/notifications/api-logs/stats`
- **권한**: `MASTER`
- **응답 코드**: `200 OK`

#### Headers
```
X-User-Id: 550e8400-e29b-41d4-a716-446655440000
X-User-Name: admin
X-User-Role: MASTER
```

#### Request Example
```
GET /api/v1/notifications/api-logs/stats
```

#### Response Body (성공)
```json
{
  "status": "SUCCESS",
  "message": "조회 성공",
  "data": {
    "SLACK": {
      "apiProvider": "SLACK",
      "totalCalls": 150,
      "successCalls": 145,
      "failedCalls": 5,
      "successRate": 96.67,
      "avgResponseTime": 234.5,
      "minResponseTime": 120,
      "maxResponseTime": 1500,
      "totalCost": 0.0
    },
    "GEMINI": {
      "apiProvider": "GEMINI",
      "totalCalls": 80,
      "successCalls": 78,
      "failedCalls": 2,
      "successRate": 97.5,
      "avgResponseTime": 3456.78,
      "minResponseTime": 2100,
      "maxResponseTime": 8000,
      "totalCost": 0.0042
    },
    "NAVER_MAPS": {
      "apiProvider": "NAVER_MAPS",
      "totalCalls": 0,
      "successCalls": 0,
      "failedCalls": 0,
      "successRate": 0.0,
      "avgResponseTime": 0.0,
      "minResponseTime": 0,
      "maxResponseTime": 0,
      "totalCost": 0.0
    }
  }
}
```

**필드 설명**:
| 필드 | 타입 | 설명 |
|------|------|------|
| apiProvider | ApiProvider | API 제공자 |
| totalCalls | long | 총 호출 수 |
| successCalls | long | 성공한 호출 수 |
| failedCalls | long | 실패한 호출 수 |
| successRate | double | 성공률 (0-100, 소수점 2자리) |
| avgResponseTime | double | 평균 응답 시간 (ms, 소수점 2자리) |
| minResponseTime | long | 최소 응답 시간 (ms) |
| maxResponseTime | long | 최대 응답 시간 (ms) |
| totalCost | BigDecimal | 총 비용 (Gemini API만 해당) |

#### 에러 응답
| HTTP 상태 | ErrorCode | 메시지 |
|-----------|-----------|--------|
| 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 403 | FORBIDDEN_ACCESS | 접근 권한이 없습니다. |

---

## ErrorCode

notification-service에서 사용하는 에러 코드입니다.

| ErrorCode | HTTP 상태 | 메시지 |
|-----------|-----------|--------|
| NOTIFICATION_NOT_FOUND | 404 NOT_FOUND | 알림을 찾을 수 없습니다. |
| NOTIFICATION_SEND_FAILED | 500 INTERNAL_SERVER_ERROR | 알림 발송에 실패했습니다. |
| UNAUTHORIZED | 401 UNAUTHORIZED | 인증이 필요합니다. |
| FORBIDDEN_ACCESS | 403 FORBIDDEN | 접근 권한이 없습니다. |
| INVALID_INPUT | 400 BAD_REQUEST | 잘못된 요청입니다. |

---

## Enum 타입

### MessageType (메시지 타입)
| 값 | 설명 |
|----|------|
| ORDER_NOTIFICATION | 주문 알림 (order-service에서 호출) |
| DELIVERY_STATUS_UPDATE | 배송 상태 변경 알림 (Kafka 이벤트 처리) |
| MANUAL | 수동 메시지 (사용자가 직접 발송) |
| DAILY_ROUTE | 일일 경로 최적화 알림 (Challenge - 미구현) |

### MessageStatus (메시지 발송 상태)
| 값 | 설명 |
|----|------|
| PENDING | 발송 대기 중 |
| SENT | 발송 완료 |
| FAILED | 발송 실패 |

### SenderType (발신자 타입)
| 값 | 설명 |
|----|------|
| USER | 사용자가 발송 (수동 메시지) |
| SYSTEM | 시스템이 발송 (주문 알림 등) |

### ApiProvider (외부 API 제공자)
| 값 | 설명 |
|----|------|
| SLACK | Slack API (메시지 발송) |
| GEMINI | Google Gemini API (AI 계산) |
| NAVER_MAPS | Naver Maps API (경로 계산 - Challenge) |

---

## 비즈니스 로직

### 주문 알림 발송 프로세스
1. **order-service → notification-service**: `POST /api/v1/notifications/order` 호출
2. **Gemini API 호출**: 배송 경로 정보를 기반으로 최종 발송 시한 계산
3. **메시지 생성**: 주문 정보 + AI 계산 결과를 포함한 Slack 메시지 생성
4. **Slack API 호출**: 허브 관리자에게 메시지 발송
5. **로그 저장**:
   - `p_notifications` 테이블에 메시지 정보 저장
   - `p_external_api_logs` 테이블에 Gemini/Slack API 호출 로그 저장

### 배송 상태 변경 알림 프로세스

**방법 1: Kafka Event (Issue #35)**
1. **delivery-service → Kafka**: `delivery.status.changed` 토픽에 이벤트 발행
2. **notification-service Kafka Consumer**: 이벤트 수신 및 멱등성 검증 (event_id)
3. **메시지 생성**: 배송 상태 변경 정보를 포함한 Slack 메시지 생성
4. **Slack API 호출**: 허브 관리자에게 메시지 발송
5. **로그 저장**:
   - `p_notifications` 테이블에 메시지 정보 저장 (messageType: DELIVERY_STATUS_UPDATE, eventId 저장)
   - `p_external_api_logs` 테이블에 Slack API 호출 로그 저장

**방법 2: REST API (Issue #84)**
1. **클라이언트 → notification-service**: `POST /api/v1/notifications/delivery-status` 호출
2. **메시지 생성**: 배송 상태 변경 정보를 포함한 Slack 메시지 생성 (Kafka와 동일한 형식)
3. **Slack API 호출**: 허브 관리자에게 메시지 발송
4. **로그 저장**:
   - `p_notifications` 테이블에 메시지 정보 저장 (messageType: DELIVERY_STATUS_UPDATE, eventId = null)
   - `p_external_api_logs` 테이블에 Slack API 호출 로그 저장

**차이점**:
- **Kafka**: eventId 저장 (멱등성 보장, 중복 방지)
- **REST**: eventId = null (중복 허용, 재발송 가능)

**사용 시나리오**:
- **Kafka**: 정상적인 배송 상태 변경 시 (delivery-service에서 자동 발행)
- **REST**: Slack 발송 실패 시 재전송, 테스트/디버깅, Kafka 장애 시 대체 수단

### 발신자 정보 스냅샷 패턴
- **목적**: 사용자 정보 변경 또는 삭제 시에도 메시지 이력 보존
- **저장 항목**: senderUsername, senderSlackId, senderName
- **적용 시점**: 메시지 발송 시점의 user-service 정보 조회 후 저장
- **SYSTEM 메시지**: 발신자 필드 모두 null

### 외부 API 로그 자동 저장
- **AOP 적용**: `@ExternalApiLog` 어노테이션 사용
- **저장 항목**: 요청/응답 데이터(JSONB), 성공 여부, 응답 시간, 비용
- **용도**: API 성능 모니터링, 비용 추적, 장애 분석

---

## 참고 사항

### Slack 메시지 형식
- **Markdown 지원**: Slack Markdown 문법 사용 (`*굵게*`, `_기울임_`, `~취소선~`)
- **Emoji 지원**: `:emoji_name:` 형식 (예: `:truck:`, `:package:`, `:clock3:`)
- **멘션 불가**: Slack ID로 직접 DM 발송 (채널 멘션 아님)

### Gemini API 비용
- **모델**: gemini-1.5-flash
- **Input**: $0.00001875 / 1K characters
- **Output**: $0.000075 / 1K characters
- **평균 비용**: 주문 알림당 $0.00005 예상

### 페이징 제한
- **최대 페이지 크기**: 50
- **기본 페이지 크기**: 10
- **허용 크기**: 10, 30, 50

---

**문서 버전**: v1.1
**최종 수정일**: 2025-11-13
**담당자**: notification-service 개발팀

**변경 이력**:
- v1.1 (2025-11-13): Issue #84 - 배송 상태 알림 REST API 추가 (API #2)
- v1.0 (2025-11-11): 초기 작성 (9개 API)
