코드 리뷰 결과

전반적으로 MSA 초기 세팅이 잘 되어있습니다. 다만 몇 가지 개선이 필요한 부분이 있습니다.

✅ 잘된 부분

1. 공통 모듈 구조: common-lib을 통한 공통 로직 분리가 잘 되어 있습니다
2. 예외 처리: GlobalExceptionHandler와 ErrorCode enum을 통한 체계적인 예외 처리
3. 응답 구조: ApiResponse와 PageResponse로 일관된 응답 형식 제공
4. 환경 설정: .env.example과 .gitignore 정리가 잘 되어 있습니다

⚠️ 개선 필요 사항

1. CLAUDE.md 문서와 실제 구조 불일치

CLAUDE.md에는 9개 서비스가 명시되어 있지만, 현재는 3개만 구현되었습니다:
- 현재: user-service, hub-service, order-service
- 문서: auth-service, hub-service, company-service, product-service, order-service, delivery-service, slack-service

권장사항:
- CLAUDE.md를 따르면 user-service → auth-service로 이름 변경 필요
- 또는 문서를 현재 구조에 맞게 수정

2. BaseTimeEntity 필드 누락 ⭐ 중요

common-lib/src/main/java/com/oneforlogis/common/model/BaseTimeEntity.java:1

현재 구현:
protected LocalDateTime createdAt;
protected LocalDateTime updatedAt;
protected LocalDateTime deletedAt;

CLAUDE.md 명세 (Audit Fields):
created_at, created_by, updated_at, updated_by, deleted_at, deleted_by

누락된 필드: createdBy, updatedBy, deletedBy

해결방안: @CreatedBy, @LastModifiedBy 어노테이션 추가 필요

3. JPA Auditing 설정 누락

각 서비스에 @EnableJpaAuditing은 추가되었으나, AuditorAware<String> 빈 설정이 없습니다.

필요한 작업:
@Bean
public AuditorAware<String> auditorProvider() {
return () -> Optional.of(getCurrentUsername()); // JWT에서 추출
}

4. Soft Delete 필터링 미적용

BaseTimeEntity에 markAsDeleted() 메서드는 있지만, 조회 시 자동 필터링이 없습니다.

권장사항:
@Where(clause = "deleted_at IS NULL")  // Hibernate 어노테이션 추가

5. DB 스키마 분리 설정 누락

CLAUDE.md에는 PostgreSQL에서 스키마별 분리(auth_db, hub_db 등)를 명시했으나, 현재 application.yml에는 DB 이름만 지정:

# 현재
url: jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${USER_DB}

명확화 필요:
- 별도 DB 인스턴스 vs 단일 DB 내 스키마 분리
- 스키마 분리 시 spring.jpa.properties.hibernate.default_schema 설정 필요

6. common-lib의 @ComponentScan 문제

GlobalExceptionHandler가 common-lib에 있지만, 각 서비스에서 자동으로 스캔되지 않을 수 있습니다.

해결방안:
- 각 서비스의 @SpringBootApplication에 scanBasePackages 명시
- 또는 common-lib에 @AutoConfiguration 추가

7. Eureka & Gateway 설정 개선

gateway-service/src/main/resources/application.yml:19-22
discovery:
locator:
enabled: true  # 자동 라우팅 활성화

이미 수동 라우트를 정의했으므로, enabled: false로 설정하는 것이 명확합니다.

8. 로그 레벨 설정 누락

각 서비스의 application.yml에 로그 레벨 설정이 없습니다.

추가 권장:
logging:
level:
com.oneforlogis: DEBUG
org.hibernate.SQL: DEBUG

9. Health Check 엔드포인트

문의하신 대로 헬스체크 추가를 권장합니다.

추가할 내용:
management:
endpoints:
web:
exposure:
include: health,info
endpoint:
health:
show-details: always

10. PageResponse.sort 필드 타입

common-lib/src/main/java/com/oneforlogis/common/api/PageResponse.java:31

.sort(pageData.getSort().toString())  // "createdAt: DESC" 형태 문자열

클라이언트에서 파싱하기 어려울 수 있으니, 구조화된 객체로 변경 고려:
private final List<SortInfo> sort;  // {field: "createdAt", direction: "DESC"}

📋 작업 제안 우선순위

High Priority:
1. BaseTimeEntity에 createdBy, updatedBy, deletedBy 필드 추가
2. AuditorAware 빈 설정
3. common-lib ComponentScan 문제 해결
4. Soft delete @Where 필터 추가

Medium Priority:
5. Health check 엔드포인트 추가
6. 로그 레벨 설정
7. DB 스키마 분리 명확화

Low Priority:
8. Gateway discovery locator 설정 정리
9. PageResponse sort 타입 개선
10. CLAUDE.md와 실제 구조 일치

---

1️⃣ 공통 예외 처리 및 응답 구조 일관성 확인

✅ 잘 구성된 부분

