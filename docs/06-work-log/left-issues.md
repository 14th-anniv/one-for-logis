# 남은 이슈 (Left Issues)

## 🎯 권장 작업 순서
```
#13 ✅ → #14 ✅ → #16 ✅ → #35 ✅ → #76 ✅ → #109 ✅ → #85 (deletedBy 수정) → #86 (Kafka 보안) → #36 (Challenge) → #87-88 (Performance)
```

**Current Status**: Issue #109 completed (2025-11-13)

**Completed**:
- ✅ Issue #76: 리스크 개선 (2025-11-12)
- ✅ Issue #109: Swagger 테스트 & FeignException 처리 (2025-11-13)

**Recommended Next**:
1. **Issue #85-86** (보안/리팩토링, 1.5일)
2. **Issue #36** (Challenge, 3-4일)
3. **Issue #87-88** (Performance, 1일)

---

## 📐 Architecture Pattern Guide (PR #44, PR #48 학습)

**PR #44에서 적용된 DDD 패턴** (hub-service 참고):

### 1️⃣ Repository 계층 분리 (✅ notification-service 이미 적용)
```
domain/repository/
  └── NotificationRepository.java        // 인터페이스 (도메인 독립성)

infrastructure/persistence/
  ├── NotificationJpaRepository.java     // Spring Data JPA 인터페이스
  └── NotificationRepositoryImpl.java    // 도메인 인터페이스 구현체
```

### 2️⃣ Service 계층 분리 (⚠️ notification-service 적용 필요)
```
application/service/
  └── NotificationService.java           // 비즈니스 흐름 제어
      - 외부 API 호출 orchestration
      - Transaction 관리
      - 캐시 로직 (필요시 infrastructure.cache로 분리)

domain/service/
  └── NotificationDomainService.java     // ✅ 이미 존재
      - 순수 비즈니스 로직
      - 외부 의존성 없음
```

**hub-service 예시** (HubService.java):
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubService {
    private final HubRepository hubRepository;
    private final HubCacheService hubCacheService;  // 캐시 로직 분리

    public HubResponse getHubById(UUID hubId) {
        // 1. 캐시 조회
        HubResponse cached = hubCacheService.getHubCache(hubId);
        if (cached != null) return cached;

        // 2. DB 조회
        Hub hub = hubRepository.findByIdAndDeletedFalse(hubId)
            .orElseThrow(() -> new CustomException(ErrorCode.HUB_NOT_FOUND));

        // 3. 캐시 저장
        HubResponse response = HubResponse.from(hub);
        hubCacheService.saveHubCache(response);
        return response;
    }
}
```

### 3️⃣ notification-service 적용 계획
- ✅ Repository 패턴: 이미 올바르게 구현됨
- ❌ Application Service 분리: 현재 domain.service에만 존재
- ❌ Infrastructure 클라이언트: user-service FeignClient 필요
- ❌ Presentation Layer: Controller, Request/Response DTO 필요

**다음 작업**: Issue #14에서 application.service.NotificationService 생성

### 4️⃣ DTO Pattern (PR #48 결정사항)

**Team Standard** (hub-service 기준):
- ✅ Presentation DTOs: Use `record` for immutability
- ✅ External API DTOs: Keep as `class` with `@Builder` (infrastructure dependency)

**Examples**:
```java
// ✅ Presentation DTO (Issue #14에서 작성)
public record NotificationCreateRequest(
    @NotBlank String recipientSlackId,
    @NotBlank String message
) {}

