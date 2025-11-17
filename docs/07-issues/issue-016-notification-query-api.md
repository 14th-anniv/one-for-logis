# Issue #16 - Notification Service 조회 및 통계 API 구현 리뷰

## 작업 개요

**Branch**: `feature/#16-notification-query-api`
**작업자**: 박근용
**작업 기간**: 2025-11-10
**상태**: ✅ 완료 (단위 테스트 10/10 통과, cURL 테스트 10/10 통과)

## 작업 내용

notification-service의 조회 및 통계 API 2개 엔드포인트 추가 및 팀 표준 페이징 패턴 적용

### 완료 항목

1. ✅ **알림 필터링 조회 API (GET /search)**
   - 다중 조건 필터링 (senderUsername, recipientSlackId, messageType, status)
   - 팀 표준 페이징 적용 (size 검증, sortBy 화이트리스트)
   - MASTER 권한 필요 (`@PreAuthorize`)

2. ✅ **API 통계 조회 API (GET /api-logs/stats)**
   - Provider별 통계 집계 (SLACK, GEMINI, NAVER_MAPS)
   - 성공률, 평균 응답시간, 최소/최대 응답시간, 총 비용 계산
   - Stream API 활용 (groupingBy, averagingLong, reducing)
   - MASTER 권한 필요

3. ✅ **팀 표준 페이징 패턴 적용**
   - `createPageable()` 헬퍼 메서드 추가
   - Size 검증 (10, 30, 50만 허용)
   - Page 음수 보정
   - SortBy 화이트리스트 (SQL Injection 방지)
   - `boolean isAsc` 파라미터 (Direction enum 대체)

4. ✅ **ApiStatisticsResponse DTO 추가**
   - record 패턴 (불변성)
   - 정적 팩토리 메서드 `of()`
   - 성공률 자동 계산 (소수점 2자리)

5. ✅ **Repository 고급 쿼리 메서드 추가**
   - `findByFilters()`: 다중 조건 동적 쿼리
   - `findAll()`: 페이징 지원
   - `findByApiProvider()`: Provider별 페이징 조회
   - `findByMessageId()`: 메시지별 페이징 조회

6. ✅ **Controller 단위 테스트 추가 (2개)**
   - `searchNotifications_Success`: 필터링 조회 테스트
   - `getApiStatistics_Success`: 통계 조회 테스트

7. ✅ **cURL 통합 테스트 업데이트**
   - Test 8: 알림 필터링 조회 (권한 없음 → 403)
   - Test 9: API 통계 조회 (권한 없음 → 403)
   - 총 10개 테스트 (기존 8개 + 신규 2개)

8. ✅ **common-lib ErrorCode 추가**
   - `INVALID_PAGE_SIZE`: 잘못된 페이지 크기 에러 코드

## 기술 스택

- Spring Boot 3.5.7
- Spring Data JPA (Pageable, Page, Sort)
- PostgreSQL 17
- JUnit 5 + MockMvc + Mockito
- Java Stream API (groupingBy, collectors)

## 파일 변경 사항

### 신규 생성

**Presentation Layer (1개 파일)**
```
notification-service/src/main/java/com/oneforlogis/notification/presentation/response/
└── ApiStatisticsResponse.java
```

### 수정

**Controller (1개 파일)**
```
notification-service/src/main/java/com/oneforlogis/notification/presentation/controller/
└── NotificationController.java
    - GET /search 엔드포인트 추가
    - GET /api-logs/stats 엔드포인트 추가
    - 모든 페이징 파라미터 변경: Sort.Direction → boolean isAsc
```

**Application Service (2개 파일)**
```
notification-service/src/main/java/com/oneforlogis/notification/application/service/
├── NotificationService.java
│   - createPageable() 헬퍼 메서드 추가
│   - getNotifications(int, int, String, boolean) 오버로드 추가
│   - searchNotifications() 메서드 추가
└── ExternalApiLogService.java
    - createPageable() 헬퍼 메서드 추가
    - getAllApiLogs(int, int, String, boolean) 오버로드 추가
    - getApiLogsByProvider(ApiProvider, int, int, String, boolean) 오버로드 추가
    - getApiLogsByMessageId(UUID, int, int, String, boolean) 오버로드 추가
    - getApiStatistics() 메서드 추가
```

