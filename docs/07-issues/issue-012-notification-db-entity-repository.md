# Issue #12 - notification-service DB Entity & Repository 구현 리뷰

## 작업 개요

**Branch**: `feature/#12-notification-service-db`
**작업자**: 박근용
**작업 기간**: 2025-11-05 09:00~16:00
**상태**: ✅ 완료 (테스트 100% 통과)

## 작업 내용

notification-service의 도메인 모델(Entity), Repository 계층, 통합 테스트 구현

### 완료 항목

1. ✅ **도메인 모델 ENUM 타입**
   - `SenderType`: USER, SYSTEM
   - `MessageType`: ORDER_NOTIFICATION, DAILY_ROUTE, MANUAL
   - `MessageStatus`: PENDING, SENT, FAILED
   - `ApiProvider`: SLACK, CHATGPT, NAVER_MAPS

2. ✅ **Notification Entity**
   - BaseEntity 상속 (Audit 필드 자동 관리)
   - `@SQLRestriction("deleted_at IS NULL")` - Soft Delete 구현
   - Snapshot 패턴: sender 정보 저장 (username, slack_id, name)
   - `@PrePersist/@PreUpdate` validation 로직
   - 비즈니스 메서드: `markAsSent()`, `markAsFailed()`

3. ✅ **ExternalApiLog Entity**
   - JSONB 필드 지원 (`@JdbcTypeCode(SqlTypes.JSON)`)
   - PostgreSQL JSONB / H2 TEXT 호환 (columnDefinition="TEXT")
   - API 호출 성공/실패 기록 메서드
   - 비용 추적 기능 (cost 필드)

4. ✅ **Repository 계층 (DDD 패턴)**
   - Domain: `NotificationRepository`, `ExternalApiLogRepository` (인터페스)
   - Infrastructure: JPA Repository + RepositoryImpl
   - Soft Delete 구현 (`NotificationRepositoryImpl.deleteById`)

5. ✅ **통합 테스트 작성**
   - `NotificationRepositoryTest`: 15개 테스트
   - `ExternalApiLogRepositoryTest`: 11개 테스트
   - H2 in-memory DB 사용 (@DataJpaTest)
   - TestJpaConfig: AuditorAware 설정

6. ✅ **테스트 환경 설정**
   - `application-test.yml` 생성 (H2 MODE=PostgreSQL)
   - TestJpaConfig: JPA Auditing 활성화
   - H2 dependency 추가 (testImplementation)

7. ✅ **H2 호환성 문제 해결**
   - JSONB → TEXT columnDefinition 변경
   - Soft Delete 테스트: 네이티브 쿼리 사용
   - Validation 테스트: Exception 타입으로 검증

## 기술 스택

- Spring Boot 3.5.7
- Spring Data JPA
- Hibernate 6.x (`@SQLRestriction`)
- PostgreSQL 17 (production)
- H2 Database (test)
- JUnit 5 + AssertJ

## 파일 변경 사항

### 신규 생성

**Domain Model (7개 파일)**
```
notification-service/src/main/java/com/oneforlogis/notification/domain/
├── model/
│   ├── SenderType.java
│   ├── MessageType.java
│   ├── MessageStatus.java
│   ├── ApiProvider.java
│   ├── Notification.java
│   └── ExternalApiLog.java
└── repository/
    ├── NotificationRepository.java
    └── ExternalApiLogRepository.java
```

**Infrastructure (4개 파일)**
```
notification-service/src/main/java/com/oneforlogis/notification/infrastructure/persistence/
├── NotificationJpaRepository.java
├── NotificationRepositoryImpl.java
├── ExternalApiLogJpaRepository.java
└── ExternalApiLogRepositoryImpl.java
```

**Test (3개 파일)**
```
notification-service/src/test/java/com/oneforlogis/notification/
├── config/TestJpaConfig.java
└── infrastructure/persistence/
    ├── NotificationRepositoryTest.java
    └── ExternalApiLogRepositoryTest.java

notification-service/src/test/resources/
└── application-test.yml
```

### 수정
- `notification-service/build.gradle`: H2 test 의존성 추가

## 도메인 모델 설계

### Notification Entity

**테이블**: `p_notifications`
**주요 필드**:
- `message_id` (UUID PK)
- `sender_type` (USER/SYSTEM)
- Sender snapshot: `sender_username`, `sender_slack_id`, `sender_name`
- Recipient: `recipient_slack_id`, `recipient_name`
- `message_content` (TEXT)
- `message_type` (ORDER_NOTIFICATION/DAILY_ROUTE/MANUAL)
- `reference_id` (UUID, 주문/배송 연결)
- `status` (PENDING/SENT/FAILED)
- `sent_at`, `error_message`
- BaseEntity 필드 (created_at, created_by, updated_at, updated_by, deleted_at, deleted_by)

**비즈니스 규칙**:
- USER 타입: sender 정보 필수 (username, slack_id, name)
- SYSTEM 타입: sender 정보 null
- Soft Delete 적용