// ✅ External API DTO (현재 상태 유지)
@Getter
@Builder
public class SlackMessageRequest {
    private String channel;
    private String text;
    @JsonProperty("username") private String username;
    @JsonProperty("icon_emoji") private String iconEmoji;
}
```

**Rationale**:
- Presentation DTOs: Internal use, controlled by us → record (immutability + conciseness)
- External API DTOs: External spec, complex Builder needs → class (flexibility + Optional fields)

---

# [FEATURE] 알림 서비스 외부 API 클라이언트 구현 (Slack, Gemini) #13

**우선순위**: ⭐⭐⭐ HIGH (선행 작업)

## 📝 Description
> Slack API 및 Google Gemini API 클라이언트 구현
> API 호출 자동 로깅 기능 구현
> 재시도 로직 및 에러 핸들링 적용

## ⭐ To-do
- [x] Slack API 클라이언트 구현 (chat.postMessage)
- [x] Gemini API 클라이언트 구현 (generateContent)
- [x] ApiLogDomainService 구현 (자동 로깅)
- [x] 민감 정보 마스킹 로직 구현 (API Key, Token)
- [x] Resilience4j 재시도 설정 (Slack: 3회, Gemini: 2회)
- [x] Exponential Backoff 구현
- [x] 에러 핸들링 및 폴백 로직
- [x] 환경변수 설정 (SLACK_BOT_TOKEN, GEMINI_API_KEY)
- [x] API 클라이언트 단위 테스트 (MockWebServer)

## ✅ ETC
- Slack SDK 대신 WebClient 사용
- Gemini API 무료 티어: 60 requests/min
- infrastructure.client 패키지에 구현
- 완료: 2025-11-05

---

# [FEATURE] 알림 서비스 주문 알림 REST API 구현 #14

**우선순위**: ⭐⭐⭐ HIGH (핵심 기능)

## 📝 Description
> 주문 생성 시 자동 알림 기능 구현 (AI 기반 발송 시한 계산)
> 수동 메시지 발송 기능 구현 (스냅샷 패턴 적용)
> user-service와 FeignClient 연동

## ⭐ To-do
- [x] 주문 알림 API 구현 (POST /api/v1/notifications/order)
- [x] Gemini AI 프롬프트 설계 (최종 발송 시한 계산)
- [x] Slack 메시지 템플릿 작성
- [x] 메시지 이력 저장 로직 (p_notifications)
- [x] 수동 메시지 발송 API 구현 (POST /api/v1/notifications/manual)
- [x] user-service FeignClient 구현 (UserServiceClient, UserResponse)
- [x] 발신자 정보 스냅샷 저장 로직
- [x] GlobalExceptionHandler 구현 (common-lib 사용)
- [ ] 비동기 처리 적용 (@Async) - 향후 필요 시
- [x] 권한 검증 로직 (@PreAuthorize)
- [x] Request/Response DTO 작성 (record pattern)
- [x] API 문서화 (Swagger - @Tag, @Operation)
- [x] Controller 테스트 작성 (NotificationControllerTest - 9 tests)
- [ ] 페이지네이션 구현 (NotificationRepository + Controller) - TODO
- [ ] E2E 통합 테스트 작성 - 향후

## ✅ ETC
- SYSTEM 타입 메시지는 sender 정보 NULL
- USER 타입 메시지는 sender_username, sender_slack_id, sender_name 필수
- 의존성: Issue #13 완료 ✅
- 실제 소요: 1일 (2025-11-07)
- 상태: ✅ 완료 (페이지네이션 제외)

---

# [FEATURE] 알림 서비스 조회 및 통계 API 구현 (MASTER) #16 ✅

**우선순위**: ⭐⭐⭐ HIGH (기본 구현 완성)

## 📝 Description
> 메시지 이력 조회 API 구현 (페이징, 필터링)
> 외부 API 호출 로그 조회 및 통계 기능 구현
> MASTER 권한 검증 적용

## ⭐ To-do
- [x] 메시지 이력 조회 API (GET /api/v1/notifications)
- [x] 페이징 및 정렬 기능 (Pageable)
- [x] 필터링 기능 (messageType, status, senderUsername, recipientSlackId)
- [x] API 로그 조회 API (GET /api/v1/notifications/api-logs) - 페이징 포함
- [x] API 통계 조회 API (GET /api/v1/notifications/api-logs/stats)
- [x] 통계 계산 로직 (성공률, 평균 응답 시간, 총 비용)
- [x] MASTER 권한 검증 (@PreAuthorize)
- [x] CriteriaBuilder 동적 쿼리 적용 (DB 레벨 필터링)
- [x] API 문서화 (Swagger)
- [x] 단위 테스트 작성 (10/10 통과)

## ✅ ETC
- 완료일: 2025-11-10
- 실제 소요: 2일
- PR: #68 (Merged to dev)
- 브랜치: feature/#16-notification-query-api
- 테스트: NotificationServiceTest (10개), NotificationRepositoryTest (10개)
- **Technical**: CriteriaBuilder로 DB 레벨 동적 쿼리 구현 (Stream 필터링 대비 성능 개선)

---

# [REFACTOR] 알림 서비스 리스크 개선 #76

**우선순위**: ⭐⭐⭐ HIGH (Issue #35 선행 권장)

## 📝 Description

**Codex 자동 리뷰 기반 notification-service 리스크 개선 작업**

notification-service의 코드 품질 및 안정성 개선을 위한 통합 리팩토링 작업입니다. Codex 자동 리뷰 결과 발견된 6개 Critical/Important 이슈와 최신 PR 패턴(#65, #75) 반영을 포함합니다.

**관련 문서**: [docs/review/notification-service-review.md](./review/notification-service-review.md)

## ⭐ To-do

### 🔴 Priority 1 (Critical - 필수)

- [ ] **1. 통합 테스트 분리 (실제 API 호출)**
  - 현재: `SlackApiAuthIntegrationTest`, `GeminiApiKeyIntegrationTest`가 기본 test 태스크에 포함
  - 해결: `@Disabled` 또는 `@EnabledIfEnvironmentVariable` 적용, 또는 별도 Gradle 태스크 분리

- [ ] **2. user-service NPE 위험 해결**
  - 현재: `userServiceClient.getUserByUsername().data()` - null 체크 없음
  - 해결: PR #75 패턴 적용 (FeignClient 응답 검증)
  - 위치: `NotificationController.java:85-86`

- [ ] **3. Slack 실패 HTTP 응답 개선**
  - 현재: Slack 발송 실패 시에도 200 OK 또는 201 Created 반환
  - 해결 옵션: Option A (예외 throw), Option B (ApiResponse.isSuccess=false), Option C (비동기 전환)
  - **Note**: 비즈니스 요구사항 명확화 필요

### 🟡 Priority 2 (Important - 권장)

- [ ] **4. Gemini messageId 연계**
  - 현재: Gemini 호출 시 `messageId` 전달 안 함 (null)
  - 해결: Notification 엔티티를 먼저 저장(PENDING 상태) → Gemini 호출 시 messageId 전달

- [ ] **5. Slack error 메시지 유실 해결**
  - 현재: fallback 응답의 `error` 필드가 비어있음
  - 해결: SlackClientWrapper 예외 처리 시 error 필드 설정

- [ ] **6. NotificationService 단위 테스트 추가**
  - 현재: `NotificationService`에 대한 단위 테스트 없음
  - 해결: MockitoExtension으로 SlackClientWrapper, GeminiClientWrapper Mock 테스트

- [ ] **7. Entity 예외 타입 통일** (from PR #65 패턴)
  - 현재: `Notification` Entity에서 `IllegalStateException` 사용
  - 해결: `IllegalStateException` → `CustomException`, ErrorCode 추가
  - 위치: `Notification.java:125-158`

## ✅ ETC

### 참고 문서
- **Codex Review**: [docs/review/notification-service-review.md](./review/notification-service-review.md)
- **PR #75 패턴**: FeignClient 응답 검증 ([docs/scrum/PR75-feignclient-status-code-fix.md](./scrum/PR75-feignclient-status-code-fix.md))
- **PR #65 패턴**: Entity 예외 타입 통일 ([docs/scrum/PR65-product-basic-CRUD.md](./scrum/PR65-product-basic-CRUD.md))

### 개발 일정 (예상)
- **Phase 1** (1일): Priority 1 (Critical) 3개 항목
- **Phase 2** (1일): Priority 2 (Important) 4개 항목
- **Total**: 2일

### 특이사항
- **Issue #3**: 비즈니스 요구사항 명확화 필요 (알림 실패가 주문 생성을 막아야 하는가?)
- **테스트 전략**: 통합 테스트 분리 후 CI/CD 파이프라인 안정성 확보
- **의존성**: user-service API 구현 필요 (getUserByUsername 엔드포인트)

---

# [FEATURE] 알림 서비스 Kafka 이벤트 소비자 구현 #35 ✅

**우선순위**: ⭐⭐⭐ HIGH (비동기 연동)

## 📝 Description
> order-service의 주문 생성 이벤트 구독
> delivery-service의 배송 상태 변경 이벤트 구독
> 이벤트 기반 알림 발송 자동화

## ⭐ To-do
- [x] Kafka 의존성 추가 (spring-kafka)
- [x] Kafka Consumer 설정 (application.yml)
- [x] order-created 이벤트 리스너 구현 (OrderCreatedConsumer)
- [x] delivery-status-changed 이벤트 리스너 구현 (DeliveryStatusChangedConsumer)
- [x] 이벤트 DTO 정의 (OrderCreatedEvent, DeliveryStatusChangedEvent - record pattern)
- [x] 이벤트 → 알림 변환 로직
- [x] 멱등성 처리 (event_id 기반 중복 방지, DB unique constraint)
- [x] 에러 핸들링 (ErrorHandlingDeserializer)
- [x] DB Schema 수정 (MessageType enum에 DELIVERY_STATUS_UPDATE 추가)
- [x] PostgreSQL CHECK constraint 수정
- [x] 이벤트 소비 통합 테스트 (test-kafka-consumer.sh, 4/4 통과)
- [ ] Dead Letter Queue (DLQ) 설정 - 향후

## ✅ ETC
- 완료일: 2025-11-11
- 실제 소요: 1일
- 브랜치: feature/#35-notification-service-kafka-challenge
- 상태: ✅ 완료 (Ready for PR)
- infrastructure.kafka 패키지에 구현
- 토픽별 별도 ContainerFactory 구성
- Real Slack integration 성공 (C09QY22AMEE)
- **Technical Highlights**:
  - ErrorHandlingDeserializer + JsonDeserializer 조합
  - 트랜잭션 분리: DB 저장 → Slack 발송
  - Kafka 멱등성: Repository.existsByEventId() 체크

---

# [FEATURE] 배송 상태 변경 알림 REST API 추가 #84 ✅

**우선순위**: ⭐⭐ MEDIUM (Issue #35 후속)

## 📝 Description

> 배송 상태 변경 알림을 REST API로 직접 발송할 수 있는 기능 추가
> 현재는 Kafka Event만 지원하며, REST API를 추가하여 일관성 및 재발송 기능 제공

**배경**: Issue #35에서 DeliveryStatusChangedConsumer (Kafka) 구현 완료. 주문 알림은 REST + Kafka 둘 다 지원하지만, 배송 상태 알림은 Kafka만 지원하여 일관성 부족.

## ⭐ To-do

- [x] `POST /api/v1/notifications/delivery-status` 엔드포인트 추가
- [x] Request DTO 작성: `DeliveryStatusNotificationRequest`
  - deliveryId (UUID)
  - orderId (UUID)
  - previousStatus (DeliveryStatus)
  - currentStatus (DeliveryStatus)
  - recipientSlackId (String)
  - recipientName (String)
- [x] Response DTO: NotificationResponse (기존 재사용)
- [x] NotificationService.sendDeliveryStatusNotification() 메서드 추가
- [x] DeliveryStatusChangedConsumer 로직 재사용 (메시지 빌드, Slack 발송)
- [x] 권한 검증: `ALL` (MASTER, HUB_MANAGER, DELIVERY_MANAGER, COMPANY_MANAGER)
- [x] Controller 테스트 작성
- [x] API 문서화 (Swagger)

## ✅ ETC

### 완료 정보
- **Status**: ✅ Completed
- **완료일**: 2025-11-13
- **PR**: #105 (Merged to dev)
- **실제 소요**: 0.5일
- **Commit**: b755707

### 구현 내용
1. **일관성 유지**: 주문 알림과 동일하게 REST + Kafka 둘 다 지원
2. **재발송 기능**: Slack 발송 실패 시 수동 재전송 가능
3. **테스트/디버깅**: REST API로 직접 테스트 가능
4. **장애 대응**: Kafka 장애 시 대체 수단 확보

### 의존성
- Issue #35 완료 ✅
- DeliveryStatusChangedConsumer 로직 재사용 ✅

---

# [FEATURE] 알림 서비스 일일 경로 최적화 스케줄러(Challenge) #36

**우선순위**: ⭐ LOW (Challenge 기능)

## 📝 Description
> 매일 06:00 배송 경로 최적화 스케줄러 실행
> Gemini API로 TSP 문제 해결
> Naver Maps Directions 5 API로 실제 경로 계산
> 최적화된 배송 순서를 업체 배송 담당자에게 Slack 알림

## ⭐ To-do
- [ ] Spring Scheduler 설정 (@EnableScheduling)
- [ ] 일일 배송 데이터 조회 로직 (delivery-service FeignClient)
- [ ] Naver Maps API 클라이언트 구현 (Directions 5)
- [ ] Gemini TSP 최적화 프롬프트 설계
- [ ] 경유지 순서 최적화 알고리즘
- [ ] 최적화 결과 Slack 메시지 템플릿
- [ ] 업체 배송 담당자 조회 로직
- [ ] Naver Maps API 호출 로깅
- [ ] 스케줄러 실행 이력 저장 (선택)
- [ ] 환경변수 설정 (NAVER_MAPS_CLIENT_ID, CLIENT_SECRET)
- [ ] 스케줄러 테스트

## ✅ ETC
- Naver Maps Directions 5 API: 경유지 최대 5개
- Gemini로 경유지 순서만 결정, Naver Maps로 실제 거리/시간 계산
- Cron: `0 0 6 * * *` (매일 06:00)
- delivery-service에서 당일 배송 정보 제공 필요 (팀 협의)
- 의존성: Issue #13, #35 완료 필요
- 예정 일정: 3-4일 소요

---

# [REFACTOR] deletedBy 사용자 정보 수집 #85

**우선순위**: ⭐⭐ MEDIUM (보안/감사)

## 📝 Description

> NotificationRepositoryImpl의 `markAsDeleted("SYSTEM")` 하드코딩을 SecurityContext 기반 사용자 정보로 변경
> 감사 추적(Audit Trail) 정확성 향상

**배경**: 현재 소프트 삭제 시 deletedBy가 "SYSTEM"으로 고정되어 실제 삭제 요청자 추적 불가

## ⭐ To-do

- [ ] AuthContextUtil 헬퍼 클래스 구현
  - getCurrentUsername(): SecurityContext에서 사용자명 추출
  - SYSTEM fallback (인증 없는 경우)
- [ ] NotificationRepositoryImpl 수정
  - deleteById(), deleteAll() 메서드에서 getCurrentUsername() 사용
- [ ] ExternalApiLogRepositoryImpl 수정 (동일 패턴)
- [ ] 단위 테스트 작성 (MockSecurityContext)

## ✅ ETC

### 구현 예시
```java
@Component
public class AuthContextUtil {
    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() ||
            auth instanceof AnonymousAuthenticationToken) {
            return "SYSTEM";
        }
        return auth.getName();
    }
}

