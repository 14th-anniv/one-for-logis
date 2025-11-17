📊 Docker Compose 파일 분석 결과

| 파일                      | notification-service 포함 | 방식               | 추천       |
  |-------------------------|-------------------------|------------------|----------|
| docker-compose.yml      | ❌                       | Volume mount     | 개발용 기본   |
| docker-compose-team.yml | ✅                       | Volume mount     | 추천 (개발용) |
| docker-compose-v12.yml  | ✅                       | Dockerfile build | 배포용      |

  ---
🚀 로컬 실행 및 Postman 테스트 가이드

Step 1: Docker 환경 실행

방법 1: docker-compose-team.yml 사용 (추천)

# 1. 모든 서비스 빌드
./gradlew clean build -x test

# 2. Docker 컨테이너 시작
docker-compose -f docker-compose-team.yml up -d

# 3. 로그 확인
docker-compose -f docker-compose-team.yml logs -f notification-service

# 4. 서비스 상태 확인
docker-compose -f docker-compose-team.yml ps

주요 서비스 포트

| 서비스                  | 포트   | URL                   |
  |----------------------|------|-----------------------|
| Eureka Server        | 8761 | http://localhost:8761 |
| Gateway              | 8000 | http://localhost:8000 |
| User Service         | 8100 | http://localhost:8100 |
| Hub Service          | 8200 | http://localhost:8200 |
| Order Service        | 8400 | http://localhost:8400 |
| Notification Service | 8700 | http://localhost:8700 |
| PostgreSQL           | 5432 | localhost:5432        |
| Redis                | 6379 | localhost:6379        |

  ---
Step 2: Postman 테스트 컬렉션

📁 Postman Collection 구조

Notification Service API Tests
├── 1. Health Check
├── 2. Order Notification (Internal API)
├── 3. Manual Notification (User API)
├── 4. Get Notification by ID
├── 5. Get Notifications (Pageable)
├── 6. Get All API Logs (MASTER only)
├── 7. Get API Logs by Provider
└── 8. Get API Logs by Message ID

  ---
🧪 테스트 케이스 상세

1️⃣ Health Check

목적: 서비스 정상 동작 확인

GET http://localhost:8700/actuator/health

Expected Response (200 OK):
{
"status": "UP"
}

  ---
2️⃣ 주문 알림 발송 (Internal API)

목적: order-service에서 호출하는 주문 알림 발송

POST http://localhost:8700/api/v1/notifications/order
Content-Type: application/json

{
"orderId": "{{$randomUUID}}",
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
"recipientName": "부산허브 관리자"
}

Expected Response (201 Created):
{
"isSuccess": true,
"code": 201,
"message": "CREATED",
"data": {
"id": "550e8400-e29b-41d4-a716-446655440000",
"senderType": "SYSTEM",
"senderUsername": null,
"senderSlackId": null,
"senderName": null,
"recipientSlackId": "U01234ABCDE",
"recipientName": "부산허브 관리자",
"messageContent": "📦 **새로운 주문 알림**...",
"messageType": "ORDER_NOTIFICATION",
"referenceId": "{{orderId}}",
"status": "SENT",
"sentAt": "2025-11-07T10:30:00",
"errorMessage": null,
"createdBy": "system",
"createdAt": "2025-11-07T10:25:00",
"updatedBy": "system",
"updatedAt": "2025-11-07T10:30:00"
}
}

검증 항목:
- ✅ Status: 201 Created
- ✅ senderType: "SYSTEM"
- ✅ messageType: "ORDER_NOTIFICATION"
- ✅ status: "SENT" (Slack 발송 성공)
- ✅ messageContent에 AI 계산 결과 포함

  ---
3️⃣ 수동 메시지 발송 (User API)

목적: 인증된 사용자가 직접 메시지 발송

⚠️ 사전 준비: user-service에서 사용자 생성 및 로그인 필요

