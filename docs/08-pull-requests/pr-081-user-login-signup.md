# PR #81 리뷰: 로그인 기능 구현

## 📋 PR 정보
- **Issue**: #7
- **작성자**: Sp-PJS
- **제목**: feat: 로그인 기능 구현
- **변경 파일**: 25개 (추가 1,314 / 삭제 16)

## 📝 구현 내용 요약

### 1. Gateway 인증 필터 구현
- JWT 기반 인증 GlobalFilter 추가
- 화이트리스트 경로 관리 (회원가입, 로그인, Swagger, Actuator)
- Access Token 헤더 검증 및 사용자 정보 추출
- 각 서비스로 헤더 전달 (X-User-Id, X-User-Name, X-User-Role)

### 2. User Service 로그인/회원가입 구현
- 회원가입: 중복 검증, 비밀번호 암호화, 승인 대기 상태 (PENDING)
- 로그인: Access Token (Header), Refresh Token (Redis + HttpOnly 쿠키)
- Status Enum 추가: PENDING, APPROVE, REJECTED
- Redis 기반 Refresh Token 관리 및 Blacklist 처리

### 3. JWT 토큰 관리
- Access Token: 30분 만료 (Header 저장)
- Refresh Token: 14일 만료 (Redis + HttpOnly 쿠키 저장)
- JTI (JWT ID) 기반 Blacklist 관리
- 토큰 무효화 로직 구현

---

## 🔴 Critical Issues

### 1. **Security: Gateway에서 WebFlux와 Spring MVC 혼용 문제**
**위치**: `gateway-service/src/main/java/com/oneforlogis/gateway/global/util/JwtUtil.java`

**문제**:
```java
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
```

- Gateway는 **WebFlux 기반**이므로 `jakarta.servlet` 패키지를 사용할 수 없음
- `HttpServletRequest`, `HttpServletResponse`, `Cookie`는 Spring MVC 전용
- Gateway의 `JwtUtil`에서 사용되지 않는 메서드들이지만, 컴파일 에러 발생 가능

**해결책**:
```java
// Gateway JwtUtil에서는 servlet 관련 코드 제거 필요
// ServerWebExchange 기반으로만 동작해야 함

// 제거해야 할 메서드들:
// - getJwtFromHeader(HttpServletRequest)
// - createRefreshTokenCookie(String)
// - deleteCookie(HttpServletResponse, String)
// - extractRefreshTokenFromCookie(HttpServletRequest)
```

**우선순위**: 🔴 **CRITICAL** - 런타임 에러 가능성

---

### 2. **Security: Refresh Token 쿠키 검증 누락**
**위치**: `user-service/src/main/java/com/oneforlogis/user/application/service/UserService.java` - `login()`

**문제**:
- 로그인 시 기존 토큰 무효화 메서드 `invalidatePreviousTokens()` 정의됨
- **하지만 login() 메서드에서 호출되지 않음** → 중복 로그인 시 이전 토큰이 유효하게 남음
- 동일 계정 여러 기기 로그인 시 보안 취약점

**해결책**:
```java
public void login(
    UserLoginRequest request,
    HttpServletRequest httpRequest,
    HttpServletResponse httpResponse) {

    User user = userRepository.findByName(request.name())
        .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_NAME));

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
        throw new CustomException(ErrorCode.INVALID_PASSWORD);
    }

    if(user.getStatus().isPending() || user.getStatus().isRejected()){
        throw new CustomException(ErrorCode.NOT_APPROVED_STATUS);
    }

    // ✅ 이전 토큰 무효화 추가 필요
    String previousAccessToken = JwtUtil.getJwtFromHeader(httpRequest);
    String previousRefreshToken = jwtUtil.extractRefreshTokenFromCookie(httpRequest);
    invalidatePreviousTokens(previousAccessToken, previousRefreshToken, httpResponse);

    // 새 토큰 발급...
}
```

**우선순위**: 🔴 **CRITICAL** - 보안 취약점

---

### 3. **Data Integrity: User 엔티티 필드명 규칙 위반**
**위치**: `user-service/src/main/java/com/oneforlogis/user/domain/model/User.java`

**문제**:
```java
@Column(nullable = false)
private String slack_id;  // ❌ snake_case

@Column(nullable = false)
private String company_name;  // ❌ snake_case
```

- **팀 컨벤션 위반**: Entity 필드는 camelCase 사용, DB 컬럼은 `@Column(name = "...")` 매핑
- 모든 서비스에서 `slackId`, `companyName` 사용 중