// NotificationRepositoryImpl
public void deleteById(UUID id) {
    notification.markAsDeleted(AuthContextUtil.getCurrentUsername());
}
```

### 개발 일정 (예상)
- **Phase 1** (0.3일): AuthContextUtil 구현, 테스트
- **Phase 2** (0.2일): Repository 수정, 통합 테스트
- **Total**: 0.5일

### 관련 문서
- [notification-service-missing-features.md](./review/notification-service-missing-features.md) (Section 1.1)

---

# [SECURITY] Kafka Consumer 보안 강화 #86

**우선순위**: ⭐⭐⭐ HIGH (보안 Critical)

## 📝 Description

> Kafka 이벤트 검증 로직 추가 (이벤트 서명 또는 발행자 정보 검증)
> 악의적 이벤트 발행 방어 및 데이터 무결성 보장

**배경**: 현재 Kafka Consumer (OrderCreated, DeliveryStatusChanged)에 인증/검증 로직 없음

## ⭐ To-do

### Option A: 이벤트 서명 검증 (권장)
- [ ] Event DTO에 서명 필드 추가
  - OrderCreatedEvent: signature (String)
  - DeliveryStatusChangedEvent: signature (String)
- [ ] EventSignatureService 구현
  - calculateSignature(): HMAC-SHA256 기반 서명 생성
  - verifySignature(): 서명 검증
- [ ] Consumer에 검증 로직 추가
  - OrderCreatedConsumer, DeliveryStatusChangedConsumer
  - 서명 실패 시 로깅 후 이벤트 무시
- [ ] 통합 테스트 작성 (유효/무효 서명)
- [ ] order-service, delivery-service 협의 (서명 추가 요청)

### Option B: 발행자 정보 검증 (대안)
- [ ] Event DTO에 발행자 필드 추가
  - publisherId (String)
  - publisherService (String: "order-service", "delivery-service")
- [ ] Consumer에 화이트리스트 검증
  ```java
  private static final Set<String> ALLOWED_PUBLISHERS =
      Set.of("order-service", "delivery-service");

  if (!ALLOWED_PUBLISHERS.contains(event.publisherService())) {
      log.error("Unauthorized publisher");
      return;
  }
  ```

## ✅ ETC

### 보안 위험 분석 (CVSS 7.5 - High)
- **공격 벡터**: 외부에서 Kafka 토픽에 직접 발행
- **영향도**: 데이터 무결성 손상, 허위 알림 발송
- **완화 방안**: 이벤트 서명 검증 + Kafka ACL

### 개발 일정 (예상)
- **Phase 1** (0.5일): EventSignatureService 구현, 단위 테스트
- **Phase 2** (0.5일): Consumer 수정, 통합 테스트
- **Total**: 1일

### 의존성
- order-service, delivery-service 협의 필요 (Event DTO 변경)

### 관련 문서
- [notification-service-missing-features.md](./review/notification-service-missing-features.md) (Section 3.1)

---

# [PERFORMANCE] Gemini AI 응답 캐싱 #87

**우선순위**: ⭐ LOW (성능 개선)

## 📝 Description

> Gemini API 응답 캐싱 (같은 경로는 1시간 TTL)
> 주문 알림 응답 시간 단축 (평균 3초 → 0.5초)

**배경**: Gemini API 응답 시간 2-5초 (평균 3초), 같은 경로에 대해 중복 호출 발생

## ⭐ To-do

- [ ] Spring Cache 설정 (Caffeine)
  ```yaml
  spring:
    cache:
      type: caffeine
      caffeine:
        spec: maximumSize=500,expireAfterWrite=1h
  ```
- [ ] NotificationService.calculateDepartureDeadline() 메서드에 @Cacheable 적용
  - Cache key: `departureHub-destinationHub`
  - TTL: 1시간
- [ ] Cache 동작 확인 (로그 레벨: DEBUG)
- [ ] 성능 테스트 (응답 시간 측정)
  - 캐시 히트 시: 0.5초 이하
  - 캐시 미스 시: 2-5초 (Gemini API 호출)

## ✅ ETC

### 구현 예시
```java
@Service
@EnableCaching
public class NotificationService {

