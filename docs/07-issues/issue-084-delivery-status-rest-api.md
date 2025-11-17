# Issue #84 - Delivery Status Notification REST API

## 작업 개요

**Branch**: `refactor/#76-notification-risk-refatoring`
**작업자**: 박근용
**작업 기간**: 2025-11-13
**상태**: ✅ 완료 (Controller 테스트 2/2, 스크립트 업데이트)

## 작업 배경

Issue #35에서 DeliveryStatusChangedConsumer (Kafka Event 기반)를 구현했으나, 주문 알림은 REST + Kafka 둘 다 지원하는 반면 배송 상태 알림은 Kafka만 지원하여 일관성 부족. REST API 추가로 다음 기능 제공:

1. **일관성 유지**: 주문 알림과 동일하게 REST + Kafka 둘 다 지원
2. **재발송 기능**: Slack 발송 실패 시 수동 재전송 가능
3. **테스트/디버깅**: REST API로 직접 테스트 가능
4. **장애 대응**: Kafka 장애 시 대체 수단 확보

## 작업 내용

### 완료 항목 (6/6)

#### 1. ✅ DeliveryStatusNotificationRequest DTO 작성

**파일**: `notification-service/src/main/java/.../presentation/request/DeliveryStatusNotificationRequest.java`

```java
public record DeliveryStatusNotificationRequest(
    @NotNull UUID deliveryId,
    @NotNull UUID orderId,
    @NotBlank String previousStatus,
    @NotBlank String currentStatus,
    @NotBlank String recipientSlackId,
    @NotBlank String recipientName
) {}
```

**주요 특징**:
- Jakarta Validation 적용 (@NotNull, @NotBlank)
- Swagger 문서화 (@Schema)
- DeliveryStatusChangedEvent.DeliveryData와 동일한 필드 구조

#### 2. ✅ NotificationService.sendDeliveryStatusNotification() 메서드 추가

**파일**: `notification-service/src/main/java/.../application/service/NotificationService.java`

```java
@Transactional
public NotificationResponse sendDeliveryStatusNotification(DeliveryStatusNotificationRequest request) {
    log.info("[NotificationService] 배송 상태 변경 알림 발송 시작 - deliveryId: {}, status: {} → {}",
            request.deliveryId(), request.previousStatus(), request.currentStatus());

    // Step 1: Slack 메시지 생성
    String message = buildDeliveryStatusChangeMessage(request);

    // Step 2: Notification 엔티티 생성 및 저장
    Notification notification = Notification.builder()
            .senderType(SenderType.SYSTEM)
            .senderUsername(null)
            .senderSlackId(null)
            .senderName(null)
            .recipientSlackId(request.recipientSlackId())
            .recipientName(request.recipientName())
            .messageContent(message)
            .messageType(MessageType.DELIVERY_STATUS_UPDATE)
            .referenceId(request.deliveryId())
            .eventId(null)  // REST API 호출이므로 eventId 없음
            .build();

    Notification savedNotification = notificationRepository.save(notification);

    // Step 3: Slack 메시지 발송
    SlackMessageRequest slackRequest = SlackMessageRequest.builder()
            .channel(request.recipientSlackId())
            .text(message)
            .build();

    SlackMessageResponse slackResponse = slackClientWrapper.postMessage(slackRequest, savedNotification.getId());

    // Step 4: 발송 상태 업데이트 (실패 시 예외 throw)
    if (slackResponse != null && slackResponse.isOk()) {
        savedNotification.markAsSent();
        log.info("[NotificationService] 배송 상태 변경 알림 발송 성공 - deliveryId: {}, notificationId: {}",
                request.deliveryId(), savedNotification.getId());
        return NotificationResponse.from(savedNotification);
    } else {
        String errorMsg = slackResponse != null ? slackResponse.getError() : "Unknown error";
        savedNotification.markAsFailed(errorMsg);
        log.error("[NotificationService] 배송 상태 변경 알림 발송 실패 - deliveryId: {}, error: {}",
                request.deliveryId(), errorMsg);
        throw new CustomException(ErrorCode.NOTIFICATION_SEND_FAILED);
    }
}

private String buildDeliveryStatusChangeMessage(DeliveryStatusNotificationRequest request) {
    return String.format(
            """
            🚚 *배송 상태 업데이트*

            배송 ID: `%s`
            주문 ID: `%s`
            이전 상태: `%s`
            현재 상태: `%s`

            수령인: %s
            """,
            request.deliveryId(),
            request.orderId(),
            request.previousStatus(),
            request.currentStatus(),
            request.recipientName()
    );
}
```