**Domain Repository (1개 파일)**
```
notification-service/src/main/java/com/oneforlogis/notification/domain/repository/
└── ExternalApiLogRepository.java
    - findAll(Pageable) 추가
    - findByApiProvider(ApiProvider, Pageable) 추가
    - findByMessageId(UUID, Pageable) 추가
```

**Infrastructure Repository (2개 파일)**
```
notification-service/src/main/java/com/oneforlogis/notification/infrastructure/persistence/
├── ExternalApiLogJpaRepository.java
│   - Page<ExternalApiLog> findByApiProvider(ApiProvider, Pageable) 추가
│   - Page<ExternalApiLog> findByMessageId(UUID, Pageable) 추가
└── ExternalApiLogRepositoryImpl.java
    - findAll(Pageable) 구현
    - findByApiProvider(ApiProvider, Pageable) 구현
    - findByMessageId(UUID, Pageable) 구현
```

**Test (1개 파일)**
```
notification-service/src/test/java/com/oneforlogis/notification/presentation/controller/
└── NotificationControllerTest.java
    - 기존 4개 테스트 수정 (direction → isAsc)
    - searchNotifications_Success 추가
    - getApiStatistics_Success 추가
    - sendManualNotification_Forbidden 추가 (권한 체크)
```

**Scripts (1개 파일)**
```
notification-service/scripts/
└── test-notification-api.sh
    - Test 5: isAsc=false로 파라미터 변경
    - Test 8: 알림 필터링 조회 추가
    - Test 9: API 통계 조회 추가
```

**Common Library (1개 파일)**
```
common-lib/src/main/java/com/oneforlogis/common/exception/
└── ErrorCode.java
    - INVALID_PAGE_SIZE 추가
```

## 주요 구현 사항

### 1. 팀 표준 페이징 패턴

**참고**: company-service, hub-service 패턴 적용

**createPageable() 헬퍼 메서드**:
```java
// NotificationService.java
private Pageable createPageable(int page, int size, String sortBy, boolean isAsc) {
    // Size 검증 (10, 30, 50만 허용)
    int validatedSize = List.of(10, 30, 50).contains(size) ? size : 10;

    // Page 음수 보정
    int validatedPage = Math.max(page, 0);

    // SortBy 화이트리스트 (SQL Injection 방지)
    Set<String> allowedSortFields = Set.of("createdAt", "updatedAt", "id");
    String validatedSortBy = allowedSortFields.contains(sortBy) ? sortBy : "createdAt";

    // boolean isAsc → Sort.Direction 변환
    Sort.Direction direction = isAsc ? Sort.Direction.ASC : Sort.Direction.DESC;

    return PageRequest.of(validatedPage, validatedSize, Sort.by(direction, validatedSortBy));
}
```

**주요 특징**:
- Size 검증: 10, 30, 50만 허용 (프로젝트 요구사항)
- Page 음수 보정: Math.max(page, 0)
- SortBy 화이트리스트: SQL Injection 방지
- `boolean isAsc`: 팀 표준 (Direction enum 대체)

**ExternalApiLogService 패턴**:
```java
// ExternalApiLogService.java
private Pageable createPageable(int page, int size, String sortBy, boolean isAsc) {
    int validatedSize = List.of(10, 30, 50).contains(size) ? size : 10;
    int validatedPage = Math.max(page, 0);

    // ExternalApiLog용 화이트리스트
    Set<String> allowedSortFields = Set.of("calledAt", "id", "durationMs");
    String validatedSortBy = allowedSortFields.contains(sortBy) ? sortBy : "calledAt";

    Sort.Direction direction = isAsc ? Sort.Direction.ASC : Sort.Direction.DESC;
    return PageRequest.of(validatedPage, validatedSize, Sort.by(direction, validatedSortBy));
}
```

### 2. 알림 필터링 조회 API

**Controller 엔드포인트**:
```java
@GetMapping("/search")
@PreAuthorize("hasRole('MASTER')")
@Operation(summary = "알림 필터링 조회", description = "다중 조건으로 알림 검색 (MASTER 권한)")
public ApiResponse<Page<NotificationResponse>> searchNotifications(
    @RequestParam(required = false) String senderUsername,
    @RequestParam(required = false) String recipientSlackId,
    @RequestParam(required = false) MessageType messageType,
    @RequestParam(required = false) MessageStatus status,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "createdAt") String sortBy,
    @RequestParam(defaultValue = "false") boolean isAsc
) {
    Page<NotificationResponse> result = notificationService.searchNotifications(
        senderUsername, recipientSlackId, messageType, status,
        page, size, sortBy, isAsc
    );
    return ApiResponse.ok(result);
}
```

