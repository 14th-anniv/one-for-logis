● notification-service 발표 자료 추천 내용

발표 목차에 맞춰 notification-service 담당자로서 강조할 내용을 정리했습니다.

   -------------------------------------------------------------------------------

1️⃣ 팀소개 - 개인 역할 소개

"저는 notification-service를 담당했으며, MSA 환경에서 이벤트 기반 실시간 알림 시스템을
구축했습니다."

     - 담당 서비스: notification-service (포트 8700)
     - 핵심 역할: 
       - Kafka 이벤트 기반 알림 자동화 (주문/배송 상태)
       - Slack API + Gemini AI 통합 (출발 시간 계산)
       - 외부 API 호출 이력 관리 및 통계

   -------------------------------------------------------------------------------

2️⃣ 프로젝트 개요 - notification-service 핵심 기능

📌 서비스 개요

"주문/배송 이벤트를 실시간으로 감지하여 Slack으로 자동 알림을 발송하는 시스템"

핵심 기능 3가지

     - 이벤트 기반 알림 자동화 (Kafka Consumer)
       - 주문 생성 → Gemini AI로 출발 시간 계산 → Slack 알림
       - 배송 상태 변경 → Slack 알림
     - 외부 API 통합
       - Slack API: 실시간 메시지 발송 (C09QY22AMEE 채널)
       - Gemini AI: 자연어 기반 출발 시간 계산
     - 알림 이력 관리
       - 발송 성공/실패 이력 저장
       - 외부 API 호출 로그 (Provider별, 메시지별 조회)
       - 통계 API (성공률, 평균 응답 시간)

   -------------------------------------------------------------------------------

3️⃣ ERD 및 시스템 아키텍처

notification-service 테이블 구조

     p_notifications (알림 메시지)
     ├── id (UUID, PK)
     ├── sender_type (SYSTEM/USER)
     ├── sender_snapshot (JSONB) - 스냅샷 패턴
     ├── recipient_slack_id
     ├── message_type (ORDER_NOTIFICATION/DELIVERY_STATUS_UPDATE/MANUAL)
     ├── message_content (TEXT)
     ├── status (PENDING/SENT/FAILED)
     ├── event_id (UNIQUE) - 멱등성 보장
     └── BaseEntity (audit 필드)
     
     p_external_api_logs (외부 API 호출 이력)
     ├── id (UUID, PK)
     ├── provider (SLACK/GEMINI/NAVER_MAPS)
     ├── request_data (JSONB)
     ├── response_data (JSONB)
     ├── status_code
     ├── duration_ms
     ├── cost_usd (Gemini API 비용)
     └── message_id (FK → p_notifications)

시스템 플로우 다이어그램

     [order-service] ─(Kafka)→ [notification-service]
                      order.created      ↓
                                      1. DB 저장
                                      2. Gemini AI 호출
                                      3. Slack 발송
                                      4. 상태 업데이트
     
     [delivery-service] ─(Kafka)→ [notification-service]
                        delivery.status.changed
                                         ↓
                                     Slack 알림

   -------------------------------------------------------------------------------

4️⃣ 핵심 기술 구현

⭐ 1. Kafka Event-Driven Architecture (이벤트 기반 알림)

"비동기 메시징으로 서비스 간 결합도를 낮추고 확장성을 확보했습니다."

구현 내용

     @KafkaListener(topics = "${topics.order-created}")
     public void onMessage(OrderCreatedEvent event) {
         // 멱등성 검증 (event_id 기반)
         if (notificationRepository.existsByEventId(event.eventId())) {
             log.info("⏭️ Event already processed (idempotency)");
             return;
         }
         
         // 1. Gemini AI로 출발 시간 계산
         String aiContent = geminiClient.generateContent(
             "주문 정보를 바탕으로 최적 출발 시간을 계산하세요"
         );
         
         // 2. DB 저장 (PENDING)
         Notification notification = Notification.createOrderNotification(...);
         notification = repository.save(notification);
         
         // 3. Slack 발송
         slackClient.postMessage(notification.getRecipientSlackId(), aiContent);
         notification.markAsSent();
     }

