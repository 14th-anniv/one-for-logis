# Issue #13 - External API Client Implementation 리뷰

## 작업 개요

**Branch**: `feature/#13-external-api-client`
**작업자**: 박근용
**작업 기간**: 2025-11-06 09:00~18:00
**상태**: ✅ 완료 (테스트 100% 통과)

## 작업 내용

notification-service의 외부 API 클라이언트(Slack, Gemini) 구현 및 자동 로깅 인프라 구축

### 완료 항목

1. ✅ **Slack API Client**
   - WebClient 기반 HTTP 클라이언트
   - `chat.postMessage` 엔드포인트 통합
   - Bearer Token 인증 방식
   - Resilience4j Retry (3회 재시도, 지수 백오프 1초 * 2^n)

2. ✅ **Gemini API Client**
   - Google Gemini API 통합 (ChatGPT 대체)
   - `gemini-2.5-flash-lite` 모델 사용
   - `x-goog-api-key` 헤더 인증
   - Resilience4j Retry (2회 재시도, 지수 백오프 2초 * 2^n)

3. ✅ **ApiLogDomainService**
   - 모든 외부 API 호출 자동 로깅
   - 민감 정보 마스킹 (token, api_key, authorization 등)
   - 실행 시간, HTTP 상태, 성공/실패 추적
   - ExternalApiLog 엔티티 자동 저장

4. ✅ **Wrapper Pattern**
   - `SlackClientWrapper`: Slack API 호출 래퍼 (자동 로깅)
   - `GeminiClientWrapper`: Gemini API 호출 래퍼 (자동 로깅)
   - Try-catch 에러 핸들링
   - 로깅 실패 시에도 원본 예외 전파

5. ✅ **WebClient Dependency Injection 리팩토링**
   - 기존: `WebClient.Builder` 주입 → 소스코드에서 baseUrl 하드코딩
   - 개선: `WebClient` 주입 → Config에서 baseUrl 설정
   - 이유: 단위 테스트에서 MockWebServer URL 주입 가능

