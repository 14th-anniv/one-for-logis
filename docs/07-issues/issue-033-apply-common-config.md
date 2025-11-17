# Issue #33 - notification-service 공통 설정 반영 리뷰

## 작업 개요

**Branch**: `fix/#33-apply-common-config`
**작업자**: 박근용
**작업 기간**: 2025-11-05 16:00~19:00
**상태**: ✅ 완료 (테스트 100% 통과, Docker 검증 완료)

## 작업 내용

PR #32에서 구성된 common-lib의 공통 설정을 notification-service에 반영하여 프로젝트 전체의 설정 일관성 확보

### 완료 항목

1. ✅ **기존 Config 파일 제거**
   - `JpaAuditConfig.java` 삭제 (infrastructure/config)
   - `SwaggerConfig.java` 삭제 (global/config, 빈 파일)
   - common-lib의 공통 설정으로 대체

2. ✅ **SecurityConfig 구현**
   - `SecurityConfig.java` 생성 (global/config)
   - `SecurityConfigBase` 추상 클래스 상속
   - HeaderAuthFilter 자동 적용
   - 기본 인증 설정 (Swagger, actuator 허용)

3. ✅ **Application 설정 변경**
   - `NotificationServiceApplication.java`에 `@Import` 추가
   - SwaggerConfig, JpaAuditConfig 명시적 임포트
   - 다른 모듈의 Config를 Spring 빈으로 등록

4. ✅ **의존성 추가**
   - `build.gradle`에 Spring Security 의존성 추가
   - SecurityConfigBase 사용을 위한 필수 의존성

5. ✅ **테스트 설정 수정**
   - `application-test.yml`에 빈 오버라이드 허용
   - TestJpaConfig와 JpaAuditConfig 충돌 해결
   - 26개 테스트 모두 통과 유지

6. ✅ **Docker 환경 검증**
   - JAR 빌드 및 Docker 이미지 재빌드
   - 컨테이너 정상 실행 확인
   - Health Check, Eureka 등록, DB 테이블 확인

## 기술 스택

- Spring Boot 3.5.7
- Spring Security 6.x
- Spring Data JPA
- common-lib (공통 설정 모듈)
- Docker

## 파일 변경 사항

### 삭제 (2개)
```
notification-service/src/main/java/com/oneforlogis/notification/
├── infrastructure/config/JpaAuditConfig.java (삭제)
└── global/config/SwaggerConfig.java (삭제)
```

### 신규 생성 (1개)
```
notification-service/src/main/java/com/oneforlogis/notification/
└── global/config/SecurityConfig.java (신규)
```

### 수정 (3개)
```
notification-service/
├── src/main/java/com/oneforlogis/notification/NotificationServiceApplication.java
├── build.gradle
└── src/test/resources/application-test.yml
```

## 적용된 공통 설정 (PR #32)

### 1. JpaAuditConfig
**위치**: `common-lib/src/main/java/com/oneforlogis/common/config/JpaAuditConfig.java`

**변경 내용**:
- **변경 전**: HTTP 헤더 `X-Username` 직접 추출
  ```java
  String username = req.getHeader("X-Username");
  ```
- **변경 후**: SecurityContext에서 UserPrincipal 추출
  ```java
  if (principal instanceof UserPrincipal userPrincipal) {
      return Optional.of(userPrincipal.username());
  }
  ```

**효과**:
- 인증 정보 표준화 (UserPrincipal 기반)
- createdBy, updatedBy 자동 설정

### 2. SwaggerConfig
**위치**: `common-lib/src/main/java/com/oneforlogis/common/config/SwaggerConfig.java`

**기능**:
- X-User-Id, X-User-Name, X-User-Role 헤더 자동 등록
- Swagger UI에서 헤더 값 입력 가능
- API 테스트 시 사용자 컨텍스트 시뮬레이션

### 3. SecurityConfigBase
**위치**: `common-lib/src/main/java/com/oneforlogis/common/security/SecurityConfigBase.java`

**기능**:
- 추상 클래스로 기본 보안 설정 제공
- HeaderAuthFilter 적용 (Gateway에서 전달받은 헤더 처리)
- 공통 경로 허용: `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/**`
- 나머지 요청은 인증 필요
- 각 서비스에서 `configureAuthorization()` 오버라이드 가능

### 4. UserPrincipal
**위치**: `common-lib/src/main/java/com/oneforlogis/common/security/UserPrincipal.java`

**역할**: 사용자 인증 정보 표준화
**필드**:
- `id` (UUID)
- `username` (String)
- `role` (Role)

**메서드**:
- `isMaster()`: MASTER 권한 확인
- `hasRole(Role)`: 특정 권한 확인
- `getRoleKey()`: Role 키 반환