    @Cacheable(value = "geminiDeadlines",
               key = "#request.departureHub + '-' + #request.destinationHub")
    public String calculateDepartureDeadline(OrderNotificationRequest request) {
        return geminiClientWrapper.generateDeadline(...);
    }
}
```

### 개발 일정 (예상)
- **Phase 1** (0.3일): Cache 설정, @Cacheable 적용
- **Phase 2** (0.2일): 성능 테스트, 로깅
- **Total**: 0.5일

### 기대 효과
- 응답 시간: 3초 → 0.5초 (83% 개선)
- Gemini API 호출 비용 절감 (60 req/min 제한 완화)

### 관련 문서
- [notification-service-missing-features.md](./review/notification-service-missing-features.md) (Section 5.2)

---

# [FEATURE] Dead Letter Queue 구현 #88

**우선순위**: ⭐ LOW (Kafka 안정성)

## 📝 Description

> Kafka Consumer 실패 메시지 자동 재처리 (DLQ 패턴)
> 이벤트 처리 실패 시 수동 재처리 UI 연동

**배경**: 현재 Consumer 실패 시 이벤트 유실 위험 (로그만 남음)

## ⭐ To-do

- [ ] KafkaListenerContainerFactory에 ErrorHandler 추가
  ```java
  factory.setCommonErrorHandler(new DefaultErrorHandler(
      new DeadLetterPublishingRecoverer(kafkaTemplate),
      new FixedBackOff(1000L, 3L)  // 1초 간격, 3회 재시도
  ));
  ```
- [ ] notification.dlq 토픽 생성 (Kafka)
- [ ] DLQ Consumer 구현
  - 실패 이벤트 로깅
  - 관리자 대시보드 UI 연동 (선택)
- [ ] 통합 테스트 작성 (의도적 실패 → DLQ 이동 확인)

## ✅ ETC

### 개발 일정 (예상)
- **Phase 1** (0.5일): ErrorHandler 설정, DLQ 토픽 생성
- **Phase 2** (0.5일): DLQ Consumer 구현, 테스트
- **Total**: 1일

### 관련 문서
- [notification-service-missing-features.md](./review/notification-service-missing-features.md) (Section 6.2)