**주요 특징**:
- DeliveryStatusChangedConsumer 로직 재사용
- SYSTEM 타입 알림 생성
- 메시지 타입: DELIVERY_STATUS_UPDATE
- Slack 발송 실패 시 CustomException throw (HTTP 500 응답)
- DeliveryStatusChangedConsumer와 동일한 메시지 형식

#### 3. ✅ POST /api/v1/notifications/delivery-status 엔드포인트 추가

**파일**: `notification-service/src/main/java/.../presentation/controller/NotificationController.java`

```java
@Operation(
        summary = "배송 상태 변경 알림 발송",
        description = "배송 상태가 변경될 때 Slack 알림을 발송합니다. delivery-service에서 호출하거나 수동 재발송 시 사용합니다."
)
@PreAuthorize("hasAnyRole('MASTER', 'HUB_MANAGER', 'DELIVERY_MANAGER', 'COMPANY_MANAGER')")
@PostMapping("/delivery-status")
@ResponseStatus(HttpStatus.CREATED)
public ApiResponse<NotificationResponse> sendDeliveryStatusNotification(
        @Valid @RequestBody DeliveryStatusNotificationRequest request
) {
    log.info("[NotificationController] POST /api/v1/notifications/delivery-status - deliveryId: {}, status: {} → {}",
            request.deliveryId(), request.previousStatus(), request.currentStatus());

    NotificationResponse response = notificationService.sendDeliveryStatusNotification(request);
    return ApiResponse.created(response);
}
```

**주요 특징**:
- 권한: MASTER, HUB_MANAGER, DELIVERY_MANAGER, COMPANY_MANAGER
- HTTP 201 Created 응답
- Swagger 문서화
- Jakarta Validation 자동 적용

#### 4. ✅ Controller 테스트 작성

**파일**: `notification-service/src/test/java/.../presentation/controller/NotificationControllerTest.java`

```java
@Test
@DisplayName("배송 상태 변경 알림 발송 API - 성공 (201 Created)")
void sendDeliveryStatusNotification_Success() throws Exception {
    // Given
    UUID deliveryId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    DeliveryStatusNotificationRequest request = new DeliveryStatusNotificationRequest(
            deliveryId,
            orderId,
            "HUB_WAITING",
            "HUB_MOVING",
            "U123456",
            "배송담당자"
    );

    NotificationResponse response = new NotificationResponse(
            UUID.randomUUID(),
            SenderType.SYSTEM,
            null, null, null,
            "U123456",
            "배송담당자",
            "🚚 *배송 상태 업데이트*\n\n배송 ID: `" + deliveryId + "`\n주문 ID: `" + orderId + "`\n이전 상태: `HUB_WAITING`\n현재 상태: `HUB_MOVING`\n\n수령인: 배송담당자\n",
            MessageType.DELIVERY_STATUS_UPDATE,
            deliveryId,
            MessageStatus.SENT,
            LocalDateTime.now().toString(),
            null,
            "system",
            LocalDateTime.now().toString(),
            "system",
            LocalDateTime.now().toString()
    );

    when(notificationService.sendDeliveryStatusNotification(any(DeliveryStatusNotificationRequest.class)))
            .thenReturn(response);

    // When & Then
    mockMvc.perform(post("/api/v1/notifications/delivery-status")
                    .with(authentication(createAuthentication("testuser", Role.DELIVERY_MANAGER)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.status").value("SENT"))
            .andExpect(jsonPath("$.data.messageType").value("DELIVERY_STATUS_UPDATE"))
            .andExpect(jsonPath("$.data.senderType").value("SYSTEM"));
}

@Test
@DisplayName("배송 상태 변경 알림 발송 API - 필수 필드 누락 시 400 Bad Request")
void sendDeliveryStatusNotification_MissingFields_400() throws Exception {
    // Given - deliveryId 누락
    DeliveryStatusNotificationRequest request = new DeliveryStatusNotificationRequest(
            null,  // deliveryId 누락
            UUID.randomUUID(),
            "HUB_WAITING",
            "HUB_MOVING",
            "U123456",
            "배송담당자"
    );

    // When & Then
    mockMvc.perform(post("/api/v1/notifications/delivery-status")
                    .with(authentication(createAuthentication("testuser", Role.DELIVERY_MANAGER)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
}
```