### ExternalApiLog Entity

**테이블**: `p_external_api_logs`
**주요 필드**:
- `log_id` (UUID PK)
- `api_provider` (SLACK/CHATGPT/NAVER_MAPS)
- `api_method` (메서드명)
- `request_data` (JSONB)
- `response_data` (JSONB)
- `http_status`, `is_success`
- `error_code`, `error_message`
- `duration_ms`, `cost`
- `called_at`
- `message_id` (논리적 FK)

**비즈니스 규칙**:
- 모든 외부 API 호출 자동 기록
- 성공/실패 구분 추적
- API 비용 추적 (선택적)
- Soft Delete 미적용 (로그 테이블)

## Repository 메서드

### NotificationRepository
- `save()`, `findById()`, `findAll()`
- `findByStatus(MessageStatus)`
- `findByMessageType(MessageType)`
- `findByRecipientSlackId(String)`
- `findByReferenceId(UUID)`
- `findBySenderUsername(String)`
- `deleteById(UUID)` - Soft Delete

### ExternalApiLogRepository
- `save()`, `findById()`, `findAll()`
- `findByApiProvider(ApiProvider)`
- `findByIsSuccess(Boolean)`
- `findByMessageId(UUID)`
- `findByCalledAtBetween(LocalDateTime, LocalDateTime)`
- `findByApiProviderAndIsSuccess(ApiProvider, Boolean)`
- `findByApiProviderAndCalledAtBetween(ApiProvider, LocalDateTime, LocalDateTime)`

## 테스트 커버리지

### NotificationRepositoryTest (15개)

**기능 테스트 (9개)**
- USER 타입 알림 저장 및 조회
- SYSTEM 타입 알림 저장
- 상태별 알림 조회
- 메시지 타입별 알림 조회
- 수신자 Slack ID로 알림 조회
- 참조 ID로 알림 조회
- 발신자 사용자명으로 알림 조회
- 알림 발송 성공 처리
- 알림 발송 실패 처리

**Soft Delete 테스트 (2개)**
- deleted_at 필드 확인 (네이티브 쿼리)
- 일반 조회에서 제외 확인 (findAll)

**Validation 테스트 (4개)**
- USER 타입에서 senderUsername 없으면 실패
- USER 타입에서 senderSlackId 없으면 실패
- USER 타입에서 senderName 없으면 실패
- SYSTEM 타입에서 sender 정보 있으면 실패

### ExternalApiLogRepositoryTest (11개)

- API 로그 저장 및 조회
- API 호출 성공 기록
- API 호출 실패 기록
- API 제공자별 로그 조회
- 성공 여부로 로그 조회
- 메시지 ID로 관련 API 로그 조회
- 특정 기간 내 API 로그 조회
- API 제공자 및 성공 여부로 로그 조회
- 특정 기간 내 API 제공자별 로그 조회
- API 호출 비용 설정
- JSONB 필드 저장 및 조회

## 검증 사항

### ✅ 빌드 확인
```bash
./gradlew :notification-service:build
# BUILD SUCCESSFUL
```

### ✅ 테스트 실행 확인
```bash
./gradlew :notification-service:test
# 26개 테스트 모두 통과 (100% 성공률)
```

**결과**:
- NotificationRepositoryTest: 15/15 통과
- ExternalApiLogRepositoryTest: 11/11 통과
- 실패: 0개, 에러: 0개, 스킵: 0개

### ✅ H2 호환성 확인
- JSONB → TEXT columnDefinition 변경 (PostgreSQL과 호환)
- @SQLRestriction 동작 확인 (Soft Delete)
- @PrePersist validation 동작 확인

### ✅ Docker 실행 확인

**컨테이너 상태**:
```bash
docker ps
# oneforlogis-notification-service: Up
# oneforlogis-postgres: Up (healthy)
# oneforlogis-eureka: Up (healthy)
```

**Health Check**:
```bash
curl http://localhost:8700/actuator/health
# {"status":"UP","components":{"db":{"status":"UP"},...}}
```

**데이터베이스 테이블 생성 확인**:
```bash
docker exec oneforlogis-postgres psql -U root -d oneforlogis_notification -c "\dt"
# p_notifications         | table | root
# p_external_api_logs     | table | root
```

**Eureka 등록 확인**:
```bash
curl http://localhost:8761/eureka/apps | grep NOTIFICATION-SERVICE
# ✓ NOTIFICATION-SERVICE 등록됨
```

**테이블 구조 검증**:
- `p_notifications`: 20개 컬럼 (UUID PK, Audit 필드, ENUM 제약조건)
- `p_external_api_logs`: 13개 컬럼 (UUID PK, JSONB → TEXT)
- CHECK 제약조건: sender_type, message_type, status, api_provider

## 주요 이슈 및 해결

### 1. JSONB H2 호환성 문제
- **문제**: `columnDefinition = "jsonb"` H2에서 지원 안 함
- **해결**: `columnDefinition = "TEXT"` 변경
- **영향**: PostgreSQL에서도 Hibernate가 JSONB로 자동 매핑 (`@JdbcTypeCode`)