**해결책**:
```java
@Column(nullable = false, name = "slack_id")
private String slackId;

@Column(nullable = false, name = "company_name")
private String companyName;
```

**영향도**: 
- FeignClient 응답 DTO 매핑 시 오류 가능성
- JSON 직렬화/역직렬화 불일치
- notification-service의 UserClient 연동 실패 가능

**우선순위**: 🔴 **CRITICAL** - 팀 전체 통신 규칙 위반

---

## ⚠️ Major Issues

### 4. **Architecture: Gateway SecurityConfig의 인증/인가 중복 설정**
**위치**: `gateway-service/src/main/java/com/oneforlogis/gateway/global/cofig/SecurityConfig.java`

**문제**:
```java
.authorizeExchange(exchanges -> exchanges
    .pathMatchers("/api/v1/users/login", "/api/v1/users/signup", ...)
    .permitAll()
)
```

- `JwtAuthenticationGlobalFilter`에 이미 동일한 화이트리스트 존재
- Security와 Filter에서 중복 관리 → 유지보수 어려움

**해결책**:
```java
// Option 1: SecurityConfig에서만 관리
.authorizeExchange(exchanges -> exchanges
    .pathMatchers(WHITELIST_PATHS).permitAll()
    .anyExchange().authenticated()
)

// Option 2: GlobalFilter에서만 관리 + Security는 모든 요청 permitAll
// 팀 정책에 따라 선택
```

**우선순위**: ⚠️ **MAJOR** - 유지보수성

---

### 5. **Security: JwtUtil의 블랙리스트 검증 예외 처리 문제**
**위치**: 
- `gateway-service/src/main/java/com/oneforlogis/gateway/global/util/JwtUtil.java:144`
- `user-service/src/main/java/com/oneforlogis/user/global/util/JwtUtil.java:144`

**문제**:
```java
if (redisService.isTokenBlacklisted(jti)) {
    log.error("Blacklisted Token: {}", jti);
    throw new SecurityException("Blacklisted Token");  // ❌ Exception 타입 불명확
}
```

- `SecurityException`은 JVM Security Manager 관련 예외 (일반적으로 사용하지 않음)
- GlobalExceptionHandler에서 처리되지 않을 가능성
- 사용자에게 적절한 에러 응답 전달 안 됨

**해결책**:
```java
if (redisService.isTokenBlacklisted(jti)) {
    log.error("Blacklisted Token: {}", jti);
    throw new CustomException(ErrorCode.INVALID_TOKEN);
}
```

**우선순위**: ⚠️ **MAJOR** - 에러 처리 일관성

---

### 6. **Configuration: application.yml 하드코딩 및 민감 정보 노출**
**위치**: 
- `user-service/src/main/resources/application.yml`
- `gateway-service/src/main/resources/application.yml`

**문제**:
```yaml
# user-service/application.yml
datasource:
  url: jdbc:postgresql://localhost:5432/oneforlogis_user  # ❌ 하드코딩
  username: postgres
  password:  # ❌ 빈 값

data:
  redis:
    host: localhost  # ❌ 하드코딩
    port: 6379
    password:  # ❌ 빈 값

jwt:
  secret:
    key:  # ❌ 빈 값 (실제 키 값은 어디에?)
  admin:
    token:  # ❌ 빈 값
```

**문제점**:
1. Docker 환경 변수 (`${POSTGRES_HOST}`) 제거 → Docker 실행 불가
2. 빈 패스워드 → 실제 값은 `.env` 파일? (`.gitignore`에 포함되어야 함)
3. JWT Secret Key 누락 → 토큰 생성/검증 불가
4. `spring.config.import`는 있지만 실제 환경 변수 사용 안 함

**해결책**:
```yaml
# application.yml (기본값)
datasource:
  url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5432}/${USER_DB:oneforlogis_user}
  username: ${POSTGRES_USER:postgres}
  password: ${POSTGRES_PASSWORD:}

data:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}

jwt:
  secret:
    key: ${JWT_SECRET_KEY}  # 필수값
  admin:
    token: ${JWT_ADMIN_TOKEN}  # 필수값
```

```properties
# .env.example (Git 커밋)
JWT_SECRET_KEY=example-base64-encoded-secret-key
JWT_ADMIN_TOKEN=example-admin-token
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
REDIS_HOST=localhost
REDIS_PORT=6379
```

**우선순위**: ⚠️ **MAJOR** - 설정 관리 + 보안

---