기술 포인트

     - 멱등성 보장: event_id UNIQUE 제약조건 + 애플리케이션 레벨 체크
     - ErrorHandlingDeserializer: JSON 파싱 오류 시 DLQ 전송
     - 토픽별 ContainerFactory: order.created, delivery.status.changed 분리

   -------------------------------------------------------------------------------

⭐ 2. 트랜잭션 분리 패턴 (에러 메시지 유실 방지)

"DB 저장과 외부 API 호출을 분리하여 에러 이력을 보존했습니다."

Before (문제)

     @Transactional
     public void sendNotification(...) {
         // 1. DB 저장
         notification = repository.save(notification);
         
         // 2. Slack 발송
         slackClient.postMessage(...); // 실패 시 롤백 → 에러 메시지 유실
     }

After (해결)

     @Transactional
     public void sendNotification(...) {
         // 1. DB 저장 (트랜잭션 보장)
         notification = repository.save(notification);
         
         // 2. Slack 발송 (트랜잭션 외부)
         try {
             slackClient.postMessage(...);
             updateSuccessStatus(notification.getId()); // 별도 트랜잭션
         } catch (Exception e) {
             updateFailedStatus(notification.getId(), e.getMessage()); // 별도 트랜잭션
             throw new CustomException(ErrorCode.NOTIFICATION_SEND_FAILED);
         }
     }
     
     @Transactional(propagation = Propagation.REQUIRES_NEW)
     public void updateFailedStatus(UUID id, String errorMessage) {
         notification.markAsFailed(errorMessage);
         repository.save(notification); // 에러 메시지 DB에 저장
     }

효과

     - Slack 실패 시: DB에 FAILED 상태 + 에러 메시지 저장
     - HTTP 500 응답으로 클라이언트에 명확한 실패 전달
     - 재시도 가능한 데이터 보존

   -------------------------------------------------------------------------------

⭐ 3. Gemini AI 통합 (자연어 기반 출발 시간 계산)

"Google Gemini API를 활용하여 주문 정보를 바탕으로 최적 출발 시간을 자동 계산했습니다."

Prompt Engineering

     String prompt = String.format(
         """
         당신은 물류 전문가입니다. 다음 정보를 바탕으로 허브에서 출발해야 할 최적 시간을 
계산하세요.

         - 출발지: %s
         - 경유지: %s
         - 목적지: %s
         - 상품: %s
         - 요청사항: %s
         
         응답 형식: "YYYY-MM-DD HH:MM까지 발송 완료 바랍니다."
         """,
         departureHub, waypoints, destinationHub, productInfo, requestDetails
     );

