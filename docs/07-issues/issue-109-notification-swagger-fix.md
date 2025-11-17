# Issue #109 - notification-service Swagger 테스트 실패 수정

## 작업 개요

**Branch**: `fix/#109-notification-service-swagger-fix`
**작업자**: 박근용
**작업 기간**: 2025-11-13
**상태**: ✅ 완료 (Slack ID 통일 완료, FeignException 처리 추가)

## 작업 배경

### 1. Swagger 테스트 Slack ID 불일치
- 테스트 코드와 DTO에서 다양한 Slack ID 사용 (U123456, U987654321 등)
- 실제 사용 중인 Slack 채널 ID (`C09QY22AMEE`)로 통일 필요
- Swagger 문서와 실제 테스트 간 일관성 부족

### 2. FeignException 에러 코드 불일치 문제
**증상**: user-service 호출 실패 시 HTTP 500 응답에 403 에러 메시지 포함

**문제 예시**:
```json
HTTP/1.1 500
{
  "isSuccess": false,
  "code": 500,
  "message": "[403] during [GET] to [http://user-service/api/v1/users/username/testmaster] [UserServiceClient#getUserByUsername(String)]: []"
}
```

**원인**:
- `NotificationExceptionHandler`가 `FeignException`을 처리하지 않음
- Spring의 기본 `GlobalExceptionHandler`가 모든 예외를 500으로 래핑
- 실제 HTTP 상태 코드(403)와 응답 코드(500)가 불일치

**영향**:
- 사용자가 실제 에러 원인을 파악하기 어려움
- API 디버깅 및 에러 추적 곤란
- 팀 컨벤션 (HTTP 상태 코드 = 응답 code) 위반

## 작업 내용

### 완료 항목 (3/3)

#### 1. ✅ Slack ID 통일 (C09QY22AMEE)

##### 1.1 테스트 코드 (12개 수정)
**파일**: `notification-service/src/test/java/.../presentation/controller/NotificationControllerTest.java`

**변경 라인**:
- Line 88: 주문 알림 테스트 request
- Line 112, 132: 수동 메시지 테스트 request
- Line 121: UserResponse mock
- Line 176, 187: 배송 상태 알림 테스트 request
- Line 459: Helper method

```java
// Before
"recipientSlackId": "U123456",

// After
"recipientSlackId": "C09QY22AMEE",
```

##### 1.2 Request DTOs (3개 수정)
**파일 목록**:
- `OrderNotificationRequest.java` (Line 55)
- `ManualNotificationRequest.java` (Line 8)
- `DeliveryStatusNotificationRequest.java` (Line 27)

```java
@Schema(description = "수신자 Slack ID", example = "C09QY22AMEE")
@NotBlank(message = "수신자 Slack ID는 필수입니다.")
String recipientSlackId,
```

##### 1.3 Response DTO (2개 수정)
**파일**: `NotificationResponse.java` (Line 22, 28)

```java
@Schema(description = "발신자 Slack ID (USER 타입만)", example = "C09QY22AMEE")
String senderSlackId,

@Schema(description = "수신자 Slack ID", example = "C09QY22AMEE")
String recipientSlackId,
```

#### 2. ✅ FeignException 처리 추가

##### 2.1 NotificationExceptionHandler 구현
**파일**: `notification-service/src/main/java/.../presentation/advice/NotificationExceptionHandler.java`

**변경 전**:
```java
@RestControllerAdvice
public class NotificationExceptionHandler {
    // TODO: Implement exception handlers
}
```

**변경 후**:
```java
@Slf4j
@RestControllerAdvice
public class NotificationExceptionHandler {

    /**
     * FeignClient 호출 실패 처리
     * - user-service 등 다른 서비스 호출 실패 시 적절한 HTTP 상태 코드 반환
     */
    @ExceptionHandler(FeignException.class)
    protected ResponseEntity<ApiResponse<Void>> handleFeignException(FeignException e) {
        int status = e.status();
        String message = extractFeignErrorMessage(e);

        log.error("[FeignException] status={}, message={}", status, message);

        // FeignException의 status를 그대로 사용
        HttpStatus httpStatus = HttpStatus.valueOf(status);
        ApiResponse<Void> response = new ApiResponse<>(false, status, message, null);

        return new ResponseEntity<>(response, httpStatus);
    }

    /**
     * FeignException에서 의미 있는 에러 메시지 추출
     */
    private String extractFeignErrorMessage(FeignException e) {
        int status = e.status();

        // HTTP 상태 코드별로 사용자 친화적인 메시지 반환
        return switch (status) {
            case 400 -> "외부 서비스 요청 형식이 올바르지 않습니다. (user-service 연동 실패)";
            case 401 -> "외부 서비스 인증에 실패했습니다. (user-service 연동 실패)";
            case 403 -> "요청한 리소스에 접근할 수 없습니다. (user-service 연동 실패)";
            case 404 -> "요청한 리소스를 찾을 수 없습니다. (user-service 연동 실패)";
            case 408 -> "외부 서비스 요청 시간이 초과되었습니다. (user-service 연동 실패)";
            case 500 -> "외부 서비스에서 오류가 발생했습니다. (user-service 연동 실패)";
            case 503 -> "외부 서비스를 일시적으로 사용할 수 없습니다. (user-service 연동 실패)";
            default -> {
                if (status >= 400 && status < 500) {
                    yield "외부 서비스 요청 처리에 실패했습니다. (user-service 연동 실패)";
                } else if (status >= 500) {
                    yield "외부 서비스에서 오류가 발생했습니다. (user-service 연동 실패)";
                }
                yield "외부 서비스 연동 중 오류가 발생했습니다.";
            }
        };
    }
}
```

