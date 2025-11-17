# Notification Service API cURL Test Result

**테스트 일시**: 2025-11-07 21:30
**브랜치**: `feature/#14-notification-service-API`
**환경**: Docker Compose (local)

---

## 📋 테스트 개요

Docker 환경에서 notification-service REST API 7개 엔드포인트를 cURL로 직접 테스트.

---

## 🧪 테스트 결과

### Test 1: 주문 알림 발송 (POST /api/v1/notifications/order)

**목적**: Internal API로 order-service에서 호출하는 주문 알림 발송 테스트

**Request**:
```bash
curl -X POST http://localhost:8700/api/v1/notifications/order \
  -H "Content-Type: application/json; charset=UTF-8" \
  -d '{
    "orderId": "550e8400-e29b-41d4-a716-446655440000",
    "ordererInfo": "Kim / kim@test.com",
    "requestingCompanyName": "Supplier Co",
    "receivingCompanyName": "Receiver Co",
    "productInfo": "Test Product x 10",
    "requestDetails": "Fast delivery please",
    "departureHub": "Gyeonggi South",
    "waypoints": ["Daejeon", "Daegu"],
    "destinationHub": "Busan",
    "destinationAddress": "Haeundae-gu, Busan",
    "deliveryPersonInfo": "Hong / U999999",
    "recipientSlackId": "U123456",
    "recipientName": "Manager"
  }'
```

**Response**:
```json
{
  "isSuccess": false,
  "code": 500,
  "message": "could not execute statement [ERROR: new row for relation \"p_external_api_logs\" violates check constraint \"p_external_api_logs_api_provider_check\"...]"
}
```

**Status**: ❌ **FAIL**

**원인**: DB CHECK 제약 조건 불일치
- DB 제약: `CHECK (api_provider IN ('SLACK', 'CHATGPT', 'NAVER_MAPS'))`
- 코드: `ApiProvider.GEMINI` 사용
- Gemini API 호출은 성공했으나 DB 저장 실패

---

### Test 2-7: 인증 필요 API (403 Forbidden)

나머지 6개 엔드포인트는 인증 없이 호출하여 **모두 403 Forbidden** 반환:

| Test | Endpoint | Method | Expected | Actual | Result |
|------|----------|--------|----------|--------|--------|
| 2 | `/manual` | POST | 403 | 403 | ✅ PASS |
| 3 | `/{id}` | GET | 403 | 403 | ✅ PASS |
| 4 | `/` (list) | GET | 403 | 403 | ✅ PASS |
| 5 | `/api-logs` | GET | 403 | 403 | ✅ PASS |
| 6 | `/api-logs/provider/{provider}` | GET | 403 | 403 | ✅ PASS |
| 7 | `/api-logs/message/{id}` | GET | 403 | 403 | ✅ PASS |

---

## 🐛 발견된 문제

### Issue: DB CHECK 제약 조건과 코드 불일치

**증상**:
```
ERROR: new row for relation "p_external_api_logs" violates check constraint "p_external_api_logs_api_provider_check"
Detail: Failing row contains (..., GEMINI, ...)
```

**근본 원인**:
1. **DB 스키마** (`p_external_api_logs` 테이블):
   ```sql
   CONSTRAINT p_external_api_logs_api_provider_check
       CHECK (api_provider IN ('SLACK', 'CHATGPT', 'NAVER_MAPS'))
   ```

2. **코드** (`ApiProvider.java`):
   ```java
   public enum ApiProvider {
       SLACK,
       GEMINI,     // ❌ DB에는 'CHATGPT'로 되어 있음
       NAVER_MAPS
   }
   ```

**영향 범위**:
- 주문 알림 API가 완전히 동작하지 않음
- Gemini API는 정상 호출되지만 로그 저장 실패로 트랜잭션 롤백

**히스토리**:
- Issue #12 (DB Entity 설계) 당시 CHATGPT로 설계
- PR #48 (외부 API Client) 당시 Google Gemini로 변경
- DB 마이그레이션 누락

---

## 🔧 수정 방안

### Option 1: DB 제약 조건 수정 (권장)

**이유**: 코드가 이미 GEMINI로 통일되어 있고, Google Gemini API를 실제 사용 중

**SQL**:
```sql
-- notification-service 데이터베이스에 연결
\c oneforlogis_notification

-- 기존 제약 조건 삭제
ALTER TABLE p_external_api_logs
  DROP CONSTRAINT IF EXISTS p_external_api_logs_api_provider_check;

-- 새 제약 조건 추가 (GEMINI로 변경)
ALTER TABLE p_external_api_logs
  ADD CONSTRAINT p_external_api_logs_api_provider_check
  CHECK (api_provider IN ('SLACK', 'GEMINI', 'NAVER_MAPS'));
```

**실행 방법**:
```bash
# PostgreSQL 컨테이너에 접속
docker exec postgres-ofl psql -U myuser -d oneforlogis_notification

# 또는 SQL 파일로 실행
docker exec -i postgres-ofl psql -U myuser -d oneforlogis_notification < fix-api-provider-check.sql
```

### Option 2: 코드를 CHATGPT로 롤백 (비권장)

**이유**:
- Google Gemini API를 실제 사용 중
- 팀원들이 작성한 코드도 모두 GEMINI로 되어 있음
- 불필요한 대규모 코드 수정

---

## ✅ 수정 후 재테스트 계획

1. **DB 제약 조건 수정** (Option 1)
2. **Docker 컨테이너 재시작**:
   ```bash
   docker-compose -f docker-compose-team.yml restart notification-service
   ```
3. **Test 1 재실행**:
   - 주문 알림 API 호출
   - 200 OK 응답 확인
   - DB에 `p_notifications`, `p_external_api_logs` 레코드 생성 확인
4. **전체 테스트 스크립트 재실행**:
   ```bash
   bash scripts/test-notification-api.sh
   ```

---

## 📊 최종 결과

**현재 상태**:
- ✅ 6/7 테스트 통과 (인증 체크 정상)
- ❌ 1/7 테스트 실패 (DB 제약 조건 불일치)

**수정 후 예상**:
- ✅ 7/7 테스트 통과

---

## 📝 참고 사항

### API 제공자 변경 이력

1. **Issue #12** (2025-11-05): DB Entity 설계 - CHATGPT 사용
2. **PR #48** (2025-11-06): 외부 API Client 구현 - GEMINI로 변경
3. **Issue #14** (2025-11-07): REST API 구현 - GEMINI 유지
4. **현재**: DB 마이그레이션 필요

### Gemini vs ChatGPT 선택 이유

- Google Gemini: 무료 API, JSON mode 지원, 빠른 응답
- OpenAI ChatGPT: 유료, 더 나은 품질 (프로덕션에서 고려)

---

## 🎯 Next Steps

1. ✅ DB 제약 조건 수정 SQL 작성
2. ⏳ DBA 또는 팀 리더 승인 필요 (프로덕션 DB 변경)
3. ⏳ 로컬 환경에서 테스트
4. ⏳ 전체 테스트 통과 후 PR 생성
5. ⏳ 팀 코드 리뷰

---

**작성자**: Claude (assisted)
**문서 위치**: `docs/curl-test-result.md`