비용 최적화

     - gemini-2.0-flash-lite 모델 사용 (무료 티어)
     - messageId 연계로 API 호출 이력 추적
     - 향후 Redis 캐싱으로 동일 경로 재사용 가능 (Issue #87)

   -------------------------------------------------------------------------------

⭐ 4. Wrapper 패턴 (자동 로깅)

"외부 API 호출을 Wrapper로 감싸 자동 로깅 및 재시도 로직을 구현했습니다."

     @Component
     public class SlackClientWrapper {
         private final SlackClient slackClient;
         private final ExternalApiLogService apiLogService;
         
         @CircuitBreaker(name = "slack", fallbackMethod = "fallback")
         @Retry(name = "slack", maxAttempts = 3)
         public SlackMessageResponse postMessage(SlackMessageRequest request, UUID messageId) {
             long startTime = System.currentTimeMillis();
             
             try {
                 SlackMessageResponse response = slackClient.postMessage(request);
                 
                 // 성공 로그 자동 저장
                 apiLogService.logApiCall(
                     ApiProvider.SLACK,
                     request,
                     response,
                     200,
                     System.currentTimeMillis() - startTime,
                     messageId
                 );
                 
                 return response;
             } catch (Exception e) {
                 // 실패 로그 자동 저장
                 apiLogService.logApiCall(..., 500, ..., messageId);
                 throw e;
             }
         }
     }

기술 포인트

     - Resilience4j: Circuit Breaker + Retry (3회, 지수 백오프)
     - 자동 로깅: 모든 외부 API 호출 이력 저장
     - 비용 추적: Gemini API 사용 비용 계산

   -------------------------------------------------------------------------------

5️⃣ 트러블슈팅

🔥 Issue #76: Codex 리스크 개선 (7개 항목)

1️⃣ 통합 테스트 Mock 설정 누락

문제:

     - 통합 테스트에서 실제 Gemini/Slack API 호출 → 비용 발생 + 테스트 불안정

해결:

     @MockBean
     private GeminiClientWrapper geminiClientWrapper;
     
     @BeforeEach
     void setUp() {
         GeminiResponse geminiResponse = mock(GeminiResponse.class);
         when(geminiResponse.getContent()).thenReturn("2024-12-31 14:00까지 발송 완료 
바랍니다.");
when(geminiClientWrapper.generateContent(any(), any())).thenReturn(geminiResponse);
}

효과: 통합 테스트 4/4 통과, 외부 API 비용 0원

   -------------------------------------------------------------------------------

2️⃣ FeignClient NPE 위험

문제:

     - user-service 타임아웃 시 null 반환 → NPE 가능성

해결:

     @FeignClient(name = "user-service", fallback = UserServiceClientFallback.class)
     public interface UserServiceClient {
         @GetMapping("/api/v1/users/{userId}")
         ApiResponse<UserResponse> getUserById(@PathVariable Long userId);
     }
     
     @Component
     public class UserServiceClientFallback implements UserServiceClient {
         public ApiResponse<UserResponse> getUserById(Long userId) {
             throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, 
                 "User service is temporarily unavailable");
         }
     }

효과: Circuit Breaker 상황에서 명시적 예외 발생, NPE 방지

   -------------------------------------------------------------------------------

3️⃣ Mockito UnnecessaryStubbingExceptio

문제:

     - Entity Mock에서 일부 getter만 사용 → Strict stubbing 에러

해결:

     private Notification createMockNotification() {
         Notification notification = mock(Notification.class);
         
         // lenient() 적용: 조건부 사용되는 stubbing
         lenient().when(notification.getId()).thenReturn(UUID.randomUUID());
         lenient().when(notification.getStatus()).thenReturn(MessageStatus.PENDING);
         
         // 상태 변경 메서드 시뮬레이션
         lenient().doAnswer(invocation -> {
             lenient().when(notification.getStatus()).thenReturn(MessageStatus.SENT);
             return null;
         }).when(notification).markAsSent();
         
         return notification;
     }

효과: 단위 테스트 5/5 통과, UnnecessaryStubbingException 해결

   -------------------------------------------------------------------------------

🔥 PR #75: FeignClient 상태 코드 오류 해결 (전체 MSA 영향)

문제

     - 팀 컨벤션: ApiResponse 항상 200 OK 반환
     - OpenFeign은 HTTP 상태 코드로 성공/실패 판단
     - NotFound 에러도 200 OK → FeignClient가 예외를 잡지 못함

해결

     // Before
     @ExceptionHandler(CustomException.class)
     public ApiResponse<Void> handleCustomException(CustomException e) {
         return ApiResponse.error(e.getErrorCode());
     }
     
     // After
     @ExceptionHandler(CustomException.class)
     public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
         return ResponseEntity
             .status(e.getErrorCode().getHttpStatus())
             .body(ApiResponse.error(e.getErrorCode()));
     }

효과

     - FeignException 정상 발생 (404, 500 등)
     - 전체 MSA 서비스 간 통신의 기반
     - notification-service의 UserServiceClient 정상 동작

   -------------------------------------------------------------------------------

6️⃣ QnA 예상 질문

Q1. Kafka를 사용한 이유는?