### 5. Role Enum
**위치**: `common-lib/src/main/java/com/oneforlogis/common/model/Role.java`

**권한 타입**:
- `MASTER`: 마스터 관리자
- `HUB_MANAGER`: 허브 관리자
- `DELIVERY_MANAGER`: 배송 관리자
- `COMPANY_MANAGER`: 업체 관리자

## 코드 변경 상세

### NotificationServiceApplication.java
```java
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
@Import({
        com.oneforlogis.common.config.SwaggerConfig.class,
        com.oneforlogis.common.config.JpaAuditConfig.class
})
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
```

**@Import 사용 이유**:
- `@ComponentScan`은 현재 패키지(`com.oneforlogis.notification`)만 스캔
- common-lib의 Config는 `com.oneforlogis.common` 패키지에 위치
- 다른 모듈의 Config는 자동 감지되지 않으므로 명시적 임포트 필요

### SecurityConfig.java
```java
package com.oneforlogis.notification.global.config;

import com.oneforlogis.common.security.SecurityConfigBase;
import org.springframework.context.annotation.Configuration;

// Spring Security 설정
// common-lib의 SecurityConfigBase를 상속받아 기본 보안 설정 적용
@Configuration
public class SecurityConfig extends SecurityConfigBase {
    // 기본 설정만 사용, 추가 인가 규칙 필요시 configureAuthorization 오버라이드
}
```

### build.gradle
```gradle
dependencies {
    // ... 기존 의존성
    implementation 'org.springframework.boot:spring-boot-starter-security'  // 추가
    // ...
}
```

### application-test.yml
```yaml
spring:
  main:
    allow-bean-definition-overriding: true  # 추가
  # ... 나머지 설정
```

## 테스트 결과

### 단위/통합 테스트
```bash
./gradlew :notification-service:test

BUILD SUCCESSFUL in 27s
26 tests completed, 26 passed (100%)
```

**테스트 구성**:
- `NotificationRepositoryTest`: 15개
- `ExternalApiLogRepositoryTest`: 11개

**검증 항목**:
- ✅ JPA Auditing 동작 (createdBy, updatedBy)
- ✅ Soft Delete 기능
- ✅ 엔티티 Validation
- ✅ JSONB 필드 저장/조회
- ✅ Repository 쿼리 메서드

## Docker 환경 검증

### 빌드 및 실행
```bash
# JAR 빌드
./gradlew :notification-service:build -x test

# Docker 이미지 빌드
docker-compose -f docker-compose-v12.yml build notification-service

# 컨테이너 실행
docker-compose -f docker-compose-v12.yml up -d notification-service
```

### 컨테이너 상태 확인
```bash
docker ps --filter "name=notification"

CONTAINER ID   IMAGE                                STATUS          PORTS
bc24bc56180c   one-for-logis-notification-service   Up 10 seconds   0.0.0.0:8700->8700/tcp
```

### Health Check
```bash
curl http://localhost:8700/actuator/health

{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "discoveryComposite": {"status": "UP"}
  }
}
```

### Eureka 등록 확인
```bash
curl http://localhost:8761/eureka/apps/NOTIFICATION-SERVICE

<status>UP</status>
```

### 데이터베이스 테이블 확인
```bash
docker exec oneforlogis-postgres psql -U root -d oneforlogis_notification -c "\dt"

              List of relations
 Schema |        Name         | Type  | Owner
--------+---------------------+-------+-------
 public | p_external_api_logs | table | root
 public | p_notifications     | table | root
(2 rows)
```

## 기술적 이슈 및 해결

### Issue 1: Spring Security 의존성 누락

**문제**:
```
error: cannot access EnableWebSecurity
  class file for org.springframework.security.config.annotation.web.configuration.EnableWebSecurity not found
```

**원인**:
- SecurityConfigBase를 상속받았지만 notification-service에 Spring Security 의존성이 없음
- common-lib는 compileOnly로 Security를 가지고 있을 수 있음

**해결**:
```gradle
implementation 'org.springframework.boot:spring-boot-starter-security'
```

### Issue 2: Bean 정의 충돌

**문제**:
```
BeanDefinitionOverrideException: Invalid bean definition 'auditorProvider'
```

**원인**:
- TestJpaConfig의 `auditorProvider` 빈
- common-lib JpaAuditConfig의 `auditorProvider` 빈
- 두 개의 동일한 이름의 빈이 충돌

**해결**:
```yaml
spring:
  main:
    allow-bean-definition-overriding: true
```

**설명**:
- 테스트 환경에서는 SecurityContext가 없음
- TestJpaConfig의 simple auditor(`"TEST_USER"`)를 사용해야 함
- 빈 오버라이드를 허용하여 TestJpaConfig가 우선 적용되도록 설정