**테스트 커버리지**:
- ✅ 성공 케이스: 201 Created 응답
- ✅ 필수 필드 누락: 400 Bad Request

#### 5. ✅ test-notification-api.sh 스크립트 업데이트

**파일**: `notification-service/scripts/test-notification-api.sh`

**변경 사항**:
- Test 2: 배송 상태 변경 알림 발송 추가
- 총 테스트 수: 9개 → 10개

```bash
# ============================================
# Test 2: 배송 상태 변경 알림 발송 (Auth Required - Issue #84)
# ============================================
DELIVERY_ID=$(powershell -Command "[guid]::NewGuid().ToString()")
ORDER_ID_FOR_DELIVERY=$(powershell -Command "[guid]::NewGuid().ToString()")

DELIVERY_STATUS_DATA=$(cat <<EOF
{
  "deliveryId": "$DELIVERY_ID",
  "orderId": "$ORDER_ID_FOR_DELIVERY",
  "previousStatus": "HUB_WAITING",
  "currentStatus": "HUB_MOVING",
  "recipientSlackId": "C09QY22AMEE",
  "recipientName": "Delivery Manager"
}
EOF
)

run_test \
    "배송 상태 변경 알림 발송 - 권한 없음 (POST /delivery-status)" \
    "POST" \
    "$BASE_URL/delivery-status" \
    "$DELIVERY_STATUS_DATA" \
    "" \
    "403"
```

#### 6. ✅ test-kafka-consumer.sh 확인

**파일**: `notification-service/scripts/test-kafka-consumer.sh`

Issue #35에서 이미 delivery.status.changed 이벤트 테스트 포함되어 있어 수정 불필요.
- Test 3: delivery.status.changed 이벤트 발행 → 알림 생성 확인
- Test 4: 멱등성 검증 - 동일 eventId 중복 발행

## 기술 스택

- Spring Boot 3.3.2
- Spring Web MVC
- Spring Security
- PostgreSQL 17
- Jakarta Validation
- Swagger (springdoc-openapi)

## 파일 변경 사항

### 신규 생성 (1개 파일)

**Request DTO**
```
notification-service/src/main/java/com/oneforlogis/notification/presentation/request/
└── DeliveryStatusNotificationRequest.java
```

### 수정 (3개 파일)

**Application Service**
```
notification-service/src/main/java/com/oneforlogis/notification/application/service/
└── NotificationService.java
    - sendDeliveryStatusNotification() 메서드 추가
    - buildDeliveryStatusChangeMessage() 헬퍼 메서드 추가
```

**Controller**
```
notification-service/src/main/java/com/oneforlogis/notification/presentation/controller/
└── NotificationController.java
    - POST /api/v1/notifications/delivery-status 엔드포인트 추가
```

**Controller Test**
```
notification-service/src/test/java/com/oneforlogis/notification/presentation/controller/
└── NotificationControllerTest.java
    - sendDeliveryStatusNotification_Success() 테스트 추가
    - sendDeliveryStatusNotification_MissingFields_400() 테스트 추가
```

**Test Script**
```
notification-service/scripts/
└── test-notification-api.sh
    - Test 2: 배송 상태 변경 알림 발송 추가
    - 총 테스트 수: 9개 → 10개
```

## API 명세

### POST /api/v1/notifications/delivery-status

**Description**: 배송 상태 변경 알림을 Slack으로 발송합니다.

**Authorization**: Required (MASTER, HUB_MANAGER, DELIVERY_MANAGER, COMPANY_MANAGER)

**Request Body**:
```json
{
  "deliveryId": "550e8400-e29b-41d4-a716-446655440001",
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "previousStatus": "HUB_WAITING",
  "currentStatus": "HUB_MOVING",
  "recipientSlackId": "U123456",
  "recipientName": "배송담당자"
}
```

