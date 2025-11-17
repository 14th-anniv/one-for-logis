# 리팩토링 컴포넌트 가이드

---

## 정적 팩토리 메서드 패턴
- 발견: Issue #33 PR 리뷰 (dyun23 코멘트)
- 파일: `ExternalApiLog.java:79-93`

### 추천 적용 위치
1. **Entity 생성자** (추천)
   - `ExternalApiLog`: `createForApiCall()`, `createForMessage()`
   - `Notification`: `createUserNotification()`, `createSystemNotification()`

2. **DTO/Request 객체** (선택)
   - 생성 의도가 다양한 경우

### 적용 제외
- 상태 변경 메서드 (`recordSuccess()`, `markAsSent()` 등)
- 이유: JPA 변경 감지를 위해 인스턴스 메서드 유지 필요

---