POST http://localhost:8700/api/v1/notifications/manual
Content-Type: application/json
X-User-Id: {{userId}}
X-User-Role: ROLE_HUB_MANAGER
X-Username: testuser

{
"recipientSlackId": "U98765ZYXWV",
"recipientName": "이수신",
"messageContent": "테스트 메시지입니다. 확인 부탁드립니다."
}

Expected Response (201 Created):
{
"isSuccess": true,
"code": 201,
"message": "CREATED",
"data": {
"id": "650e8400-e29b-41d4-a716-446655440001",
"senderType": "USER",
"senderUsername": "testuser",
"senderSlackId": "U123456",
"senderName": "테스트 사용자",
"recipientSlackId": "U98765ZYXWV",
"recipientName": "이수신",
"messageContent": "테스트 메시지입니다. 확인 부탁드립니다.",
"messageType": "MANUAL",
"referenceId": null,
"status": "SENT",
"sentAt": "2025-11-07T11:00:00",
"errorMessage": null,
"createdBy": "testuser",
"createdAt": "2025-11-07T11:00:00",
"updatedBy": "testuser",
"updatedAt": "2025-11-07T11:00:00"
}
}

검증 항목:
- ✅ Status: 201 Created
- ✅ senderType: "USER"
- ✅ senderUsername: "testuser" (스냅샷 저장)
- ✅ messageType: "MANUAL"
- ✅ status: "SENT"

권한 없이 호출 시 (403 Forbidden):
POST http://localhost:8700/api/v1/notifications/manual
Content-Type: application/json

{...}

  ---
4️⃣ 알림 단일 조회

GET http://localhost:8700/api/v1/notifications/{{notificationId}}
X-User-Id: {{userId}}
X-User-Role: ROLE_MASTER
X-Username: admin

Expected Response (200 OK):
{
"isSuccess": true,
"code": 200,
"message": "OK",
"data": {
"id": "550e8400-e29b-41d4-a716-446655440000",
...
}
}

  ---
5️⃣ 알림 목록 조회 (Pagination)

GET
http://localhost:8700/api/v1/notifications?page=0&size=10&sortBy=createdAt&direction=DESC
X-User-Id: {{userId}}
X-User-Role: ROLE_MASTER
X-Username: admin

Expected Response (200 OK):
{
"isSuccess": true,
"code": 200,
"message": "OK",
"data": {
"content": [
{
"id": "...",
"senderType": "SYSTEM",
...
}
],
"pageable": {
"pageNumber": 0,
"pageSize": 10,
"sort": {
"sorted": true,
"unsorted": false,
"empty": false
}
},
"totalElements": 25,
"totalPages": 3,
"last": false,
"size": 10,
"number": 0,
"first": true,
"numberOfElements": 10,
"empty": false
}
}

검증 항목:
- ✅ Pagination 정보 정확
- ✅ sortBy=createdAt, direction=DESC 적용
- ✅ MASTER 권한 필요

  ---
6️⃣ 외부 API 로그 전체 조회

GET http://localhost:8700/api/v1/notifications/api-logs
X-User-Id: {{userId}}
X-User-Role: ROLE_MASTER
X-Username: admin

Expected Response (200 OK):
{
"isSuccess": true,
"code": 200,
"message": "OK",
"data": [
{
"id": "750e8400-e29b-41d4-a716-446655440000",
"apiProvider": "GEMINI",
"apiMethod": "generateContent",
"requestData": {...},
"responseData": {...},
"httpStatus": 200,
"isSuccess": true,
"durationMs": 1250,
"cost": 0.0015,
"messageId": "550e8400-e29b-41d4-a716-446655440000",
"createdAt": "2025-11-07T10:25:00"
},
{
"id": "850e8400-e29b-41d4-a716-446655440001",
"apiProvider": "SLACK",
"apiMethod": "chat.postMessage",
"requestData": {...},
"responseData": {...},
"httpStatus": 200,
"isSuccess": true,
"durationMs": 320,
"cost": 0.0,
"messageId": "550e8400-e29b-41d4-a716-446655440000",
"createdAt": "2025-11-07T10:30:00"
}
]
}