**Response**: 201 Created
```json
{
  "data": {
    "id": "uuid",
    "senderType": "SYSTEM",
    "senderUsername": null,
    "senderSlackId": null,
    "senderName": null,
    "recipientSlackId": "U123456",
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

**Error Response**: 500 Internal Server Error (Slack 발송 실패 시)
```json
{
  "error": {
    "code": "NOTIFICATION_SEND_FAILED",
    "message": "Slack message send failed: channel_not_found"
  }
}
```

## 사용 시나리오

### 1. delivery-service에서 자동 호출

```java
// delivery-service: DeliveryService.updateStatus()
@Transactional
public void updateStatus(UUID deliveryId, DeliveryStatus newStatus) {
    Delivery delivery = deliveryRepository.findById(deliveryId)
        .orElseThrow(() -> new CustomException(ErrorCode.DELIVERY_NOT_FOUND));

    DeliveryStatus previousStatus = delivery.getStatus();
    delivery.updateStatus(newStatus);

    // notification-service 호출
    DeliveryStatusNotificationRequest request = new DeliveryStatusNotificationRequest(
        delivery.getId(),
        delivery.getOrderId(),
        previousStatus.name(),
        newStatus.name(),
        delivery.getRecipientSlackId(),
        delivery.getRecipientName()
    );

    notificationServiceClient.sendDeliveryStatusNotification(request);
}
```

### 2. 수동 재발송 (Slack 실패 시)

```bash
# Slack 발송 실패한 배송 건에 대해 수동 재발송
curl -X POST http://localhost:8700/api/v1/notifications/delivery-status \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -H "X-User-Role: DELIVERY_MANAGER" \
  -d '{
    "deliveryId": "550e8400-e29b-41d4-a716-446655440001",
    "orderId": "550e8400-e29b-41d4-a716-446655440000",
    "previousStatus": "HUB_WAITING",
    "currentStatus": "HUB_MOVING",
    "recipientSlackId": "U123456",
    "recipientName": "배송담당자"
  }'
```

### 3. 테스트/디버깅

```bash
# Docker 환경에서 REST API 테스트
bash notification-service/scripts/test-notification-api.sh

# 특정 배송 건 알림 발송 테스트
curl -X POST http://localhost:8700/api/v1/notifications/delivery-status \
  -H "Content-Type: application/json" \
  -d @test-delivery-status-data.json
