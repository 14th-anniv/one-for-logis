# Issue #11 - notification-service 초기 설정 리뷰

## 작업 개요

**Branch**: `feature/#11-notification-service-init`
**작업자**: 박근용
**작업 기간**: 2025-11-04 12:00~17:30
**상태**: ✅ 완료 (Docker 실행 검증 완료)

## 작업 내용

notification-service의 Spring Boot 프로젝트 초기 설정 및 멀티모듈 구조 통합

### 완료 항목

1. ✅ **멀티모듈 등록**
   - `settings.gradle`에 notification-service 추가

2. ✅ **의존성 설정**
   - Spring Web, JPA, Validation
   - PostgreSQL Driver
   - Eureka Client, OpenFeign
   - common-lib 모듈 의존성

3. ✅ **애플리케이션 설정**
   - `application.yml` 작성 (포트: 8700, DB: notification_db)
   - Eureka 클라이언트 설정
   - JPA 및 PostgreSQL 연결 설정

4. ✅ **메인 클래스 생성**
   - `NotificationServiceApplication.java`
   - `@EnableJpaAuditing`, `@EnableFeignClients`, `@EnableDiscoveryClient` 적용

5. ✅ **DDD 패키지 구조**
   - presentation (controller, request, response, advice)
   - application (dto)
   - domain (model, repository, service, exception)
   - infrastructure (persistence, client, config)
   - global (config, util)

6. ✅ **JPA Auditing 설정**
   - `JpaAuditConfig.java` 생성
   - X-Username 헤더 기반 createdBy/updatedBy 자동 설정

7. ✅ **Docker 설정**
   - `Dockerfile` 생성 (eclipse-temurin:17-jre-alpine 기반)
   - `docker-compose.yml` 생성 (전체 MSA 오케스트레이션)
   - PostgreSQL 초기화 스크립트 작성 (scripts/init-databases.sql)

8. ✅ **문서화**
   - `README.md` 작성 (서비스 개요, 기능, 환경 설정)
   - `docs/DOCKER_GUIDE.md` 작성 (Docker 사용 가이드)

9. ✅ **스켈레톤 파일 생성**
   - 모든 DDD 패키지에 TODO 주석이 포함된 파일 생성
   - Git 추적을 위한 빈 디렉토리 처리

10. ✅ **Actuator 설정**
    - `spring-boot-starter-actuator` 의존성 추가
    - Health check 엔드포인트 활성화
    - Eureka Server에도 actuator 적용

11. ✅ **보안 강화**
    - application.yml에서 하드코딩된 DB 접속 정보 제거
    - 환경변수만 사용하도록 수정 (기본값 제거)

12. ✅ **Docker 실행 검증**
    - PostgreSQL, Eureka Server, notification-service 모두 정상 작동
    - Health check: UP 상태 확인
    - Eureka 등록 확인 완료

## 기술 스택

- Spring Boot 3.5.7
- Spring Cloud (Eureka, OpenFeign)
- PostgreSQL
- JPA/Hibernate
- Lombok
- Docker

## 파일 변경 사항

### 신규 생성
```
notification-service/
├── src/main/java/com/oneforlogis/notification/
│   ├── NotificationServiceApplication.java
│   ├── presentation/
│   │   ├── controller/NotificationController.java
│   │   ├── request/NotificationRequest.java
│   │   ├── response/NotificationResponse.java
│   │   └── advice/NotificationExceptionHandler.java
│   ├── application/
│   │   └── dto/NotificationDto.java
│   ├── domain/
│   │   ├── model/Notification.java
│   │   ├── repository/NotificationRepository.java
│   │   ├── service/NotificationDomainService.java
│   │   └── exception/NotificationException.java
│   ├── infrastructure/
│   │   ├── persistence/NotificationJpaRepository.java
│   │   ├── client/SlackClient.java
│   │   └── config/JpaAuditConfig.java
│   └── global/
│       ├── config/SwaggerConfig.java
│       └── util/AuthContextUtil.java
├── src/main/resources/
│   └── application.yml
├── build.gradle
├── Dockerfile
└── README.md
```