### 7. **Code Quality: ErrorCode 코드 스타일 불일치**
**위치**: `common-lib/src/main/java/com/oneforlogis/common/exception/ErrorCode.java`

**문제**:
```java
// 기존 코드: Pascal Case
HUB_NOT_FOUND(HttpStatus.NOT_FOUND, "허브를 찾을 수 없습니다."),

// 새로 추가된 코드: 들여쓰기 탭(Tab) 사용
	DUPLICATE_USERNAME(HttpStatus.CONFLICT, "이미 존재하는 아이디입니다."),
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다."),
```

**문제점**:
1. 기존 코드는 스페이스 4칸, 새 코드는 탭 사용
2. 주석 `// user` 소문자 → 다른 섹션은 `// Hub`, `// Delivery` 대문자

**해결책**:
```java
// User
DUPLICATE_USERNAME(HttpStatus.CONFLICT, "이미 존재하는 아이디입니다."),
DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다."),
DUPLICATE_SLACK_ID(HttpStatus.CONFLICT, "이미 존재하는 슬랙 아이디입니다."),

NOT_FOUND_NAME(HttpStatus.NOT_FOUND, "아이디가 존재하지 않습니다."),
INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
NOT_APPROVED_STATUS(HttpStatus.UNAUTHORIZED, "관리자의 승인을 기다려주세요.");
```

**우선순위**: ⚠️ **MINOR** - 코드 스타일 일관성

---

## 💡 Minor Issues & Suggestions

### 8. **Validation: 회원가입 Request 검증 부족**
**위치**: `user-service/src/main/java/com/oneforlogis/user/presentation/request/UserSignupRequest.java`

**문제**:
```java
@Schema(description = "슬랙 아이디", example = "U24CAKY1N2O")
@NotBlank(message = "Slack ID는 필수 입력 항목입니다.")
String slack_id,
```

- Slack User ID 형식 검증 없음 (일반적으로 `U` + 10자리 대문자 영숫자)
- `roleAuthKey`는 선택 사항인데 MASTER 회원가입 시에만 필수 → Bean Validation으로 검증 불가

**제안**:
```java
@Pattern(regexp = "^U[A-Z0-9]{10}$", message = "유효하지 않은 Slack ID 형식입니다.")
String slackId,
```

**우선순위**: 💡 **SUGGESTION**

---

### 9. **Code Quality: 불필요한 파일 추가**
**위치**: `user-service/src/main/java/com/oneforlogis/user/domain/service/UserService.java`

**문제**:
```java
package com.oneforlogis.user.domain.service;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserService {
}
```

- **빈 클래스** → 사용되지 않음
- `application.service.UserService`와 중복 (패키지 다름)

**제안**:
- 삭제 권장 (DDD 패턴에서 domain service가 필요하면 그때 추가)

**우선순위**: 💡 **MINOR**

---

### 10. **Naming: Gateway 패키지명 오타**
**위치**: `gateway-service/src/main/java/com/oneforlogis/gateway/global/cofig/`

**문제**:
- `cofig` → `config` 오타

**제안**:
```
global/cofig/ → global/config/
```

**우선순위**: 💡 **TYPO**

---

### 11. **Documentation: Gateway application.yml 라우팅 주석 필요**
**위치**: `gateway-service/src/main/resources/application.yml`

**제안**:
```yaml
routes:
  # 업체 관리 서비스 (8300)
  - id: company-service
    uri: lb://company-service
    predicates:
      - Path=/api/v1/companies/**

  # 허브 관리 서비스 (8200)
  - id: hub-service
    uri: lb://hub-service
    predicates:
      - Path=/api/v1/hubs/**
```

**우선순위**: 💡 **DOCUMENTATION**

---

### 12. **Security: Common SecurityConfigBase 세션 관리 설정 위치 검토**
**위치**: `common-lib/src/main/java/com/oneforlogis/common/security/SecurityConfigBase.java`

**문제**:
```java
.sessionManagement(sessionManagement ->  // 세션 비활성화 -> JWT 사용
    sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

- 주석 스타일: `//` 권장 (팀 컨벤션)
- 모든 서비스에 적용되는 설정이므로 위치는 적절함