검증 항목:
- ✅ MASTER 권한 필요
- ✅ Gemini + Slack API 로그 모두 존재
- ✅ messageId로 알림과 연결

  ---
7️⃣ 외부 API 로그 Provider별 조회

GET http://localhost:8700/api/v1/notifications/api-logs/provider/SLACK
X-User-Id: {{userId}}
X-User-Role: ROLE_MASTER
X-Username: admin

Expected Response (200 OK):
{
"isSuccess": true,
"code": 200,
"message": "OK",
"data": [
{
"id": "...",
"apiProvider": "SLACK",
...
}
]
}

Provider 옵션: SLACK, GEMINI, NAVER_MAPS

  ---
8️⃣ 외부 API 로그 메시지 ID별 조회

GET http://localhost:8700/api/v1/notifications/api-logs/message/{{notificationId}}
X-User-Id: {{userId}}
X-User-Role: ROLE_MASTER
X-Username: admin

Expected Response (200 OK):
{
"isSuccess": true,
"code": 200,
"message": "OK",
"data": [
{
"id": "...",
"apiProvider": "GEMINI",
"messageId": "{{notificationId}}"
},
{
"id": "...",
"apiProvider": "SLACK",
"messageId": "{{notificationId}}"
}
]
}

검증 항목:
- ✅ 한 메시지에 대해 Gemini + Slack 2개 로그 존재

  ---
Step 3: 테스트 체크리스트

✅ 기능 테스트

- 주문 알림 발송 성공 (201, SYSTEM 타입)
- Gemini AI 응답 포함 (messageContent에 "최종 발송 시한" 포함)
- Slack 메시지 실제 발송 확인 (Slack 앱에서 확인)
- 수동 메시지 발송 성공 (201, USER 타입)
- 발신자 정보 스냅샷 저장 확인
- 알림 단일 조회 성공 (200)
- 알림 목록 페이징 조회 성공 (200)
- API 로그 전체 조회 성공 (200)
- API 로그 Provider별 조회 성공 (200)
- API 로그 메시지 ID별 조회 성공 (200)

✅ 권한 테스트

- 수동 메시지 발송 - 인증 없이 호출 (403)
- 알림 목록 조회 - MASTER 외 권한 (user-service 구현 후 테스트)
- API 로그 조회 - MASTER 외 권한 (user-service 구현 후 테스트)

✅ 외부 API 연동

- Slack API 호출 성공 (p_external_api_logs 확인)
- Gemini API 호출 성공 (p_external_api_logs 확인)
- API 로그 DB 저장 확인

✅ DB 확인

-- PostgreSQL 접속
docker exec -it postgres-ofl psql -U root -d oneforlogis

-- 알림 데이터 확인
\c oneforlogis_notification
SELECT * FROM p_notifications ORDER BY created_at DESC LIMIT 5;

-- API 로그 확인
SELECT * FROM p_external_api_logs ORDER BY created_at DESC LIMIT 10;

  ---
Step 4: 문제 발생 시 디버깅

로그 확인

# notification-service 로그
docker-compose -f docker-compose-team.yml logs -f notification-service

# 전체 서비스 로그
docker-compose -f docker-compose-team.yml logs -f

일반적인 문제

1. Slack API 호출 실패

증상: status: "FAILED", errorMessage: "invalid_auth"
원인: Slack Bot Token 문제
해결: .env.docker의 SLACK_BOT_TOKEN 확인

2. Gemini API 호출 실패

증상: status: "SENT" but messageContent에 "AI 계산 실패"
원인: Gemini API Key 문제
해결: .env.docker의 GEMINI_API_KEY 확인

3. User FeignClient 실패

증상: 500 Internal Server Error
원인: user-service 미구현 또는 미실행
해결: user-service 구현 완료까지 Header로 직접 전달
