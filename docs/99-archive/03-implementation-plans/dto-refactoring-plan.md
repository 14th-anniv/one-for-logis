# DTO 구조 리팩토링 계획

> **작성일**: 2025-11-10
> **참조**: docs/scrum/turtor-qna-1107.md
> **목적**: 튜터 권장사항 반영 - 단일 클라이언트 환경에서 DTO 구조 개선

---

## 📋 배경

### 튜터 권장사항 (2025-11-07)

**Q3. DTO 위치 관련 질문:**
> 지금 프로젝트처럼 관리자/허브/업체 등 권한 별로 접근 가능한 리소스만 다르고 API 포맷은 동일하게 설계할 경우에는 이것을 단일 클라이언트로 보고 계층 별로 DTO를 나누지 않고 application req/res DTO 하나로 유지해도 될까요?

**튜터 답변:**
> 현재 프로젝트처럼 관리자/허브/업체 등 **권한만 다르고 API 포맷이 동일**하게 설계할 경우, 이는 **단일 클라이언트 유형**으로 볼 수 있어요! 따라서 Presentation 계층과 Application 계층 간의 DTO를 나누지 않고, **Application 계층에 단일 request DTO, response DTO 세트를 두고 유지하는 것이 효율적**입니다.

**결론:**
- ✅ **응용 계층으로 DTO를 옮기는 게 좋을 것 같습니다** (현재 presentation에 DTO 위치)
- 클라이언트 구분: "요청, 응답 포맷이 다르다"로 판단
- 권한별 접근 제어: Controller/Service의 Security/Authorization 레이어에서 처리

---

## 🎯 리팩토링 목표

### 현재 구조 (AS-IS)

```
notification-service/
├── presentation/
│   ├── controller/
│   ├── request/           ← 현재 위치
│   │   ├── ManualNotificationRequest.java
│   │   ├── NotificationRequest.java
│   │   └── OrderNotificationRequest.java
│   └── response/          ← 현재 위치
│       ├── ApiStatisticsResponse.java
│       ├── ExternalApiLogResponse.java
│       └── NotificationResponse.java
└── application/
    ├── service/
    └── dto/               ← 거의 사용 안 됨
        └── NotificationDto.java
```

### 목표 구조 (TO-BE)

```
notification-service/
├── presentation/
│   ├── controller/        ← DTO import 경로만 변경
│   └── advice/
└── application/
    ├── service/
    └── dto/               ← 모든 DTO 통합 위치
        ├── request/
        │   ├── ManualNotificationRequest.java
        │   ├── OrderNotificationRequest.java
        │   └── (NotificationRequest.java 필요시)
        └── response/
            ├── ApiStatisticsResponse.java
            ├── ExternalApiLogResponse.java
            └── NotificationResponse.java
```

---

## 📊 영향 범위 분석

### 1. 이동 대상 파일 (6개)

**Request DTOs** (3개):
- `presentation/request/ManualNotificationRequest.java` → `application/dto/request/`
- `presentation/request/OrderNotificationRequest.java` → `application/dto/request/`
- `presentation/request/NotificationRequest.java` → `application/dto/request/` (또는 삭제)

**Response DTOs** (3개):
- `presentation/response/ApiStatisticsResponse.java` → `application/dto/response/`
- `presentation/response/ExternalApiLogResponse.java` → `application/dto/response/`
- `presentation/response/NotificationResponse.java` → `application/dto/response/`

### 2. Import 경로 변경 필요 파일

**Controller** (1개):
- `NotificationController.java` - 모든 DTO import 경로 업데이트

**Service** (2개):
- `NotificationService.java` - Request/Response DTO import 경로 업데이트
- `ExternalApiLogService.java` - Response DTO import 경로 업데이트

**Test** (1개):
- `NotificationControllerTest.java` - 모든 DTO import 경로 업데이트

### 3. 패키지 정리

**삭제 예정**:
- `presentation/request/` 디렉토리 (이동 후)
- `presentation/response/` 디렉토리 (이동 후)
- `application/dto/NotificationDto.java` (사용되지 않음)

**생성 필요**:
- `application/dto/request/` 디렉토리
- `application/dto/response/` 디렉토리

---

## 🔧 작업 계획

### Phase 1: 디렉토리 구조 준비
1. ✅ `application/dto/request/` 디렉토리 생성
2. ✅ `application/dto/response/` 디렉토리 생성

### Phase 2: DTO 파일 이동
1. Request DTOs 이동 (3개)
2. Response DTOs 이동 (3개)
3. 패키지 선언 변경:
   - `package com.oneforlogis.notification.presentation.request;`
   - → `package com.oneforlogis.notification.application.dto.request;`
   - `package com.oneforlogis.notification.presentation.response;`
   - → `package com.oneforlogis.notification.application.dto.response;`

### Phase 3: Import 경로 업데이트
1. **NotificationController.java**:
   ```java
   // BEFORE
   import com.oneforlogis.notification.presentation.request.*;
   import com.oneforlogis.notification.presentation.response.*;

   // AFTER
   import com.oneforlogis.notification.application.dto.request.*;
   import com.oneforlogis.notification.application.dto.response.*;
   ```

