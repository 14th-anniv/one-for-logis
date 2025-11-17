# PR #13 - External API Client Implementation

## Issue Number
closed #13

## 📝 Description

notification-service의 외부 API 클라이언트(Slack, Gemini) 구현 및 자동 로깅 인프라 구축을 완료했습니다.

### 주요 구현 사항

#### 1. Slack API Client
- WebClient 기반 HTTP 클라이언트 구현
- `chat.postMessage` 엔드포인트 통합
- Bearer Token 인증 방식
- Resilience4j Retry (3회 재시도, 지수 백오프 1초 * 2^n)

#### 2. Gemini API Client
- Google Gemini API 통합 (ChatGPT 대체)
- `gemini-2.5-flash-lite` 모델 사용
- `x-goog-api-key` 헤더 인증
- Resilience4j Retry (2회 재시도, 지수 백오프 2초 * 2^n)

#### 3. ApiLogDomainService (자동 로깅)
- 모든 외부 API 호출 자동 로깅
- 민감 정보 마스킹 (token, api_key, authorization 등)
- 실행 시간, HTTP 상태, 성공/실패 추적
- ExternalApiLog 엔티티 자동 저장

#### 4. Wrapper Pattern
- `SlackClientWrapper`: Slack API 호출 래퍼 (자동 로깅)
- `GeminiClientWrapper`: Gemini API 호출 래퍼 (자동 로깅)
- Try-catch 에러 핸들링
- 로깅 실패 시에도 원본 예외 전파

#### 5. WebClient 주입 패턴 리팩토링
- 기존: `WebClient.Builder` 주입 → 소스코드에서 baseUrl 하드코딩
- 개선: `WebClient` 주입 → Config에서 baseUrl 설정
- 효과: 단위 테스트에서 MockWebServer URL 주입 가능

#### 6. ExternalApiConfig
- `slackWebClient`, `geminiWebClient` Bean 등록 (baseUrl 포함)
- `slackRetry`, `geminiRetry` Bean 등록 (Resilience4j RetryConfig)

#### 7. ChatGPT → Gemini 전환
- ChatGPT 유료화로 인한 Gemini 전환
- ChatGPT 관련 코드 주석처리 (향후 참고용)
- `ApiProvider.CHATGPT` → `ApiProvider.GEMINI`로 변경

### 신규 파일 (16개)

**Infrastructure - External API Clients (10개)**
- `slack/SlackApiClient.java`
- `slack/SlackMessageRequest.java`
- `slack/SlackMessageResponse.java`
- `gemini/GeminiApiClient.java`
- `gemini/GeminiRequest.java`
- `gemini/GeminiResponse.java`
- `gemini/GeminiContent.java`
- `SlackClientWrapper.java`
- `GeminiClientWrapper.java`
- `ExternalApiConfig.java`

**Domain Service (1개)**
- `ApiLogDomainService.java`

**Unit Tests (2개)**
- `slack/SlackApiClientTest.java`
- `gemini/GeminiApiClientTest.java`

**Integration Tests (2개)**
- `slack/SlackApiAuthIntegrationTest.java`
- `gemini/GeminiApiKeyIntegrationTest.java`

**Documentation (1개)**
- `docs/review/issue-13-external-api-client.md`

### 수정 파일 (4개)
- `ExternalApiLog.java`: Builder 생성자 추가
- `application.yml`: Slack, Gemini API 키 환경변수 추가
- `application-test.yml`: 테스트용 API 키 설정 (더미 값으로 변경)
- `.env.example`: API 키 예시 추가

## 🌐 Test Result

### 테스트 결과 (35/35 tests passed, 100% success rate)

```bash
./gradlew :notification-service:test

# 테스트 상세
✅ NotificationRepositoryTest: 15/15
✅ ExternalApiLogRepositoryTest: 11/11
✅ SlackApiClientTest: 3/3
✅ GeminiApiClientTest: 3/3
✅ SlackApiAuthIntegrationTest: 1/1
✅ GeminiApiKeyIntegrationTest: 2/2
```

### Unit Tests (MockWebServer)
- Slack API 호출 성공/실패/네트워크 에러 테스트
- Gemini API 호출 성공/빈 응답/네트워크 에러 테스트
- RecordedRequest로 Authorization 헤더 검증
- MockResponse로 HTTP 응답 모킹

### Integration Tests (Real API)
- Slack Bot Token 유효성 검증 (`/auth.test`)
- Gemini API Key 유효성 검증 (간단한 프롬프트)
- Gemini 배송 시한 계산 프롬프트 테스트

## 🔎 To Reviewer

### 주요 리뷰 포인트

#### 1. WebClient 주입 패턴
- **문제**: 초기 구현에서 `WebClient.Builder` 주입 시 테스트에서 baseUrl 제어 불가
- **해결**: `WebClient` 주입 + Config에서 baseUrl 설정
- **질문**: 이 패턴이 Spring WebClient 사용 베스트 프랙티스에 부합하는가?

#### 2. Wrapper 패턴 vs AOP
- **선택**: Wrapper 패턴으로 자동 로깅 구현
- **이유**: SRP 준수, 테스트 용이성, 명시적 제어
- **질문**: AOP 대신 Wrapper를 선택한 것이 적절한가?

#### 3. 민감 정보 마스킹
- **구현**: 정규표현식으로 JSON 패턴 탐지 및 `***MASKED***` 치환
- **범위**: token, api_key, authorization, password
- **질문**: 마스킹 패턴이 충분한가? 추가 필요한 키워드가 있는가?

#### 4. Retry 전략
- **Slack**: 3회 재시도, 지수 백오프 1초 * 2^n
- **Gemini**: 2회 재시도, 지수 백오프 2초 * 2^n
- **질문**: 재시도 횟수와 백오프 전략이 적절한가?

#### 5. Gemini vs ChatGPT
- **선택**: Gemini (`gemini-2.5-flash-lite`)
- **이유**: ChatGPT 유료화, Gemini 무료 tier 60 req/min
- **질문**: 프로젝트 요구사항에 Gemini가 적합한가?

#### 6. 테스트 전략
- **단위 테스트**: MockWebServer로 HTTP 응답 모킹
- **통합 테스트**: 실제 API 호출 (`.env` 파일의 실제 키 사용)
- **질문**: 통합 테스트를 CI/CD에서 실행할지 여부? (API 키 보안)

### 기술적 결정 사항

| 항목 | 선택 | 근거 |
|------|------|------|
| WebClient 주입 | `WebClient` (not Builder) | 테스트 가능성, baseUrl 제어 |
| AI API | Gemini | 무료 tier, 60 req/min |
| Retry 라이브러리 | Resilience4j | Spring Boot 표준, 경량 |
| 로깅 패턴 | Wrapper | SRP, 명시적 제어 |
| 테스트 모킹 | MockWebServer | 실제 HTTP 통신 시뮬레이션 |

### 다음 단계 (Issue #14)

- NotificationFacade 구현 (Use case orchestration)
- NotificationController 구현 (REST endpoints)
- Gemini AI 프롬프트 템플릿 작성 (배송 시한 계산)
- Slack 메시지 템플릿 작성
- Controller 통합 테스트

### 참고 문서
- [상세 리뷰 문서](issue-13-external-api-client.md)
- [notification-service README.md](../../notification-service/README.md)

---

**작성일**: 2025-11-06  
**작성자**: gy990  
**브랜치**: `feature/#13-external-api-client`
