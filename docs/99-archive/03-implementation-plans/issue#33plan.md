notification-service 공통 설정 반영 Issue 생성

Issue 제목: feat: notification-service에 공통 설정 반영 (PR #32)

Issue 내용:

## 📋 작업 개요
PR #32에서 구성된 common-lib의 공통 설정을 notification-service에 반영합니다.

## 🎯 작업 목표
- [ ] common-lib의 JpaAuditConfig 사용 (기존 로컬 Config 제거)
- [ ] common-lib의 SwaggerConfig 사용 (헤더 설정 포함)
- [ ] SecurityConfig 구현 (SecurityConfigBase 상속)
- [ ] Application 클래스에 Config Import 설정
- [ ] 변경사항 테스트 및 검증

## 📦 적용할 공통 설정 (PR #32)

### 1. JpaAuditConfig
- **변경**: `X-Username` 헤더 → UserPrincipal 기반 Auditing
- **위치**: `common-lib/src/main/java/com/oneforlogis/common/config/JpaAuditConfig.java`
- **기능**: SecurityContext에서 UserPrincipal 추출하여 auditor 설정

### 2. SwaggerConfig
- **추가**: X-User-Id, X-User-Name, X-User-Role 헤더 자동 등록
- **위치**: `common-lib/src/main/java/com/oneforlogis/common/config/SwaggerConfig.java`

### 3. SecurityConfigBase
- **추가**: 추상 클래스 상속하여 SecurityConfig 구현
- **위치**:
  `common-lib/src/main/java/com/oneforlogis/common/security/SecurityConfigBase.java`
- **기능**: HeaderAuthFilter 적용, 기본 인증 설정

### 4. UserPrincipal
- **추가**: 사용자 인증 정보 표준화
- **위치**: `common-lib/src/main/java/com/oneforlogis/common/security/UserPrincipal.java`

### 5. Role Enum
- **추가**: MASTER, HUB_MANAGER, DELIVERY_MANAGER, COMPANY_MANAGER
- **위치**: `common-lib/src/main/java/com/oneforlogis/common/model/Role.java`

## 🔧 구현 상세

### 1. 기존 파일 제거
notification-service/src/main/java/com/oneforlogis/notification/
├── infrastructure/config/JpaAuditConfig.java (삭제)
└── global/config/SwaggerConfig.java (삭제 - 현재 빈 파일)

### 2. SecurityConfig 생성
  ```java
  // notification-service/src/main/java/com/oneforlogis/notification/global/config/SecurityCo
  nfig.java
  @Configuration
  public class SecurityConfig extends SecurityConfigBase {
      // 필요시 configureAuthorization 오버라이드
  }

  3. Application 클래스 수정

  @EnableFeignClients
  @EnableDiscoveryClient
  @SpringBootApplication
  @Import({
      com.oneforlogis.common.config.SwaggerConfig.class,
      com.oneforlogis.common.config.JpaAuditConfig.class
  })
  public class NotificationServiceApplication {
      // ...
  }

  ✅ 검증 항목

  - Swagger UI에서 X-User-Id, X-User-Name, X-User-Role 헤더 입력 가능
  - JPA Auditing이 UserPrincipal 기반으로 동작 (createdBy, updatedBy)
  - SecurityFilter가 헤더에서 UserPrincipal 추출하여 SecurityContext 설정
  - 기존 테스트 26개 모두 통과 (TestJpaConfig는 유지)
  - Docker 환경에서 정상 동작

  📚 참고

  - PR #32: https://github.com/14th-anniv/one-for-logis/pull/32
  - Issue #3: hub-service 공통 설정 적용 사례
  - docs/scrum/userpricipalAndHubServicePR32-issue3.md