- ApiResponse: 성공/실패에 대한 일관된 응답 구조 제공
    - success, code, message, data, timestamp 필드로 표준화
    - @JsonInclude(NON_NULL)로 null 필드 제외 처리
- PageResponse: 페이징 응답 구조 체계적 구성
    - Spring Data의 Page 객체를 DTO로 변환하는 fromPage() 메서드 제공
- GlobalExceptionHandler: 다양한 예외를 포괄적으로 처리
    - CustomException, Validation, BindException, HTTP 메서드 오류 등 처리
    - 로그 레벨 구분 (warn/error)

⚠️ 개선 필요사항

중요: common-lib의 GlobalExceptionHandler가 각 서비스에서 자동으로 스캔되지 않음

문제점:
// common-lib에 있는 @RestControllerAdvice
@RestControllerAdvice
public class GlobalExceptionHandler { ... }

각 서비스 패키지(com.oneforlogis.user, com.oneforlogis.hub 등)는 common-lib의 com.oneforlogis.common 패키지를 자동 스캔하지 않습니다.

해결 방안 (택 1):

방법 1: 각 서비스에 ComponentScan 추가
@SpringBootApplication(scanBasePackages = {
"com.oneforlogis.user",
"com.oneforlogis.common"
})

방법 2: common-lib에 Auto Configuration 추가 (권장)
// common-lib/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.oneforlogis.common.exception.GlobalExceptionHandler

  ---
2️⃣ 각 서비스 DB 설정 및 Eureka 연결 정상 여부 확인

✅ DB 설정 정상

user-service (application.yml:17)
datasource:
url: jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${USER_DB}
username: ${POSTGRES_USER}
password: ${POSTGRES_PASSWORD}

hub-service (application.yml:17)
datasource:
url: jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${HUB_DB}

order-service (application.yml:17)
datasource:
url: jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${ORDER_DB}

- 각 서비스가 별도 DB로 분리됨 (USER_DB, HUB_DB, ORDER_DB)
- JPA 설정 (ddl-auto: update, show_sql: true) 적절
- PostgreSQL 드라이버 정상 설정

✅ Eureka 연결 정상

eureka-server (application.yml:20-24)
eureka:
client:
register-with-eureka: false  # 서버 자체는 등록 안 함
fetch-registry: false

user-service, hub-service, order-service
eureka:
client:
service-url:
defaultZone: ${EUREKA_SERVER_URL}  # http://localhost:8761/eureka

gateway-service (application.yml:40-43)
eureka:
client:
service-url:
defaultZone: ${EUREKA_SERVER_URL}

- 모든 서비스가 Eureka Client 설정 완료
- Application에 @EnableDiscoveryClient 어노테이션 적용됨
- Gateway의 라우팅 설정도 lb:// 프로토콜로 로드밸런싱 지원

의존성 확인:
- spring-cloud-starter-netflix-eureka-server (eureka-server)
- spring-cloud-starter-netflix-eureka-client (나머지 서비스들)

✅ 추가 확인사항

- user-service: Spring Security 포함 (build.gradle:11)
- order-service: OpenFeign + CircuitBreaker 설정 (application.yml:30-33)
- hub-service: Cache 설정 포함 (build.gradle:11)

  ---
3️⃣ 헬스체크 추가 여부

판단: 추가 권장

이유:
1. MSA 환경에서 필수적: 각 서비스의 상태를 모니터링하고 장애 감지에 필수
2. Gateway 라우팅: Gateway가 서비스 health 기반으로 라우팅 결정 가능
3. 운영 관리: 로컬 개발 중에도 서비스 상태 파악이 용이
4. Eureka 연동: Eureka가 health 정보를 기반으로 서비스 가용성 판단

추가 방법:
각 서비스의 build.gradle에 의존성 추가:
implementation 'org.springframework.boot:spring-boot-starter-actuator'

각 서비스의 application.yml에 설정 추가:
management:
endpoints:
web:
exposure:
include: health,info,metrics
endpoint:
health:
show-details: always
probes:
enabled: true  # Kubernetes 사용 시

제공될 엔드포인트:
- GET /actuator/health - 전체 상태
- GET /actuator/health/liveness - 컨테이너 재시작 여부 판단
- GET /actuator/health/readiness - 트래픽 수용 준비 여부

  ---
최종 판정

| 항목           | 상태        | 비고                  |
|--------------|-----------|---------------------|
| 공통 예외 처리 일관성 | ⚠️ 조건부 통과 | ComponentScan 설정 필요 |
| DB 설정        | ✅ 정상      | 3개 서비스 모두 독립 DB 분리  |
| Eureka 연결    | ✅ 정상      | 모든 설정 적절            |
| 헬스체크         | ⚠️ 미구현    | 추가 강력 권장            |

권장 조치사항:
1. GlobalExceptionHandler ComponentScan 문제 해결
2. Actuator 의존성 및 health 엔드포인트 추가