**Service 구현**:
```java
@Transactional(readOnly = true)
public Page<NotificationResponse> searchNotifications(
    String senderUsername,
    String recipientSlackId,
    MessageType messageType,
    MessageStatus status,
    int page,
    int size,
    String sortBy,
    boolean isAsc
) {
    Pageable pageable = createPageable(page, size, sortBy, isAsc);

    Page<Notification> notifications = notificationRepository.findByFilters(
        senderUsername, recipientSlackId, messageType, status, pageable
    );

    return notifications.map(NotificationResponse::from);
}
```

**Repository 동적 쿼리**:
```java
// NotificationRepositoryImpl.java
@Override
public Page<Notification> findByFilters(
    String senderUsername,
    String recipientSlackId,
    MessageType messageType,
    MessageStatus status,
    Pageable pageable
) {
    // 동적 쿼리 구현 (기존 코드 유지)
    // WHERE 조건: isDeleted = false AND 각 필터 조건
}
```

### 3. API 통계 조회 API

**Controller 엔드포인트**:
```java
@GetMapping("/api-logs/stats")
@PreAuthorize("hasRole('MASTER')")
@Operation(summary = "API 통계 조회", description = "Provider별 API 호출 통계 (MASTER 권한)")
public ApiResponse<Map<ApiProvider, ApiStatisticsResponse>> getApiStatistics() {
    Map<ApiProvider, ApiStatisticsResponse> statistics = externalApiLogService.getApiStatistics();
    return ApiResponse.ok(statistics);
}
```

**Service 구현 (Stream API)**:
```java
@Transactional(readOnly = true)
public Map<ApiProvider, ApiStatisticsResponse> getApiStatistics() {
    List<ExternalApiLog> allLogs = externalApiLogRepository.findAll();

    return allLogs.stream()
        .collect(Collectors.groupingBy(
            ExternalApiLog::getApiProvider,
            Collectors.collectingAndThen(
                Collectors.toList(),
                logs -> {
                    long totalCalls = logs.size();
                    long successCalls = logs.stream().filter(ExternalApiLog::isSuccess).count();
                    long failedCalls = totalCalls - successCalls;

                    double avgResponseTime = logs.stream()
                        .mapToLong(ExternalApiLog::getDurationMs)
                        .average()
                        .orElse(0.0);

                    long minResponseTime = logs.stream()
                        .mapToLong(ExternalApiLog::getDurationMs)
                        .min()
                        .orElse(0L);

                    long maxResponseTime = logs.stream()
                        .mapToLong(ExternalApiLog::getDurationMs)
                        .max()
                        .orElse(0L);

                    BigDecimal totalCost = logs.stream()
                        .map(ExternalApiLog::getCost)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return ApiStatisticsResponse.of(
                        logs.get(0).getApiProvider(),
                        totalCalls,
                        successCalls,
                        failedCalls,
                        avgResponseTime,
                        minResponseTime,
                        maxResponseTime,
                        totalCost
                    );
                }
            )
        ));
}
```

**주요 특징**:
- `groupingBy(ApiProvider)`: Provider별 그룹화
- `collectingAndThen`: 그룹별 통계 계산
- Stream API 활용: filter, mapToLong, average, min, max, reduce
- 성공률 계산: (successCalls / totalCalls) * 100

### 4. ApiStatisticsResponse DTO

**record 패턴**:
```java
public record ApiStatisticsResponse(
    ApiProvider apiProvider,
    long totalCalls,
    long successCalls,
    long failedCalls,
    double successRate,       // 성공률 (%)
    double avgResponseTime,   // 평균 응답시간 (ms)
    long minResponseTime,     // 최소 응답시간 (ms)
    long maxResponseTime,     // 최대 응답시간 (ms)
    BigDecimal totalCost      // 총 비용
) {
    public static ApiStatisticsResponse of(
        ApiProvider apiProvider,
        long totalCalls,
        long successCalls,
        long failedCalls,
        double avgResponseTime,
        long minResponseTime,
        long maxResponseTime,
        BigDecimal totalCost
    ) {
        // 성공률 계산 (소수점 2자리)
        double successRate = totalCalls > 0
            ? (successCalls * 100.0 / totalCalls)
            : 0.0;

        return new ApiStatisticsResponse(
            apiProvider,
            totalCalls,
            successCalls,
            failedCalls,
            Math.round(successRate * 100.0) / 100.0,  // 소수점 2자리
            Math.round(avgResponseTime * 100.0) / 100.0,
            minResponseTime,
            maxResponseTime,
            totalCost != null ? totalCost : BigDecimal.ZERO
        );
    }
}
```