```

## 주요 구현 사항

### 1. DeliveryStatusChangedConsumer와 로직 일관성

**Consumer (Kafka Event)**:
```java
@KafkaListener(topics = "#{@topicProperties.deliveryStatusChanged}")
public void onMessage(DeliveryStatusChangedEvent event) {
    String message = buildStatusChangeMessage(event.delivery());

    Notification notification = Notification.builder()
        .senderType(SenderType.SYSTEM)
        .messageType(MessageType.DELIVERY_STATUS_UPDATE)
        .referenceId(event.delivery().deliveryId())
        .eventId(event.eventId())  // Kafka eventId
        .build();

    // ... Slack 발송
}
```

**Service (REST API)**:
```java
@Transactional
public NotificationResponse sendDeliveryStatusNotification(DeliveryStatusNotificationRequest request) {
    String message = buildDeliveryStatusChangeMessage(request);

    Notification notification = Notification.builder()
        .senderType(SenderType.SYSTEM)
        .messageType(MessageType.DELIVERY_STATUS_UPDATE)
        .referenceId(request.deliveryId())
        .eventId(null)  // REST API는 eventId 없음
        .build();

    // ... Slack 발송
}
```

**차이점**:
- `eventId`: Kafka는 이벤트 ID 존재 (멱등성), REST는 null
- 메시지 형식: 동일 (buildStatusChangeMessage)
- 로직: 동일 (DB 저장 → Slack 발송 → 상태 업데이트)

### 2. 에러 처리 전략

**Slack 발송 실패 시**:
1. DB에 FAILED 상태 저장 (에러 메시지 포함)
2. CustomException 발생
3. GlobalExceptionHandler에서 500 Internal Server Error 응답

**이유**:
- 클라이언트에 명확한 실패 전달
- 이력 유지 (DB에 FAILED 상태)
- 재발송 가능 (동일한 deliveryId로 재호출)

## 테스트 커버리지

### Controller Tests (2/2 통과)

**NotificationControllerTest**

1. ✅ **Test 1: 배송 상태 변경 알림 발송 성공**
   - Given: 유효한 DeliveryStatusNotificationRequest
   - When: POST /api/v1/notifications/delivery-status
   - Then: 201 Created, SENT 상태, DELIVERY_STATUS_UPDATE 타입

2. ✅ **Test 2: 필수 필드 누락 시 400 Bad Request**
   - Given: deliveryId가 null인 Request
   - When: POST /api/v1/notifications/delivery-status
   - Then: 400 Bad Request (Jakarta Validation 에러)

### Integration Tests (기존 테스트 활용)

**test-kafka-consumer.sh** (4/4 통과)

- ✅ Test 3: delivery.status.changed 이벤트 발행 → 알림 생성 확인
- ✅ Test 4: 멱등성 검증 - 동일 eventId 중복 발행

**test-notification-api.sh** (10/10 통과)

- ✅ Test 2: 배송 상태 변경 알림 발송 - 권한 없음 (403 Forbidden)

## 다음 단계

### Issue #85: deletedBy 사용자 정보 수집 (예상 0.5일)
**우선순위**: 🟡 보통

- AuthContextUtil 헬퍼 클래스 구현
- NotificationRepositoryImpl, ExternalApiLogRepositoryImpl 수정
- SecurityContext에서 username 자동 수집

### Issue #86: Kafka Consumer 보안 강화 (예상 1일)
**우선순위**: 🔴 높음 (CVSS 7.5)

- 이벤트 서명 검증 (HMAC-SHA256)
- Event DTO에 signature 필드 추가
- order-service, delivery-service와 협의 필요

### Issue #87-88: Performance 개선 (예상 1.5일)
**우선순위**: 🟢 낮음

- Gemini API 응답 캐싱 (Caffeine)
- Dead Letter Queue 구현

### Issue #36: Daily Route Optimization (Challenge, 예상 3-4일)
**우선순위**: 🟢 낮음

- Naver Maps API 클라이언트 구현
- 일일 배송 경로 최적화 스케줄러
- Gemini TSP 프롬프트

## 기술적 결정 사항

### 1. eventId 처리 방식

**결정**: REST API는 eventId를 null로 저장
**이유**:
- Kafka 이벤트: eventId 필수 (멱등성 보장)
- REST API: 중복 호출 허용 (재발송 시나리오)
- DB 스키마: eventId nullable

### 2. 권한 설정

**결정**: MASTER, HUB_MANAGER, DELIVERY_MANAGER, COMPANY_MANAGER
**이유**:
- 배송 상태 알림은 모든 역할이 필요할 수 있음 (재발송 시나리오)
- 필요 시 INTERNAL_SERVICE_ONLY로 변경 가능

### 3. HTTP 응답 코드

**결정**: 201 Created (성공), 500 Internal Server Error (실패)
**이유**:
- 201: 알림 리소스 생성 성공
- 500: Slack 발송 실패는 서버 책임 (클라이언트가 재시도 필요)

## 참고 문서

- [Issue #35: Kafka Consumer 구현](./issue-35-notification-kafka-consumer.md)
- [Issue #76: Risk Refactoring](./issue-76-notification-risk-refactoring.md)
- [CLAUDE.md](../../CLAUDE.md)
- [docs/completed-work.md](../completed-work.md)
- [docs/service-status.md](../service-status.md)
- [docs/left-issue.md](../left-issue.md)

## 성과

- ✅ 배송 상태 알림 REST API 추가 완료
- ✅ Kafka Event + REST API 일관성 유지
- ✅ 재발송 기능 제공 (Slack 실패 시)
- ✅ Controller 테스트 2/2 통과
- ✅ test-notification-api.sh 스크립트 업데이트 (10개 테스트)
- ✅ DeliveryStatusChangedConsumer 로직 재사용

## 커밋 메시지

```
feat: add delivery status notification REST API

- POST /api/v1/notifications/delivery-status 엔드포인트 추가
- DeliveryStatusNotificationRequest DTO 작성
- NotificationService.sendDeliveryStatusNotification() 메서드 추가
- DeliveryStatusChangedConsumer 로직 재사용 (메시지 형식 통일)
- Controller 테스트 2개 추가 (성공, 필수 필드 누락)
- test-notification-api.sh 스크립트 업데이트 (Test 2 추가)

Related to Issue #84
```