### Issue 3: Gradle 빌드 캐시 문제

**문제**:
```
Cannot access output property 'destinationDirectory'
java.io.IOException: Cannot snapshot ApiResponse.class: not a regular file
```

**원인**:
- Windows 환경에서 파일 잠금으로 인한 빌드 디렉토리 삭제 실패
- Gradle 캐시 손상

**해결**:
- 사용자가 수동으로 build 디렉토리 삭제
- clean 후 재빌드
- 팀 컨벤션: Windows 파일 잠금 문제로 디렉토리 삭제는 사용자가 직접 수행

## 변경 영향 분석

### 긍정적 영향

1. **설정 일관성**
   - 전체 서비스가 동일한 보안/인증 로직 사용
   - 유지보수 포인트 단일화

2. **유지보수성 향상**
   - 공통 설정 변경 시 common-lib만 수정
   - 각 서비스는 @Import만 업데이트

3. **코드 중복 제거**
   - JpaAuditConfig, SwaggerConfig 중복 제거
   - SecurityConfig 기본 설정 재사용

4. **표준화**
   - UserPrincipal 기반 인증 정보 통일
   - Role Enum으로 권한 관리 표준화

### 주의사항

1. **테스트 설정**
   - 빈 오버라이드 허용으로 인한 혼동 가능성
   - TestJpaConfig가 JpaAuditConfig보다 우선 적용됨을 문서화

2. **의존성 증가**
   - Spring Security 의존성 추가로 JAR 크기 약간 증가
   - 보안 기능을 사용하지 않더라도 의존성 필요

3. **SecurityContext 의존**
   - JPA Auditing이 SecurityContext에 의존
   - Gateway를 거치지 않는 요청은 auditor가 empty일 수 있음

## 다음 단계

1. **Controller 구현**
   - REST API 엔드포인트 개발
   - UserPrincipal 활용한 권한 검증
   - @PreAuthorize 또는 SecurityConfig에서 URL 기반 인가

2. **Swagger 문서화**
   - @Operation, @ApiResponse 어노테이션 추가
   - Request/Response DTO 문서화
   - 헤더 파라미터 설명

3. **권한 검증 추가**
   - SecurityConfig에서 `configureAuthorization()` 오버라이드
   - API별 필요 권한 설정
   - 예: 알림 조회(모든 인증 사용자), 로그 조회(MASTER만)

4. **통합 테스트 작성**
   - @SpringBootTest로 전체 컨텍스트 테스트
   - SecurityContext를 포함한 E2E 테스트
   - MockMvc로 권한 검증 테스트

## 참고 자료

- **PR #32**: https://github.com/14th-anniv/one-for-logis/pull/32 (UserPrincipal 추가 및 공통 설정)
- **Issue #3**: hub-service 공통 설정 적용 사례
- **docs/scrum/userpricipalAndHubServicePR32-issue3.md**: PR #32 상세 설명

## 커밋 메시지

```
fix: notification-service에 공통 설정 반영 (#33)

- JpaAuditConfig, SwaggerConfig를 common-lib로 통합
- SecurityConfig 구현 (SecurityConfigBase 상속)
- Spring Security 의존성 추가
- 테스트 26개 통과, Docker 환경 검증 완료
```

## 💬 Review Comments

**sonaanweb** - NotificationServiceApplication.java
> https://github.com/14th-anniv/one-for-logis/pull/34#pullrequestreview-3421174690
> 지금은 import만으로 충분한 거 같습니다! 문서가 진짜 깔끔하시네요 고생하셨습니다

**검토 결과(Claude)**
```@Import 어노테이션 vs import 문의 차이

import 문 (Java 언어 수준)

// Java import 문 - 단순히 클래스 참조를 위한 것
import com.oneforlogis.common.config.SwaggerConfig;
import com.oneforlogis.common.config.JpaAuditConfig;

public class NotificationServiceApplication {
// 이렇게 해도 Spring이 해당 Config를 빈으로 등록하지 않음!
}
- 역할: 코드에서 클래스 이름을 짧게 쓰기 위한 용도
- 효과: 컴파일러에게 클래스 위치를 알려줄 뿐
- Spring 빈 등록: ❌ 되지 않음

@Import 어노테이션 (Spring 프레임워크 수준)

@Import({
com.oneforlogis.common.config.SwaggerConfig.class,
com.oneforlogis.common.config.JpaAuditConfig.class
})
public class NotificationServiceApplication {
// Spring이 해당 Config 클래스들을 빈으로 등록함!
}
- 역할: Spring에게 특정 설정 클래스를 빈으로 등록하라고 지시
- 효과: Spring ApplicationContext에 Config 빈 등록
- Spring 빈 등록: ✅ 됨
```