A:

     - 비동기 처리: 주문 생성 시 알림 발송 대기 불필요 (빠른 응답)
     - 결합도 감소: order-service가 notification-service에 직접 의존하지 않음
     - 확장성: 알림 발송 실패 시 재시도, DLQ 처리 가능
     - 이벤트 소싱: 주문/배송 이벤트 이력 추적

   -------------------------------------------------------------------------------

Q2. 멱등성(Idempotency)을 어떻게 보장했나?

A:

     - DB UNIQUE 제약조건: event_id 필드에 UNIQUE 설정
     - 애플리케이션 레벨 체크: existsByEventId() 조회 후 skip
     - 효과: 동일 이벤트 중복 발행 시 1개만 처리

     if (repository.existsByEventId(event.eventId())) {
         log.info("⏭️ Event already processed (idempotency)");
         return;
     }

   -------------------------------------------------------------------------------

Q3. 외부 API 호출 실패 시 어떻게 처리했나?

A:

     - Resilience4j Circuit Breaker + Retry:
       - Slack: 3회 재시도 (지수 백오프)
       - Gemini: 2회 재시도
     - 트랜잭션 분리:
       - DB에 FAILED 상태 + 에러 메시지 저장
       - HTTP 500 응답으로 클라이언트에 실패 전달
     - 향후 개선 (Issue #88):
       - DLQ (Dead Letter Queue) 구현
       - 실패 메시지 별도 Topic 저장 후 수동 재처리

   -------------------------------------------------------------------------------

Q4. Gemini API 비용은 얼마나 드나?

A:

     - gemini-2.0-flash-lite 모델: 무료 티어 (1일 1,500 요청)
     - 현재 비용: $0 (개발 환경 기준)
     - 향후 개선:
       - Redis 캐싱 (Issue #87): 동일 경로 요청 재사용
       - Prompt 최적화: 토큰 수 감소

   -------------------------------------------------------------------------------

Q5. 테스트 커버리지는?

A:

     - 단위 테스트: 5/5 (NotificationServiceTest)
     - 통합 테스트: 4/4 (Kafka Consumers)
     - Docker 환경: Kafka 4/4, REST API 10/10
     - 총 63+ 테스트 (기존 58+ → Issue #76에서 5개 증가)

   -------------------------------------------------------------------------------

📊 발표 자료 슬라이드 구성 예시

Slide 1: notification-service 개요

     - 서비스 소개 (포트, 역할)
     - 핵심 기능 3가지 (이벤트 알림, AI 통합, 이력 관리)

Slide 2: 시스템 아키텍처

     - Kafka 플로우 다이어그램
     - ERD (p_notifications, p_external_api_logs)

Slide 3: 핵심 기술 구현 #1 - Kafka Event-Driven

     - 코드 스니펫 (OrderCreatedConsumer)
     - 멱등성 보장 메커니즘

Slide 4: 핵심 기술 구현 #2 - 트랜잭션 분리

     - Before/After 비교
     - Propagation.REQUIRES_NEW 설명

Slide 5: 핵심 기술 구현 #3 - Gemini AI

     - Prompt Engineering 예시
     - 비용 최적화 전략

Slide 6: 트러블슈팅 #1 - Codex 리스크 개선

     - 7개 항목 요약
     - Mock 패턴, Fallback 패턴, lenient() 적용

Slide 7: 트러블슈팅 #2 - FeignClient 오류 해결

     - 문제 상황 (200 OK + error)
     - 해결책 (ResponseEntity 패턴)

Slide 8: 테스트 결과

     - 단위/통합/Docker 테스트 결과
     - 총 63+ 테스트 통과

   -------------------------------------------------------------------------------

💡 발표 Tip

     - 시연 영상 준비: Slack 채널에 실제 알림이 오는 화면
     - 트러블슈팅 강조: Issue #76의 7개 리스크 개선 과정
     - 숫자로 말하기: "63+ 테스트", "7개 리스크 개선", "3단계 트랜잭션 분리"
     - 기술 키워드: "Kafka", "Gemini AI", "Circuit Breaker", "멱등성", "트랜잭션 분리"
     - 팀 기여도: "PR #75는 전체 MSA 통신의 기반이 되었습니다"