### 2. @SQLRestriction H2 동작 차이
- **문제**: H2에서 `@SQLRestriction` 적용 시점이 PostgreSQL과 다름
- **해결**: 네이티브 쿼리로 deleted_at 필드 직접 확인하는 테스트 작성

### 3. @PrePersist 타이밍 문제
- **문제**: H2에서 `@PrePersist` 즉시 실행 안 됨
- **해결**: `entityManager.flush()` 강제 호출 + 상위 Exception 타입으로 검증

### 4. Gradle daemon 파일 잠금
- **문제**: Windows에서 build 디렉토리 삭제 실패
- **해결**: `./gradlew --stop` 후 수동 삭제

## 다음 단계

1. Application Layer 구현 (Facade, DTO)
2. Domain Service 구현 (비즈니스 로직)
3. Presentation Layer 구현 (Controller, Request/Response)
4. Feign Client 구현 (Slack, ChatGPT, Naver Maps)
5. 통합 테스트 작성 (API 엔드포인트)
6. AI 기반 출발 시간 계산 로직 구현

## 커밋 예정 이력

1. `feat: add domain model enums for notification-service`
2. `feat: add Notification entity with soft delete`
3. `feat: add ExternalApiLog entity with JSONB support`
4. `feat: add repository interfaces for notification domain`
5. `feat: add repository implementations for notification infrastructure`
6. `test: add NotificationRepositoryTest with 15 test cases`
7. `test: add ExternalApiLogRepositoryTest with 11 test cases`
8. `chore: add H2 test dependency and test configuration`

## 리뷰 포인트

- ✅ DDD 패턴: domain/infrastructure 계층 분리가 적절한가?
- ✅ Soft Delete: BaseEntity + @SQLRestriction 구현이 올바른가?
- ✅ Snapshot 패턴: sender 정보 저장 설계가 감사 추적에 적합한가?
- ✅ Validation: @PrePersist 비즈니스 규칙 검증이 적절한가?
- ✅ JSONB 지원: PostgreSQL/H2 호환성이 보장되는가?
- ✅ 테스트 커버리지: 주요 기능이 모두 테스트되었는가?
- ✅ H2 호환성: 테스트 환경에서 정상 동작하는가?
- ✅ 네이밍: 팀 컨벤션 준수 (entity 필드에 domain prefix 없음)

## 기술적 결정 사항

### 1. Hibernate 6 @SQLRestriction 사용
- Hibernate 5의 `@Where` 대신 Hibernate 6의 `@SQLRestriction` 사용
- Soft Delete 자동 필터링

### 2. JSONB columnDefinition = "TEXT"
- PostgreSQL: Hibernate가 JSONB로 매핑 (dialect 우선)
- H2: TEXT로 저장하고 JSON 직렬화/역직렬화
- 프로덕션과 테스트 환경 호환성 확보

### 3. Repository 계층 분리
- Domain: 인터페이스 (프레임워크 독립성)
- Infrastructure: JPA 구현체
- 도메인 모델이 인프라에 의존하지 않음

### 4. Soft Delete 구현 방식
- BaseEntity의 `markAsDeleted()` 메서드 활용
- Repository에서 `delete()` 대신 `markAsDeleted()` 호출
- `@SQLRestriction`으로 조회 쿼리 자동 필터링

## 참고 문서

- [CLAUDE.md](../../CLAUDE.md)
- [notification-table-spec.md](../personal-plan/notification-table-spec.md)
- [issue-11-notification-service-init.md](./issue-11-notification-service-init.md)

## 성과

- ✅ 26개 테스트 100% 통과 (NotificationRepositoryTest: 15, ExternalApiLogRepositoryTest: 11)
- ✅ PostgreSQL 17 / H2 호환성 확보 (JSONB → TEXT)
- ✅ DDD 패턴 구현 완료 (Domain/Infrastructure 계층 분리)
- ✅ Soft Delete 패턴 검증 완료 (@SQLRestriction)
- ✅ JSONB 필드 저장/조회 검증 완료 (@JdbcTypeCode)
- ✅ Domain validation 로직 검증 완료 (@PrePersist/@PreUpdate)
- ✅ Docker 환경 검증 완료 (테이블 생성, Health Check, Eureka 등록)

## 💬 Review Comments

**dyun23** - ExternalApiLog.java (lines 98-125)
> https://github.com/14th-anniv/one-for-logis/pull/31/files/55d9e366e5225f3c08b7f933c5de8ed59a38f27b#r2493446161
> Builder 활용해서 builder.build();로 구현하면 더 좋을 것 같습니당!

**검토 결과(Claude)**: 엔티티 상태 변경 메서드는 현재 setter 방식 유지
- Builder는 새 객체 생성용으로 JPA 영속성 컨텍스트와 충돌
- 현재 방식이 변경 감지(Dirty Checking)에 적합
- 비즈니스 의미를 명확히 표현하는 도메인 메서드로 적절
