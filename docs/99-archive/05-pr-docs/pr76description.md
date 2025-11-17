## Issue Number
> closed #76

## 📝 Description

PR #68 Codex 리뷰에서 식별된 **7개 리스크 항목** 개선 완료

### 주요 개선 사항

**Priority 1 (Critical)**
1. 통합 테스트 분리 - OrderCreatedConsumerIT, DeliveryStatusChangedConsumerIT Mock 설정 추가
2. user-service NPE 위험 제거 - FeignClient Fallback 구현
3. Slack 실패 시 HTTP 500 응답 - CustomException 발생

**Priority 2 (High)**
1. Gemini messageId 연계 - generateContent()에 messageId 파라미터 추가
2. Slack error 메시지 유실 방지 - 트랜잭션 분리 (DB 저장 + Slack 발송)
3. NotificationService 단위 테스트 - 5/5 통과 (lenient Mock 패턴)
4. Entity 예외 타입 통일 - NotificationException 도메인 예외 생성

**추가 작업**
- JWT 환경 변수 설정 (.env, application.yml)
- Docker 환경 검증 (Kafka 4/4, REST API 10/10)
- 한글 테스트 데이터 지원 (test-data-order-korean.json)

## 🌐 Test Result

### Unit Tests - 5/5 ✅

NotificationServiceTest
- ✅ 주문_알림_발송_성공
- ✅ Slack_전송_실패_시_예외_발생
- ✅ Gemini_AI_호출_실패_시_예외_발생
- ✅ 수동_메시지_발송_성공
- ✅ 수동_메시지_발송_실패_Slack_실패

### Integration Tests - 4/4 ✅

Kafka Consumer Tests
- ✅ order.created 이벤트 처리
- ✅ order.created 멱등성 검증
- ✅ delivery.status.changed 이벤트 처리
- ✅ delivery.status.changed 멱등성 검증

### Docker Environment Tests - 10/10 ✅

REST API Tests (test-notification-api.sh)
- ✅ 주문 알림 발송 (201)
- ✅ 실제 Slack 채널 발송 (201)
- ✅ 수동 메시지 발송 - 권한 없음 (403)
- ✅ 알림 단일 조회 - 권한 없음 (403)
- ✅ 알림 목록 조회 - 권한 없음 (403)
- ✅ API 로그 전체 조회 - 권한 없음 (403)
- ✅ API 로그 Provider별 조회 - 권한 없음 (403)
- ✅ API 로그 메시지별 조회 - 권한 없음 (403)
- ✅ 알림 필터링 조회 - 권한 없음 (403)
- ✅ API 통계 조회 - 권한 없음 (403)

**Total**: 63+ tests (기존 58+ → 5개 증가)

## 🔎 To Reviewer

### 1. 트랜잭션 분리 전략

**위치**: `NotificationService.sendOrderNotification()`

DB 저장 (트랜잭션 내부) → Slack 발송 (트랜잭션 외부) → 실패 시 `Propagation.REQUIRES_NEW`로 별도 저장

**질문**: 트랜잭션 분리 전략이 적절한가? 더 나은 패턴(Event Publishing, Saga)이 필요한가?

### 2. Slack 실패 시 HTTP 응답 코드

**현재**: 500 Internal Server Error 반환

**질문**: 500이 적절한가? 206 Partial Content는 어떤가? (DB 저장 성공, Slack 발송 실패)

### 3. lenient Mock 패턴

**위치**: `NotificationServiceTest.createMockNotification()`

Entity Mock에 `lenient()` 적용하여 UnnecessaryStubbingException 방지

**질문**: 메서드별 `lenient()` vs 클래스 레벨 `@Mock(strictness = Strictness.LENIENT)` 중 선호하는 방식은?

### 4. FeignClient Fallback 예외 처리

**위치**: `UserServiceClientFallback.getUserById()`

Fallback에서 CustomException 발생 (null 반환 안 함)

**질문**: Fallback에서 예외 발생 vs null 반환 중 어느 것이 더 나은가?

### 리뷰 우선순위

1. 🔴 **High**: 트랜잭션 분리 전략
2. 🔴 **High**: Slack 실패 시 HTTP 응답 코드
3. 🟡 **Medium**: lenient Mock 패턴
4. 🟡 **Medium**: FeignClient Fallback 예외 처리