6. ✅ **ExternalApiConfig**
   - `slackWebClient` Bean 등록 (baseUrl: https://slack.com/api)
   - `geminiWebClient` Bean 등록 (baseUrl: https://generativelanguage.googleapis.com/v1beta)
   - `slackRetry` Bean 등록 (Resilience4j RetryConfig)
   - `geminiRetry` Bean 등록 (Resilience4j RetryConfig)

7. ✅ **Unit Tests (MockWebServer)**
   - `SlackApiClientTest` (3개 테스트)
   - `GeminiApiClientTest` (3개 테스트)
   - MockWebServer로 HTTP 응답 모킹
   - RecordedRequest로 요청 검증

8. ✅ **Integration Tests (Real API)**
   - `SlackApiAuthIntegrationTest` (1개 테스트)
   - `GeminiApiKeyIntegrationTest` (2개 테스트)
   - 실제 API 키 검증
   - `.env` 파일에서 API 키 로드

9. ✅ **환경 설정**
   - `application.yml`: Slack, Gemini API 키 환경변수 바인딩
   - `application-test.yml`: 테스트용 설정 (H2 DB, 테스트 API 키)
   - `.env.example`: API 키 예시

10. ✅ **ChatGPT 코드 제거**
    - `ChatGptApiClient.java`: 주석처리 (향후 참고용)
    - `ChatGptClientWrapper.java`: 주석처리
    - `ChatGptApiClientTest.java`: 주석처리
    - `ApiProvider.CHATGPT` → `ApiProvider.GEMINI`로 변경

## 기술 스택

- Spring Boot 3.5.7
- Spring WebFlux (WebClient)
- Resilience4j (Retry with Exponential Backoff)
- OkHttp MockWebServer (테스트)
- Jackson ObjectMapper (JSON 처리)
- Lombok
- JUnit 5 + AssertJ

## 파일 변경 사항

### 신규 생성

**Infrastructure - External API Clients (10개 파일)**
```
notification-service/src/main/java/com/oneforlogis/notification/infrastructure/client/
├── slack/
│   ├── SlackApiClient.java
│   ├── SlackMessageRequest.java
│   └── SlackMessageResponse.java
├── gemini/
│   ├── GeminiApiClient.java
│   ├── GeminiRequest.java
│   ├── GeminiResponse.java
│   └── GeminiContent.java
├── SlackClientWrapper.java
├── GeminiClientWrapper.java
└── (ChatGptApiClient.java - 주석처리)
```

**Domain Service (1개 파일)**
```
notification-service/src/main/java/com/oneforlogis/notification/domain/service/
└── ApiLogDomainService.java
```

**Infrastructure Config (1개 파일)**
```
notification-service/src/main/java/com/oneforlogis/notification/infrastructure/config/
└── ExternalApiConfig.java
```

**Unit Tests (2개 파일)**
```
notification-service/src/test/java/com/oneforlogis/notification/infrastructure/client/
├── slack/SlackApiClientTest.java
└── gemini/GeminiApiClientTest.java
```

**Integration Tests (2개 파일)**
```
notification-service/src/test/java/com/oneforlogis/notification/infrastructure/client/
├── slack/SlackApiAuthIntegrationTest.java
└── gemini/GeminiApiKeyIntegrationTest.java
```

### 수정

- `ExternalApiLog.java`: Builder 생성자 추가 (모든 필드 지원)
- `application.yml`: Slack, Gemini API 키 환경변수 추가
- `application-test.yml`: 테스트용 API 키 설정 추가
- `.env.example`: API 키 예시 추가

## 주요 구현 사항

### 1. Slack API Client

**구현 방식**:
```java
@RequiredArgsConstructor
public class SlackApiClient {
    private final WebClient slackWebClient;  // Config에서 baseUrl 설정
    private final Retry slackRetry;

    @Value("${external-api.slack.bot-token}")
    private String botToken;

    public SlackMessageResponse postMessage(SlackMessageRequest request) {
        return Retry.decorateSupplier(slackRetry, () ->
            slackWebClient.post()
                .uri("/chat.postMessage")
                .header("Authorization", "Bearer " + botToken)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SlackMessageResponse.class)
                .block()
        ).get();
    }
}
```

**주요 특징**:
- WebClient 주입 받아 baseUrl 변경 불가 → 테스트 가능
- Retry 데코레이터로 재시도 로직 적용
- Bearer Token 인증
- 동기식 호출 (`.block()`)

### 2. Gemini API Client

**구현 방식**:
```java
@RequiredArgsConstructor
public class GeminiApiClient {
    private static final String MODEL = "gemini-2.5-flash-lite";
    private final WebClient geminiWebClient;
    private final Retry geminiRetry;

    @Value("${external-api.gemini.api-key}")
    private String apiKey;

    public GeminiResponse generateContent(GeminiRequest request) {
        return Retry.decorateSupplier(geminiRetry, () ->
            geminiWebClient.post()
                .uri("/models/" + MODEL + ":generateContent")
                .header("x-goog-api-key", apiKey)  // 헤더 인증
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeminiResponse.class)
                .block()
        ).get();
    }
}
```

**주요 특징**:
- ChatGPT 대신 Gemini 사용 (무료 tier: 60 req/min)
- `x-goog-api-key` 헤더 인증 (쿼리 파라미터 아님!)
- `gemini-2.5-flash-lite` 모델 (최신 경량 모델)

### 3. ApiLogDomainService (자동 로깅)

**구현 방식**:
```java
public <T> T logApiCall(
    ApiProvider apiProvider,
    String apiMethod,
    Supplier<T> apiCall,
    Function<T, Map<String, Object>> responseExtractor
) {
    long startTime = System.currentTimeMillis();
    try {
        T response = apiCall.get();
        long duration = System.currentTimeMillis() - startTime;

        ExternalApiLog log = ExternalApiLog.builder()
            .apiProvider(apiProvider)
            .apiMethod(apiMethod)
            .requestData(maskSensitiveInfo(request))
            .responseData(maskSensitiveInfo(responseExtractor.apply(response)))
            .httpStatus(200)
            .isSuccess(true)
            .durationMs(duration)
            .calledAt(LocalDateTime.now())
            .build();

        externalApiLogRepository.save(log);
        return response;
    } catch (Exception e) {
        // 실패 로깅 후 예외 전파
    }
}
```

**민감 정보 마스킹**:
- `token`, `api_key`, `authorization`, `password` 필드 마스킹
- 정규표현식으로 JSON 내 패턴 탐지 및 `***MASKED***` 치환
- 원본 데이터는 메모리에서만 존재, DB에는 마스킹된 데이터 저장

### 4. Wrapper Pattern

**구현 방식**:
```java
@RequiredArgsConstructor
public class SlackClientWrapper {
    private final SlackApiClient slackApiClient;
    private final ApiLogDomainService apiLogDomainService;

    public SlackMessageResponse postMessage(SlackMessageRequest request) {
        return apiLogDomainService.logApiCall(
            ApiProvider.SLACK,
            "postMessage",
            () -> slackApiClient.postMessage(request),
            response -> objectMapper.convertValue(response, Map.class)
        );
    }
}
```

**효과**:
- Client 코드 수정 없이 로깅 기능 추가
- Service 계층에서는 Wrapper만 사용 → 자동 로깅
- 로깅 실패해도 원본 API 호출은 정상 진행

## 테스트 커버리지

### Unit Tests (6개)

**SlackApiClientTest (3개)**
1. Slack API 호출 성공 테스트
   - MockResponse로 성공 응답 모킹
   - RecordedRequest로 Authorization 헤더 검증
2. Slack API 호출 실패 테스트 (ok=false)
   - error 필드 검증
3. Slack API 네트워크 에러 테스트
   - 500 에러 응답 모킹
   - 예외 발생 확인

**GeminiApiClientTest (3개)**
1. Gemini API 호출 성공 테스트
   - candidates 배열 검증
   - RecordedRequest로 x-goog-api-key 헤더 검증
2. Gemini API 빈 응답 테스트
   - candidates가 비어있을 때 null 반환 확인
3. Gemini API 네트워크 에러 테스트
   - 500 에러 응답 모킹

### Integration Tests (3개)

**SlackApiAuthIntegrationTest (1개)**
1. Slack Bot Token 유효성 검증
   - `/auth.test` 엔드포인트 호출
   - ok=true, team 정보 반환 확인

**GeminiApiKeyIntegrationTest (2개)**
1. Gemini API Key 유효성 검증
   - 간단한 프롬프트 전송
   - candidates 응답 확인
2. Gemini API 계산 테스트
   - 배송 시한 계산 프롬프트 전송
   - 응답 내용 검증

## 테스트 결과

```bash
./gradlew :notification-service:test

# 결과: 35/35 tests passed (100% success rate)
# - NotificationRepositoryTest: 15/15
# - ExternalApiLogRepositoryTest: 11/11
# - SlackApiClientTest: 3/3
# - GeminiApiClientTest: 3/3
# - SlackApiAuthIntegrationTest: 1/1
# - GeminiApiKeyIntegrationTest: 2/2
```

## 주요 이슈 및 해결

### 1. WebClient.Builder vs WebClient 주입 문제

**문제**:
- 초기 구현: `WebClient.Builder` 주입 → 소스코드에서 `.baseUrl(REAL_URL)` 호출
- 테스트에서 MockWebServer URL 주입해도 소스코드가 덮어씀
- 단위 테스트가 실제 API 호출 → 400 Bad Request 발생

**해결**:
- `WebClient.Builder` 대신 `WebClient` 주입
- `ExternalApiConfig`에서 baseUrl 미리 설정한 WebClient Bean 생성
- 테스트에서는 MockWebServer URL로 생성한 WebClient 주입

**코드 변경**:
```java
// Before
@Bean
public WebClient.Builder webClientBuilder() {
    return WebClient.builder();
}

// After
@Bean
public WebClient slackWebClient() {
    return WebClient.builder()
        .baseUrl("https://slack.com/api")
        .build();
}
```

**효과**:
- 프로덕션 코드 수정 없이 테스트 가능
- 테스트에서 완전한 제어 가능
- 의존성 주입 원칙 준수

### 2. Gemini API 인증 방식

**문제**:
- 초기 구현: 쿼리 파라미터 `?key=API_KEY`
- 실제 호출 시 400 Bad Request 발생
- curl 테스트 결과 헤더 인증 필요함을 확인

**해결**:
```java
// Before
.uri(uriBuilder -> uriBuilder
    .path("/models/" + MODEL + ":generateContent")
    .queryParam("key", apiKey)
    .build())

// After
.uri("/models/" + MODEL + ":generateContent")
.header("x-goog-api-key", apiKey)
```

### 3. Gemini 모델 버전 업데이트

**문제**:
- 초기 모델: `gemini-pro` (구버전)
- API 호출 시 404 Not Found

**해결**:
- 최신 모델로 변경: `gemini-2.5-flash-lite`
- 더 빠른 응답 속도, 무료 tier 60 req/min

### 4. RetryConfig IllegalStateException

**문제**:
```java
RetryConfig config = RetryConfig.custom()
    .maxAttempts(3)
    .waitDuration(Duration.ofSeconds(1))  // ❌ 충돌!
    .intervalFunction(IntervalFunction.ofExponentialBackoff(1000, 2))  // ❌ 충돌!
    .build();
```
- `waitDuration`과 `intervalFunction`을 동시에 사용 불가
- IllegalStateException 발생

**해결**:
```java
RetryConfig config = RetryConfig.custom()
    .maxAttempts(3)
    .intervalFunction(IntervalFunction.ofExponentialBackoff(1000, 2))  // ✅ 지수 백오프만 사용
    .retryExceptions(Exception.class)
    .build();
```

### 5. ChatGPT → Gemini 전환

**문제**:
- 프로젝트 초기 ChatGPT API 사용 계획
- ChatGPT 유료화로 Gemini로 변경 결정
- 기존 ChatGPT 코드 처리 필요

**해결**:
- ChatGPT 관련 코드 주석처리 (향후 참고용)
- ApiProvider.CHATGPT → ApiProvider.GEMINI 변경
- ExternalApiLogRepositoryTest 테스트 데이터 수정

### 6. ApiLogDomainService 반환 타입 문제

**문제**:
```java
// Before
private String maskSensitiveInfo(Object data) {
    String json = objectMapper.writeValueAsString(data);
    // ... masking logic
    return json;  // ❌ String 반환
}

// ExternalApiLog.builder()
//     .requestData(maskSensitiveInfo(request))  // ❌ Map<String,Object> 필요
```

**해결**:
```java
// After
private Map<String, Object> maskSensitiveInfo(Object data) {
    String json = objectMapper.writeValueAsString(data);
    // ... masking logic
    return objectMapper.readValue(json, Map.class);  // ✅ Map 반환
}
```

## 다음 단계

### Issue #14: REST API 구현
1. NotificationFacade 구현 (Use case orchestration)
2. NotificationController 구현 (REST endpoints)
3. Request/Response DTOs 생성
4. Gemini AI 프롬프트 템플릿 작성 (배송 시한 계산)
5. Slack 메시지 템플릿 작성
6. Controller 통합 테스트

### Issue #35: Kafka Event Consumer
1. OrderCreatedEvent 소비자 구현
2. DeliveryStatusChangedEvent 소비자 구현
3. Event → Notification 변환 로직
4. Kafka 통합 테스트

### Issue #16: 조회 및 통계 API
1. 알림 조회 API (페이징, 검색)
2. API 로그 조회 API (MASTER 권한)
3. 통계 API (성공률, 평균 응답시간)

### Issue #36: Daily Route Optimization (Challenge)
1. Naver Maps API client 구현
2. 일일 배송 경로 최적화 스케줄러
3. Gemini TSP 프롬프트 작성
4. 06:00 Slack 알림 발송

## 커밋 예정 이력

1. `feat: add slack api client with webclient and resilience4j`
2. `feat: add gemini api client with retry logic`
3. `feat: add api log domain service with sensitive data masking`
4. `feat: add slack and gemini client wrappers`
5. `refactor: change webclient injection pattern for testability`
6. `feat: add external api config with retry settings`
7. `test: add unit tests for slack and gemini api clients`
8. `test: add integration tests with real api validation`
9. `chore: comment out chatgpt code and update api provider enum`
10. `docs: update readme and claude.md with issue #13 completion`

## 리뷰 포인트

- ✅ WebClient 주입 패턴: 테스트 가능성과 프로덕션 코드 분리가 적절한가?
- ✅ Wrapper 패턴: 자동 로깅이 비즈니스 로직과 잘 분리되었는가?
- ✅ 민감 정보 마스킹: 보안 요구사항을 충족하는가?
- ✅ Retry 설정: 지수 백오프가 외부 API 호출에 적합한가?
- ✅ 테스트 전략: 단위/통합 테스트 분리가 적절한가?
- ✅ API 선택: Gemini vs ChatGPT 선택이 합리적인가?

## 기술적 결정 사항

### 1. WebClient 주입 패턴

**결정**: `WebClient.Builder` 대신 `WebClient` 주입
**이유**:
- 테스트에서 MockWebServer URL 완전 제어 가능
- 프로덕션 코드에서 baseUrl 하드코딩 방지
- 의존성 주입 원칙 준수

### 2. Gemini API 선택

**결정**: ChatGPT 대신 Gemini 사용
**이유**:
- ChatGPT 유료화 (월 $20)
- Gemini 무료 tier: 60 req/min (충분)
- Google Cloud 통합 용이

### 3. Retry 전략

**결정**: Slack 3회, Gemini 2회 재시도 + 지수 백오프
**이유**:
- Slack: 알림 중요도 높음 → 더 많은 재시도
- Gemini: AI 응답 시간 김 → 적은 재시도
- 지수 백오프: 외부 서버 부하 분산

### 4. 민감 정보 마스킹

**결정**: 정규표현식으로 JSON 패턴 탐지 및 마스킹
**이유**:
- DB에 API 키, 토큰 저장 방지
- 감사 추적 유지하면서 보안 강화
- 로깅 실패 시에도 원본 API 호출 정상 진행

### 5. Wrapper 패턴

**결정**: Client와 Wrapper 분리
**이유**:
- SRP: Client는 HTTP 호출만, Wrapper는 로깅만
- 테스트 용이성: Client 단독 테스트 가능
- 확장성: 로깅 외 기능(캐싱, 메트릭) 추가 용이

## 참고 문서

- [CLAUDE.md](../../CLAUDE.md)
- [notification-service README.md](../../notification-service/README.md)
- [issue-12-notification-db-entity-repository.md](./issue-12-notification-db-entity-repository.md)
- [Slack API - chat.postMessage](https://api.slack.com/methods/chat.postMessage)
- [Google Gemini API Docs](https://ai.google.dev/gemini-api/docs)

## 성과

- ✅ 35/35 테스트 100% 통과 (단위 6 + 통합 3 + 기존 26)
- ✅ Slack, Gemini API 키 검증 완료
- ✅ 자동 로깅 인프라 구축 (민감 정보 마스킹 포함)
- ✅ WebClient 주입 패턴 리팩토링 (테스트 가능성 확보)
- ✅ Retry 로직 구현 (지수 백오프)
- ✅ Wrapper 패턴 적용 (관심사 분리)
- ✅ ChatGPT → Gemini 전환 완료