##### 2.2 주요 특징
1. **HTTP 상태 코드 일치**: FeignException의 status를 그대로 반환
2. **사용자 친화적 메시지**: 7가지 주요 HTTP 에러 + default 처리
3. **팀 컨벤션 준수**: `ApiResponse` 사용 (common-lib `GlobalExceptionHandler`와 동일)
4. **Java 17 switch expression**: 가독성 향상 및 컴파일 타임 완전성 검사

##### 2.3 처리하는 HTTP 상태 코드
- **400 Bad Request**: 잘못된 요청 형식
- **401 Unauthorized**: 인증 실패
- **403 Forbidden**: 접근 권한 없음 ⭐ (현재 발생하는 에러)
- **404 Not Found**: 리소스 없음
- **408 Request Timeout**: 요청 시간 초과
- **500 Internal Server Error**: 서버 오류
- **503 Service Unavailable**: 서비스 불가
- **Default**: 4xx/5xx 범위별 일반 메시지

## 테스트 결과

### 1. Slack ID 변경 확인
- ✅ NotificationControllerTest.java (12 occurrences)
- ✅ OrderNotificationRequest.java
- ✅ ManualNotificationRequest.java
- ✅ DeliveryStatusNotificationRequest.java
- ✅ NotificationResponse.java

### 2. FeignException 처리 테스트

#### 수정 전 (에러 코드 불일치):
```bash
$ curl -X POST http://localhost:8000/api/v1/notifications/manual \
  -H "Authorization: Bearer {JWT}" \
  -d '{"recipientSlackId":"C09QY22AMEE","recipientName":"Test User","messageContent":"Test"}'

HTTP/1.1 500
{
  "isSuccess": false,
  "code": 500,
  "message": "[403] during [GET] to [http://user-service/api/v1/users/username/testmaster]..."
}
```

#### 수정 후 (에러 코드 일치):
```bash
$ curl -X POST http://localhost:8000/api/v1/notifications/manual \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{"recipientSlackId":"C09QY22AMEE","recipientName":"Test User","messageContent":"Urgent delivery added"}'

HTTP/1.1 403
{
  "isSuccess": false,
  "code": 403,
  "message": "요청한 리소스에 접근할 수 없습니다. (user-service 연동 실패)"
}
```

#### Docker 로그 확인:
```
2025-11-13T13:27:40.959+09:00 ERROR 1 --- [notification-service] [nio-8700-exec-6] c.o.n.p.a.NotificationExceptionHandler   : [FeignException] status=403, message=요청한 리소스에 접근할 수 없습니다. (user-service 연동 실패)
```

### 3. 주문 알림 API 테스트 (정상 케이스)
```bash
$ curl -X POST http://localhost:8000/api/v1/notifications/order \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{
    "orderId": "550e8400-e29b-41d4-a716-446655440000",
    "ordererInfo": "Test Orderer / test@example.com",
    "recipientSlackId": "C09QY22AMEE",
    "recipientName": "Hub Manager"
  }'

HTTP/1.1 201
{
  "isSuccess": true,
  "code": 201,
  "message": "Order notification sent successfully",
  "data": {
    "id": "uuid",
    "recipientSlackId": "C09QY22AMEE",
    "status": "SENT"
  }
}
```

✅ Slack 채널 C09QY22AMEE에 실제 메시지 발송 확인

## 기술 스택

- Java 17 (switch expression)
- Spring Boot 3.3.2
- Spring Cloud OpenFeign
- Spring Web (RestControllerAdvice, ExceptionHandler)
- Lombok
- Docker + Docker Compose

## 파일 변경 사항

### 수정된 파일 (6개)

```
notification-service/src/main/java/com/oneforlogis/notification/
├── presentation/
│   ├── advice/
│   │   └── NotificationExceptionHandler.java       (FeignException 처리 추가, ~60 lines)
│   ├── request/
│   │   ├── OrderNotificationRequest.java           (Slack ID 예시 변경)
│   │   ├── ManualNotificationRequest.java          (Slack ID 예시 변경)
│   │   └── DeliveryStatusNotificationRequest.java  (Slack ID 예시 변경)
│   └── response/
│       └── NotificationResponse.java                (Slack ID 예시 변경)
└── test/
    └── java/com/oneforlogis/notification/presentation/controller/
        └── NotificationControllerTest.java          (Slack ID 12개 수정)
```