### 수정
- `settings.gradle`: notification-service 모듈 추가

## 다음 이슈에서 구현 예정

- Entity 클래스 구현 (p_notifications, p_external_api_logs)
- Repository, Service, Controller 로직 구현
- Feign Client 구현 (Slack, ChatGPT, Naver Maps)
- Request/Response DTO 구현
- 비즈니스 로직 구현 (주문 알림, AI 기반 출발 시간 계산)

## 주요 설정

### 포트
- **8700** (Eureka에 등록)

### 데이터베이스
- Schema: `notification_db`
- PostgreSQL 15+

### 외부 API 통합 예정
- Slack API
- ChatGPT API
- Naver Maps Directions 5 API

## 검증 사항

### ✅ 빌드 확인
```bash
./gradlew :notification-service:build
# BUILD SUCCESSFUL
```

### ✅ Docker 실행 확인
```bash
docker-compose up -d
# PostgreSQL, Eureka Server, notification-service 모두 UP
```

### ✅ Health Check 확인
```bash
curl http://localhost:8700/actuator/health
# {"status":"UP","components":{"db":{"status":"UP"},...}}
```

### ✅ Eureka 등록 확인
- http://localhost:8761 접속
- NOTIFICATION-SERVICE 인스턴스 UP 상태 확인 완료

## 다음 단계

1. Entity 클래스 구현 (`p_notifications`, `p_external_api_logs`)
2. JPA Repository 구현
3. Domain Service 구현
4. Feign Client 구현 (Slack, ChatGPT, Naver Maps)
5. API 엔드포인트 구현
6. 통합 테스트 작성

## 커밋 이력

1. `init: add notification-service directory structure`
2. `chore: add notification-service to settings.gradle`
3. `build: add notification-service build.gradle`
4. `feat: add notification-service application.yml config`
5. `feat: add NotificationServiceApplication main class`
6. `feat: add DDD package structure for notification-service`
7. `feat: add JpaAuditConfig for notification-service`
8. `build: add Dockerfile for notification-service`
9. `docs: add README.md for notification-service`
10. `feat: add skeleton files for DDD package structure`
11. `build: add docker-compose.yml for MSA orchestration`
12. `build: add actuator dependency for health check`
13. `fix: remove hardcoded credentials from application.yml`
14. `chore: untrack other services application.yml files`

## 리뷰 포인트

- ✅ 멀티모듈 구조에 올바르게 통합되었는가?
- ✅ DDD 패키지 구조가 팀 컨벤션을 따르는가?
- ✅ JPA Auditing이 X-Username 헤더 기반으로 동작하는가?
- ✅ Eureka 클라이언트 설정이 올바른가?
- ✅ 포트 충돌이 없는가? (8700)
- ✅ 환경 변수 기반 설정이 적용되었는가?
- ✅ Docker 환경에서 정상 실행되는가?
- ✅ 보안: DB 접속 정보가 하드코딩되지 않았는가?
- ✅ Health check 엔드포인트가 동작하는가?

## 주요 이슈 및 해결

### 1. Eureka Server actuator 누락
- **문제**: Health check 엔드포인트 404 오류
- **해결**: `spring-boot-starter-actuator` 의존성 추가

### 2. 하드코딩된 DB 접속 정보
- **문제**: application.yml에 `${POSTGRES_USER:root}` 등 기본값 하드코딩
- **해결**: 기본값 제거, 환경변수만 사용

### 3. PostgreSQL 데이터베이스 미생성
- **문제**: init-databases.sql 스크립트가 실행되지 않음
- **해결**: 수동으로 데이터베이스 생성 (`docker exec` 명령어 사용)

### 4. Git tracking 정리
- **문제**: 다른 서비스 application.yml이 추적되고 있음
- **해결**: `git rm --cached`로 untrack 처리 (각 담당자가 구현 예정)

## 참고 문서

- [CLAUDE.md](../../CLAUDE.md)
- [notification-table-spec.md](../personal-plan/notification-table-spec.md)
- [notification-service README.md](../../notification-service/README.md)