**제안**:
```java
// 세션 비활성화 (JWT 기반 인증)
.sessionManagement(sessionManagement ->
    sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

**우선순위**: 💡 **STYLE**

---

## 📊 리뷰 통계
- **Critical Issues**: 3개 (Gateway WebFlux 혼용, 토큰 무효화 누락, Entity 필드명 규칙 위반)
- **Major Issues**: 4개 (인증 중복 설정, 블랙리스트 예외 처리, 설정 하드코딩, 코드 스타일)
- **Minor Issues**: 5개 (검증 부족, 불필요한 파일, 패키지명 오타, 문서화, 스타일)

---

## ✅ 수정 우선순위

### Phase 1 (즉시 수정 필요)
1. **Gateway JwtUtil에서 Servlet 코드 제거** (WebFlux 호환성)
2. **User Entity 필드명 camelCase 변경** (팀 전체 통신 규칙)
3. **login() 메서드에서 이전 토큰 무효화 호출 추가** (보안)
4. **application.yml 환경 변수 복원** (Docker 실행)

### Phase 2 (리팩토링)
5. Gateway SecurityConfig와 GlobalFilter 인증 로직 통합
6. 블랙리스트 예외를 CustomException으로 변경
7. ErrorCode 스타일 통일 (들여쓰기, 주석)
8. 불필요한 파일 삭제 (domain/service/UserService.java)

### Phase 3 (선택)
9. Slack ID 형식 검증 추가
10. 패키지명 오타 수정 (cofig → config)
11. YAML 주석 추가

---

## 💬 To Reviewer 질문에 대한 답변

> **Gateway의 Application.yml 파일 보시면 각 서비스로 라우팅 하는 설정이 있는데 추가 안되거나 잘못된 부분이 있는지**

### 현재 라우팅 설정 분석

```yaml
routes:
  - id: company-service
    uri: lb://company-service
    predicates:
      - Path=/api/v1/companies/**

  - id: hub-service
    uri: lb://hub-service
    predicates:
      - Path=/api/v1/hubs/**

  - id: notification-service
    uri: lb://notification-service
    predicates:
      - Path=/api/v1/notifications/**

  - id: order-service
    uri: lb://order-service
    predicates:
      - Path=/api/v1/orders/**

  - id: product-service
    uri: lb://product-service
    predicates:
      - Path=/api/v1/products/**

  - id: user-service
    uri: lb://user-service
    predicates:
      - Path=/api/v1/users/**
```

### 분석 결과

✅ **정상적으로 설정됨**:
- 모든 서비스 라우팅 포함 (6개 서비스)
- Eureka 서비스명과 일치 (`lb://` 프리픽스 사용)
- 경로 패턴 일관성 있음 (`/api/v1/{domain}/**`)

❌ **누락된 라우팅**:
```yaml
# 배송 서비스 누락
- id: delivery-service
  uri: lb://delivery-service
  predicates:
    - Path=/api/v1/deliveries/**
```

### 기타 개선 사항

1. **라우팅 순서**: 특정 경로가 우선순위를 가져야 한다면 순서 조정 필요
2. **Filters 추가 고려**:
   ```yaml
   - id: user-service
     uri: lb://user-service
     predicates:
       - Path=/api/v1/users/**
     filters:
       - RewritePath=/api/v1/users/(?<segment>.*), /$\{segment}
   ```
3. **Rate Limiting, Circuit Breaker 필터 추가 고려** (향후 개선)

---

## 🎯 종합 평가

### 👍 잘된 점
1. **JWT 기반 인증 아키텍처** 잘 설계됨 (Access + Refresh Token)
2. **Redis 기반 토큰 관리** (Blacklist, Refresh Token 저장)
3. **승인 대기 시스템** (PENDING → APPROVE 프로세스)
4. **화이트리스트 기반 인증 스킵** 구현
5. **Swagger 통합** (회원가입/로그인 API 문서화)

### 🔧 개선 필요
1. **Gateway WebFlux vs Servlet 혼용 문제 해결** (Critical)
2. **Entity 필드명 팀 컨벤션 준수** (Critical)
3. **토큰 무효화 로직 호출 누락** (Critical)
4. **환경 변수 설정 복원** (Major)
5. **인증 로직 중복 제거** (Major)

### 추천 Action Items
```markdown
- [ ] Gateway JwtUtil Servlet 코드 제거
- [ ] User Entity 필드명 변경 (slackId, companyName)
- [ ] login() 메서드에 invalidatePreviousTokens() 호출 추가
- [ ] application.yml 환경 변수 복원
- [ ] delivery-service 라우팅 추가
- [ ] SecurityException → CustomException 변경
- [ ] 불필요한 파일 삭제 (domain.service.UserService)
- [ ] ErrorCode 스타일 통일
```

---

**리뷰 작성일**: 2025-11-11  
**리뷰어**: Claude (notification-service 담당)