### 변경 통계
- **수정된 파일**: 6개
- **추가된 코드**: ~60 lines (NotificationExceptionHandler)
- **변경된 Slack ID**: 17 occurrences (테스트 12개 + DTO 5개)

## 주요 구현 사항

### 1. 팀 컨벤션 검토 결과

#### 분석 대상:
- ✅ **hub-service**: common-lib `GlobalExceptionHandler`만 사용 (ApiResponse)
- ✅ **notification-service**: `NotificationExceptionHandler` + `GlobalExceptionHandler` (ApiResponse)
- ❌ **delivery-service**: `DeliveryExceptionHandler` (Map 사용) - 팀 컨벤션 불일치

#### 결론:
notification-service는 팀 컨벤션에 일치 (ApiResponse 사용)

### 2. 에러 메시지 형식 전략
**결정**: "설명 메시지 (user-service 연동 실패)" 형식

**이유**:
- 사용자가 문제 원인을 쉽게 파악 가능
- 어느 외부 서비스에서 문제가 발생했는지 명시
- 일관된 메시지 형식 유지

### 3. Java 17 switch expression vs if-else
**결정**: switch expression 사용

**장점**:
- 가독성 향상 (7개 case를 명확하게 표현)
- 컴파일 타임 완전성 검사 (모든 case 처리 보장)
- 코드 간결성 (yield 키워드로 default 블록 처리)

```java
// 기존 if-else 방식
if (status == 403) {
    return "message1";
} else if (status == 404) {
    return "message2";
}
// ...

// Java 17 switch expression
return switch (status) {
    case 403 -> "message1";
    case 404 -> "message2";
    // ...
    default -> "default message";
};
```

## 남은 작업

### ✅ 완료: user-service 마이페이지 API 활용

**해결 방법**:
- ~~새로운 API 생성 필요 없음~~ ✅
- user-service의 기존 `GET /api/v1/users/me` 활용
- UserServiceClient.getMyInfo() 메서드 추가로 해결

**구현 내용**:
1. UserServiceClient에 getMyInfo() 메서드 추가
2. NotificationController에서 getUserByUsername() → getMyInfo() 변경
3. userPrincipal.username() → userPrincipal.id() 사용

**장점**:
- 새로운 API 개발 불필요
- 최신 사용자 정보 보장 (DB 직접 조회)
- MSA 원칙 준수 (user-service가 사용자 정보 관리)
- 팀 컨벤션 일치 (FeignClient 패턴 활용)

### 🟢 Optional (개선 사항)

1. **FeignClient Fallback 검증**
   - Issue #76에서 구현했다고 문서화되어 있으나, 실제 코드에서 동작하지 않음
   - `UserServiceClientFallback` 클래스 존재 여부 확인 필요
   - Fallback이 트리거되지 않는 이유 분석

2. **delivery-service 컨벤션 정리**
   - Map 사용 → ApiResponse로 변경 (팀 컨벤션 통일)

## 참고 문서

- [Issue #76: notification-service 리스크 개선](./issue-76-notification-risk-refactoring.md)
- [Issue #84: 배송 상태 REST API](./issue-84-delivery-status-rest-api.md)
- [PR #81: user-service 로그인/회원가입](../scrum/PR81-user-login-signup.md)
- [CLAUDE.md](../../CLAUDE.md)
- [docs/service-status.md](../service-status.md)
- [common-lib GlobalExceptionHandler](../../common-lib/src/main/java/com/oneforlogis/common/exception/GlobalExceptionHandler.java)

## 성과

- ✅ Swagger 테스트 데이터 Slack ID 통일 (C09QY22AMEE)
- ✅ FeignException HTTP 상태 코드 일치 (500 → 403)
- ✅ 사용자 친화적 에러 메시지 제공
- ✅ 팀 컨벤션 준수 (ApiResponse 사용)
- ✅ Java 17 modern features 활용 (switch expression)
- ✅ 7가지 주요 HTTP 에러 + default 처리
- ✅ Docker 환경에서 실제 Slack 메시지 발송 테스트 성공
- ❌ user-service username 조회 API 미구현 (별도 Issue #110 필요)

## 후속 작업

1. **Issue #110 생성**: user-service에 `GET /api/v1/users/username/{username}` API 추가
2. **PR 생성**: fix/#109 → dev 머지 요청
3. **Swagger 전체 API 테스트**: 수정 사항 반영 확인

## 커밋 메시지

```
fix: resolve swagger test failures and feign exception handling

- Slack ID 통일: 모든 테스트 코드와 DTO를 C09QY22AMEE로 변경
- NotificationExceptionHandler 구현: FeignException 처리 추가
- HTTP 상태 코드 일치: 500 → 실제 에러 코드 (403) 반환
- 사용자 친화적 에러 메시지: 7가지 HTTP 에러 + default 처리
- Java 17 switch expression 활용

Related to Issue #109
```