2. **NotificationService.java**:
   ```java
   // BEFORE
   import com.oneforlogis.notification.presentation.request.ManualNotificationRequest;
   import com.oneforlogis.notification.presentation.request.OrderNotificationRequest;
   import com.oneforlogis.notification.presentation.response.NotificationResponse;

   // AFTER
   import com.oneforlogis.notification.application.dto.request.ManualNotificationRequest;
   import com.oneforlogis.notification.application.dto.request.OrderNotificationRequest;
   import com.oneforlogis.notification.application.dto.response.NotificationResponse;
   ```

3. **ExternalApiLogService.java**:
   ```java
   // BEFORE
   import com.oneforlogis.notification.presentation.response.ApiStatisticsResponse;
   import com.oneforlogis.notification.presentation.response.ExternalApiLogResponse;

   // AFTER
   import com.oneforlogis.notification.application.dto.response.ApiStatisticsResponse;
   import com.oneforlogis.notification.application.dto.response.ExternalApiLogResponse;
   ```

4. **NotificationControllerTest.java**:
   ```java
   // BEFORE
   import com.oneforlogis.notification.presentation.request.*;
   import com.oneforlogis.notification.presentation.response.*;

   // AFTER
   import com.oneforlogis.notification.application.dto.request.*;
   import com.oneforlogis.notification.application.dto.response.*;
   ```

### Phase 4: 검증
1. ✅ 컴파일 오류 확인
2. ✅ 테스트 실행 (전체 테스트 통과 확인)
3. ✅ Swagger 문서 정상 생성 확인

### Phase 5: 정리
1. 구 디렉토리 삭제 요청:
   - `presentation/request/`
   - `presentation/response/`
2. 사용되지 않는 파일 삭제:
   - `application/dto/NotificationDto.java`

---

## ✅ 체크리스트

### 준비 사항
- [ ] 현재 브랜치 커밋 완료 (Issue #16)
- [ ] 새 브랜치 생성: `refactor/dto-layer-restructure`
- [ ] 백업 확인

### 작업 단계
- [ ] Phase 1: 디렉토리 구조 준비
- [ ] Phase 2: DTO 파일 이동 (6개)
- [ ] Phase 3: Import 경로 업데이트 (4개 파일)
- [ ] Phase 4: 검증 (컴파일, 테스트, Swagger)
- [ ] Phase 5: 정리 (구 디렉토리 삭제)

### 검증
- [ ] `./gradlew :notification-service:compileJava` 성공
- [ ] `./gradlew :notification-service:test` 성공 (10/10 pass)
- [ ] Swagger UI 정상 작동 확인
- [ ] Controller 메서드 시그니처 변경 없음 확인

---

## 🚨 주의사항

1. **Git 이동 명령어 사용**:
   ```bash
   # Windows 파일 시스템 이슈로 인해 Claude Code에서 직접 파일 이동 불가
   # 사용자가 수동으로 이동하거나 Git 명령어 사용 필요
   git mv presentation/request/ManualNotificationRequest.java application/dto/request/
   ```

2. **패키지 선언 변경**:
   - 파일 이동 후 패키지 선언 반드시 업데이트
   - IDE 자동 import 기능 주의 (구 경로 참조할 수 있음)

3. **테스트 우선**:
   - 각 Phase 완료 후 컴파일 테스트
   - 전체 변경 완료 후 통합 테스트

4. **Swagger 문서**:
   - `@Operation`, `@Schema` 어노테이션은 DTO 위치와 무관
   - 패키지 경로만 변경되므로 Swagger 동작에 영향 없음

---

## 📝 참고 사항

### DDD 계층 별 DTO 역할 (튜터 답변 요약)

1. **DTO 검증 vs 도메인 검증**:
   - **DTO 검증**: 외부 데이터 무결성, 형식 검증 (`@NotBlank`, `@Size`)
   - **도메인 검증**: 불변식(invariant) 보장, 비즈니스 규칙
   - 중복되는 검증은 자연스러움 (목적이 다름)

2. **단일 클라이언트 환경**:
   - 권한만 다르고 API 포맷 동일 → 단일 클라이언트
   - Application 계층에 DTO 통합 배치
   - 권한 제어: Controller/Service의 Security 레이어에서 처리 (`@PreAuthorize`)

3. **다중 클라이언트 환경** (참고):
   - 모바일/웹/관리자 등 포맷이 다를 경우
   - Presentation 계층에 클라이언트별 DTO 분리
   - 예: `MobileUserResponse`, `WebUserResponse`, `AdminUserResponse`

---

## 📅 예상 작업 시간

- **Phase 1-2**: 10분 (디렉토리 생성, 파일 이동)
- **Phase 3**: 15분 (Import 경로 업데이트)
- **Phase 4**: 10분 (검증)
- **Phase 5**: 5분 (정리)
- **총 예상 시간**: 40분

---

## 🔗 관련 문서

- [docs/scrum/turtor-qna-1107.md](turtor-qna-1107.md) - 튜터 Q&A 원본
- [CLAUDE.md](../../CLAUDE.md) - 프로젝트 가이드
- [docs/service-status.md](../service-status.md) - 서비스 구현 상태