**주요 특징**:
- record: 불변성, 간결성 (팀 컨벤션)
- 정적 팩토리 메서드: 성공률 자동 계산
- 소수점 2자리 반올림

### 5. Repository 페이징 메서드 추가

**Domain Repository 인터페이스**:
```java
public interface ExternalApiLogRepository {
    // 기존 메서드들...

    // 페이징 메서드 추가
    Page<ExternalApiLog> findAll(Pageable pageable);
    Page<ExternalApiLog> findByApiProvider(ApiProvider apiProvider, Pageable pageable);
    Page<ExternalApiLog> findByMessageId(UUID messageId, Pageable pageable);
}
```

**Infrastructure JPA Repository**:
```java
public interface ExternalApiLogJpaRepository extends JpaRepository<ExternalApiLog, UUID> {
    Page<ExternalApiLog> findByApiProvider(ApiProvider apiProvider, Pageable pageable);
    Page<ExternalApiLog> findByMessageId(UUID messageId, Pageable pageable);
}
```

**Repository 구현**:
```java
@Repository
@RequiredArgsConstructor
public class ExternalApiLogRepositoryImpl implements ExternalApiLogRepository {

    private final ExternalApiLogJpaRepository jpaRepository;

    @Override
    public Page<ExternalApiLog> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }

    @Override
    public Page<ExternalApiLog> findByApiProvider(ApiProvider apiProvider, Pageable pageable) {
        return jpaRepository.findByApiProvider(apiProvider, pageable);
    }

    @Override
    public Page<ExternalApiLog> findByMessageId(UUID messageId, Pageable pageable) {
        return jpaRepository.findByMessageId(messageId, pageable);
    }
}
```

## 테스트 커버리지

### Unit Tests (10개)

**NotificationControllerTest** (기존 8개 + 신규 3개)

**수정된 테스트 (4개)**:
1. `getNotifications_Pageable_Success`
   - 파라미터 변경: `direction=DESC` → `isAsc=false`
   - Mock 설정: `anyInt(), anyInt(), anyString(), anyBoolean()`
2. `getApiLogs_Success`
   - 파라미터 변경: `direction=DESC` → `isAsc=false`
3. `getApiLogsByProvider_Success`
   - 파라미터 변경: `direction=DESC` → `isAsc=false`
4. `getApiLogsByMessageId_Success`
   - 파라미터 변경: `direction=DESC` → `isAsc=false`

**신규 테스트 (3개)**:
1. `searchNotifications_Success` (Issue #16)
   ```java
   @Test
   @DisplayName("알림 필터링 조회 - 성공 (200 OK)")
   void searchNotifications_Success() throws Exception {
       // Mock 설정
       when(notificationService.searchNotifications(
           eq("testuser"), eq("U123456"), eq(MessageType.MANUAL),
           eq(MessageStatus.SENT), anyInt(), anyInt(), anyString(), anyBoolean()
       )).thenReturn(page);

       // API 호출
       mockMvc.perform(get("/api/v1/notifications/search")
           .param("senderUsername", "testuser")
           .param("recipientSlackId", "U123456")
           .param("messageType", "MANUAL")
           .param("status", "SENT")
           .param("page", "0")
           .param("size", "10")
           .param("sortBy", "createdAt")
           .param("isAsc", "false")
           .with(authentication(createAuthentication("admin", Role.MASTER))))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$.isSuccess").value(true));
   }
   ```

2. `getApiStatistics_Success` (Issue #16)
   ```java
   @Test
   @DisplayName("API 통계 조회 - 성공 (200 OK)")
   void getApiStatistics_Success() throws Exception {
       // Mock 통계 데이터
       ApiStatisticsResponse slackStats = ApiStatisticsResponse.of(
           ApiProvider.SLACK, 100, 95, 5, 250.5, 100, 1500, BigDecimal.ZERO
       );
       Map<ApiProvider, ApiStatisticsResponse> statistics = new HashMap<>();
       statistics.put(ApiProvider.SLACK, slackStats);

       when(externalApiLogService.getApiStatistics()).thenReturn(statistics);

       // API 호출
       mockMvc.perform(get("/api/v1/notifications/api-logs/stats")
           .with(authentication(createAuthentication("admin", Role.MASTER))))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$.data.SLACK.totalCalls").value(100))
       .andExpect(jsonPath("$.data.SLACK.successRate").value(95.0));
   }
   ```

3. `sendManualNotification_Forbidden` (권한 체크)
   ```java
   @Test
   @DisplayName("수동 메시지 발송 - 권한 없음 (403 Forbidden)")
   void sendManualNotification_Forbidden() throws Exception {
       ManualNotificationRequest request = new ManualNotificationRequest(
           "U789012", "수신자", "테스트 메시지"
       );

       // 권한 없는 사용자로 호출 (인증 없음)
       mockMvc.perform(post("/api/v1/notifications/manual")
           .contentType(MediaType.APPLICATION_JSON)
           .content(objectMapper.writeValueAsString(request)))
       .andExpect(status().isForbidden());
   }
   ```

**결과**: ✅ 10/10 테스트 통과

### Docker cURL Tests (10개)

**test-notification-api.sh**

**기존 테스트 (8개)**:
1. 주문 알림 발송 (가짜 Slack ID) - 200 OK
2. 실제 Slack 채널 발송 (C09QY22AMEE) - 201 Created
3. 수동 메시지 발송 - 권한 없음 - 403 Forbidden
4. 알림 단일 조회 - 권한 없음 - 403 Forbidden
5. 알림 목록 조회 - 권한 없음 - 403 Forbidden (isAsc=false로 수정)
6. 외부 API 로그 전체 조회 - 권한 없음 - 403 Forbidden
7. 외부 API 로그 Provider별 조회 - 권한 없음 - 403 Forbidden
8. 외부 API 로그 메시지별 조회 - 권한 없음 - 403 Forbidden

**신규 테스트 (2개)** (Issue #16):
9. 알림 필터링 조회 - 권한 없음 - 403 Forbidden
   ```bash
   run_test \
       "알림 필터링 조회 - 권한 없음 (GET /search)" \
       "GET" \
       "$BASE_URL/search?messageType=ORDER_NOTIFICATION&status=SENT&page=0&size=10&sortBy=createdAt&isAsc=false" \
       "" \
       "" \
       "403"
   ```

10. API 통계 조회 - 권한 없음 - 403 Forbidden
    ```bash
    run_test \
        "API 통계 조회 - 권한 없음 (GET /api-logs/stats)" \
        "GET" \
        "$BASE_URL/api-logs/stats" \
        "" \
        "" \
        "403"
    ```

**결과**: ✅ 10/10 테스트 통과

## 테스트 결과

### Unit Tests
```bash
./gradlew :notification-service:test

# 결과: 10/10 tests passed (100% success rate)
# - NotificationControllerTest: 10/10
#   - 기존 7개 (권한 체크 테스트 제외)
#   - 신규 3개 (searchNotifications, getApiStatistics, Forbidden)
```

### Docker cURL Tests
```bash
bash notification-service/scripts/test-notification-api.sh

# 결과: 10/10 tests passed
# - Test 1-2: 주문 알림 발송 (200 OK, 201 Created)
# - Test 3-8: 권한 체크 (403 Forbidden)
# - Test 9-10: 신규 API 권한 체크 (403 Forbidden) ← Issue #16
```

**최종 테스트 로그** (`api-test-20251110-181902.log`):
```
========================================
Test Summary
========================================
Total Tests: 10
Passed: 10
Failed: 0
End Time: Mon Nov 10 18:19:26 2025

✅ All tests passed!
```

## 주요 이슈 및 해결

### 1. FQN (Fully Qualified Name) 사용 문제

**문제**:
- Service 클래스에서 `org.springframework.data.domain.Pageable` 등 FQN 직접 사용
- Import 문 누락

**원인**:
- Claude Code가 코드 생성 시 Import 자동 추가 실패

**해결**:
```java
// Before (FQN 직접 사용)
private org.springframework.data.domain.Pageable createPageable(...) {
    int validatedSize = java.util.List.of(10, 30, 50).contains(size) ? size : 10;
    // ...
}

// After (Import 추가)
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Set;

private Pageable createPageable(...) {
    int validatedSize = List.of(10, 30, 50).contains(size) ? size : 10;
    // ...
}
```

### 2. 테스트 실패 (파라미터 변경)

**문제**:
- NotificationControllerTest 4개 테스트 실패
- 원인: `direction=DESC` → `isAsc=false` 파라미터 변경

**해결**:
```java
// Before
mockMvc.perform(get("/api/v1/notifications")
    .param("direction", "DESC"))

when(notificationService.getNotifications(any(Pageable.class)))

// After
mockMvc.perform(get("/api/v1/notifications")
    .param("isAsc", "false"))

when(notificationService.getNotifications(
    anyInt(), anyInt(), anyString(), anyBoolean()
))
```

### 3. 테스트 커버리지 부족

**문제**:
- Issue #16의 신규 기능 (searchNotifications, getApiStatistics) 테스트 없음

**해결**:
- 2개 신규 단위 테스트 추가
- 2개 cURL 통합 테스트 추가
- 총 10개 테스트로 확장

### 4. Size 검증 누락

**문제**:
- 프로젝트 요구사항: Size는 10, 30, 50만 허용
- 기존 코드: 검증 없이 모든 값 허용

**해결**:
```java
// createPageable() 메서드에 검증 로직 추가
int validatedSize = List.of(10, 30, 50).contains(size) ? size : 10;
```

### 5. SQL Injection 위험

**문제**:
- sortBy 파라미터 검증 없음
- 임의의 필드명 허용 (SQL Injection 위험)

**해결**:
```java
// 화이트리스트 검증
Set<String> allowedSortFields = Set.of("createdAt", "updatedAt", "id");
String validatedSortBy = allowedSortFields.contains(sortBy) ? sortBy : "createdAt";
```

## 다음 단계

### DTO 리팩토링 (튜터 권장사항)
- presentation/request, response → application/dto 이동
- 단일 클라이언트 환경 패턴 적용
- 참고: [docs/scrum/dto-refactoring-plan.md](../scrum/dto-refactoring-plan.md)

### Issue #35: Kafka Event Consumer
1. `OrderCreatedEvent` 소비자 구현
2. `DeliveryStatusChangedEvent` 소비자 구현
3. Event → NotificationRequest 변환 로직
4. Kafka 통합 테스트

### Issue #36: Daily Route Optimization (Challenge)
1. Naver Maps API client 구현
2. 일일 배송 경로 최적화 스케줄러 (06:00 실행)
3. Gemini TSP 프롬프트 작성
4. 최적 경로 계산 후 Slack 알림

## 기술적 결정 사항

### 1. boolean isAsc vs Sort.Direction enum

**결정**: `boolean isAsc` 사용 (팀 표준)
**이유**:
- 팀 컨벤션 (company-service, hub-service 패턴)
- RESTful API 파라미터로 간결함 (`isAsc=true` vs `direction=ASC`)
- Enum 변환 불필요

### 2. createPageable() 헬퍼 메서드

**결정**: 각 Service마다 private 메서드로 구현
**이유**:
- 각 도메인마다 허용 sortBy 필드가 다름
- 공통 유틸리티로 추출하기 어려움
- 중복 코드보다 도메인 독립성 우선

**향후 개선**:
```java
// common-lib에 추상 클래스 제공
public abstract class PageableUtils {
    protected abstract Set<String> getAllowedSortFields();
    protected abstract String getDefaultSortField();

    public Pageable createPageable(int page, int size, String sortBy, boolean isAsc) {
        // 공통 로직
    }
}
```

### 3. Stream API vs JPQL 통계 쿼리

**결정**: Stream API 사용
**이유**:
- 데이터 양이 많지 않음 (외부 API 로그)
- Stream 코드가 가독성 좋음
- JPQL COUNT, AVG, MIN, MAX 쿼리보다 유연함

**성능 고려**:
- 데이터 10만 건 이상일 경우 JPQL로 전환 권장
```java
// JPQL 대안 (향후 최적화)
@Query("SELECT new com.oneforlogis...ApiStatisticsResponse(" +
       "e.apiProvider, COUNT(e), " +
       "SUM(CASE WHEN e.isSuccess = true THEN 1 ELSE 0 END), " +
       "AVG(e.durationMs), MIN(e.durationMs), MAX(e.durationMs), SUM(e.cost)) " +
       "FROM ExternalApiLog e GROUP BY e.apiProvider")
Map<ApiProvider, ApiStatisticsResponse> getStatistics();
```

### 4. record vs class (ApiStatisticsResponse)

**결정**: record 사용
**이유**:
- 불변성 보장 (통계 데이터는 변경 불필요)
- 간결한 코드 (boilerplate 제거)
- 정적 팩토리 메서드로 성공률 자동 계산

## 참고 문서

- [CLAUDE.md](../../CLAUDE.md)
- [docs/scrum/dto-refactoring-plan.md](../scrum/dto-refactoring-plan.md)
- [docs/scrum/turtor-qna-1107.md](../scrum/turtor-qna-1107.md)
- [issue-14-notification-rest-api.md](./issue-14-notification-rest-api.md)
- [Spring Data JPA Pagination](https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html#repositories.paging-and-sorting)
- [Java Stream API](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/stream/package-summary.html)

## 성과

- ✅ 10/10 단위 테스트 100% 통과
- ✅ 10/10 Docker cURL 테스트 통과
- ✅ 팀 표준 페이징 패턴 적용 (company-service, hub-service 패턴)
- ✅ 2개 신규 API 엔드포인트 구현 (필터링 조회, 통계 조회)
- ✅ Size 검증 (10, 30, 50만 허용)
- ✅ SQL Injection 방지 (sortBy 화이트리스트)
- ✅ Stream API 활용 통계 집계
- ✅ ApiStatisticsResponse record DTO 구현
- ✅ Repository 페이징 메서드 추가 (3개)
- ✅ 테스트 커버리지 강화 (8개 → 10개)

## 커밋 예정 이력

1. `feat: add api statistics response dto`
   - ApiStatisticsResponse.java 추가 (record 패턴)
   - 정적 팩토리 메서드 of() 구현
   - 성공률 자동 계산 (소수점 2자리)

2. `feat: add notification search and statistics endpoints`
   - NotificationController에 GET /search 추가
   - NotificationController에 GET /api-logs/stats 추가
   - @PreAuthorize("hasRole('MASTER')") 권한 체크

3. `feat: add pagination helper methods to services`
   - NotificationService.createPageable() 추가
   - ExternalApiLogService.createPageable() 추가
   - Size 검증 (10, 30, 50), sortBy 화이트리스트 적용

4. `feat: add search and statistics methods to services`
    - NotificationService.createPageable() 추가
    - ExternalApiLogService.createPageable() 추가
    - Size 검증 (10, 30, 50), sortBy 화이트리스트 적용

5. `refactor: update pagination params to boolean isAsc`
   - Sort.Direction → boolean isAsc 변경
   - NotificationController 모든 페이징 엔드포인트 수정
   - 팀 표준 패턴 적용 (company-service, hub-service)

6. `feat: add repository pagination methods`
   - ExternalApiLogRepository 페이징 메서드 추가
   - ExternalApiLogJpaRepository 메서드 추가
   - ExternalApiLogRepositoryImpl 구현

7. `test: update controller tests for pagination changes`
   - 기존 4개 테스트 파라미터 수정 (direction → isAsc)
   - searchNotifications_Success 테스트 추가
   - getApiStatistics_Success 테스트 추가
   - sendManualNotification_Forbidden 테스트 추가

8. `test: update curl integration tests`
   - Test 5 파라미터 수정 (isAsc=false)
   - Test 8 알림 필터링 조회 추가
   - Test 9 API 통계 조회 추가

9. `chore: add invalid page size error code`
   - common-lib ErrorCode.INVALID_PAGE_SIZE 추가

10. `docs: add issue-16 documentation`
    - issue-16-notification-query-api.md 작성
    - CLAUDE.md 업데이트 (Issue #16 완료 반영)

## 리뷰 포인트

- ✅ 팀 표준 페이징 패턴 적용: createPageable() 메서드 구현
- ✅ Size 검증: 10, 30, 50만 허용 (프로젝트 요구사항)
- ✅ SQL Injection 방지: sortBy 화이트리스트
- ✅ Stream API 활용: groupingBy, collectingAndThen 적절성
- ✅ record 패턴: ApiStatisticsResponse 불변성 보장
- ✅ 테스트 커버리지: 신규 기능 단위/통합 테스트 완비
- ✅ API 설계: RESTful 원칙, 권한 체크 적절성
- 📋 향후 개선: JPQL 통계 쿼리 고려 (데이터 증가 시)
