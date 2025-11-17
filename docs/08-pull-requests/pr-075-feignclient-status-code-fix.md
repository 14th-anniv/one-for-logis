# PR #75: FeignClient 상태 코드 오류 해결 및 Hub-Company Client 통신

## Issue Number
> closed #74

## 📝 Description

### FeignClient HTTP 상태 코드 처리 개선
- **핵심 문제**: OpenFeign이 HTTP 상태 코드를 기준으로 성공/실패 판단하는데, 팀 컨벤션 `ApiResponse`가 항상 200 OK 반환
- **GlobalExceptionHandler 수정**: `ResponseEntity` 반환으로 실제 HTTP 상태 코드 반영 (404, 400, 500 등)
- **HubClient 매핑 수정**: `ApiResponse<HubResponse>` 반환 타입으로 변경
- **Company Controller 상태 코드 통일**: 200, 201, 204 적용

### 문제 상황 1: Company 생성 시 Hub 검증 실패
```
company 생성 (유효한 hubId 필요) 
  → hub Client 호출로 hubId 검증 
  → ⚠️ 검증 실패 (NotFound도 200 OK 처리됨)
```

**원인**:
- OpenFeign은 HTTP 상태 코드로 성공/실패 판단
- 팀 컨벤션 `ApiResponse`는 응답 바디에만 성공/실패 정보 포함 (HTTP는 항상 200 OK)
- NotFound 에러도 바디에만 담기고 HTTP 상태는 200 OK → 예외를 잡지 못함

**해결**:
- GlobalExceptionHandler에 `ResponseEntity` 반환 추가
- 에러 발생 시 실제 HTTP 상태 코드 반환 (404 Not Found, 400 Bad Request 등)

### 문제 상황 2: Hub 데이터가 null로 조회됨
```
Client 통신 테스트 중 기능은 정상 작동하는데 로그에는 허브 데이터가 null로 찍힘
```

**원인**:
- Hub API는 `ApiResponse<HubResponse>` 형태로 응답
- HubClient는 `HubResponse`로 매핑 → 내부 `data` 필드가 매핑되지 않음

**해결**:
- HubClient 반환 타입을 `ApiResponse<HubResponse>`로 변경
- 서비스 레이어에서 `.data()`를 통해 실제 Hub 데이터 접근

## 📊 변경 사항

### 변경 파일 (3개 파일, +104/-42)

#### Common-lib
- **GlobalExceptionHandler**: 
  - 모든 예외 핸들러를 `ApiResponse<Void>` → `ResponseEntity<ApiResponse<Void>>` 반환으로 변경
  - 실제 HTTP 상태 코드 반영: `new ResponseEntity<>(response, status)`
  - 주석 추가: "HTTP 상태 코드가 실제 Response에 반영되도록"

**변경된 핸들러**:
- `handleBusinessException()`: CustomException 처리
- `handleMethodArgumentNotValid()`: 요청 값 유효성 검증 실패
- `handleBindException()`: 파라미터 바인딩 오류
- `handleMethodNotSupported()`: HTTP 메서드 지원 안됨
- `handleNotFound()`: 핸들러 없음 (404)
- `handleAccessDeniedException()`: 권한 없음
- `handleException()`: 그 외 서버 에러

#### Company Service
- **HubClient**: 
  - 반환 타입: `HubResponse` → `ApiResponse<HubResponse>` 변경
  - `@GetMapping("/{hubId}")` 엔드포인트 매핑

- **HubResponse**: 
  - FeignClient 응답 바인딩 DTO 추가
  - `record` 타입으로 불변성 보장
  - 필드: `id`, `name`, `address`, `lat`, `lon`

- **CompanyService**: 
  - Hub 검증: `hubClient.getHub(hubId).data()` - `.data()` 추가
  - 디버깅 로그 추가: `log.info("등록하는 업체 허브 ID: {} ({})", hub.name(), hub.id())`

- **CompanyController**: 
  - HTTP 상태 코드 통일 (200, 201, 204)
  - `createCompany()`: `ResponseEntity.status(HttpStatus.CREATED)` 유지
  - `updateCompany()`: `ResponseEntity` 제거 → `ApiResponse` 직접 반환
  - `deleteCompany()`: `ResponseEntity.noContent().build()` 반환 (204 No Content)
  - `getCompanyDetail()`: `ResponseEntity` 제거 → `ApiResponse` 직접 반환
  - `getCompanies()`: `ResponseEntity` 제거 → `ApiResponse` 직접 반환

## 🌐 Test Result

### Hub Client 통신 테스트
- Company 생성 시 유효하지 않은 hubId 입력 → 404 Not Found 정상 반환
- 유효한 hubId 입력 → Hub 정보 정상 조회 및 Company 생성 성공
- 로그에 Hub 정보 정상 출력 확인

## 🔍 코드 리뷰 결과

### ✅ 잘된 점

#### 1. 핵심 문제 정확히 파악 (⭐⭐⭐⭐⭐)
- OpenFeign의 HTTP 상태 코드 기반 처리 메커니즘 이해
- 팀 컨벤션 `ApiResponse`와의 충돌 지점 정확히 파악
- **전체 MSA 통신에 필수적인 개선**

#### 2. GlobalExceptionHandler 개선 우수
- `ResponseEntity` 반환으로 실제 HTTP 상태 코드 반영
- 모든 예외 핸들러에 일관되게 적용
- 주석으로 변경 의도 명확히 전달

#### 3. FeignClient DTO 매핑 수정
- `ApiResponse<HubResponse>` 반환 타입으로 올바르게 변경
- 서비스 레이어에서 `.data()` 접근으로 안전하게 처리

#### 4. 문제 해결 과정 문서화
- PR Description에 문제 상황, 원인, 해결 과정 상세히 기록
- 다른 서비스에서도 참고 가능하도록 공유

### 🚨 Critical Issues (필수 수정)

#### 1. Controller 응답 타입 혼용 (심각도: 매우 높음)

**현재 코드** (`CompanyController.java`):
```java
// ❌ 혼재된 반환 타입
@PostMapping
public ResponseEntity<ApiResponse<CompanyCreateResponse>> createCompany(...) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
}

@PatchMapping("/{companyId}")
public ApiResponse<CompanyUpdateResponse> updateCompany(...) {
    return ApiResponse.success(response);
}

@DeleteMapping("/{companyId}")
public ResponseEntity<ApiResponse<Void>> deleteCompany(...) {
    return ResponseEntity.noContent().build();
}

@GetMapping("/{companyId}")
public ApiResponse<CompanyDetailResponse> getCompanyDetail(...) {
    return ApiResponse.success(response);
}
```

**문제점**:
- 동일 Controller 내에서 응답 타입이 일관되지 않음
- `ResponseEntity<ApiResponse<T>>`, `ApiResponse<T>`, `ResponseEntity<Void>` 혼용
- API 사용자 입장에서 혼란 발생
- **팀 전체 표준화 필요**

**해결 방안**:

**Option 1: ApiResponse만 사용 (현재 팀 컨벤션 유지)**
```java
// 모든 엔드포인트에서 ApiResponse만 반환 (HTTP 상태는 항상 200 OK)
@PostMapping
public ApiResponse<CompanyCreateResponse> createCompany(...) {
    var response = companyService.createCompany(request);
    return ApiResponse.created(response); // 200 OK + message: "정상 등록 되었습니다."
}

@PatchMapping("/{companyId}")
public ApiResponse<CompanyUpdateResponse> updateCompany(...) {
    return ApiResponse.success(response); // 200 OK
}

@DeleteMapping("/{companyId}")
public ApiResponse<Void> deleteCompany(...) {
    companyService.deleteCompany(companyId, userPrincipal.username());
    return ApiResponse.noContent(); // 200 OK + message: "정상 처리 되었습니다."
}
```

**장점**:
- 응답 형식 통일
- ApiResponse 래퍼로 일관된 구조
- FeignClient 호출 시 항상 `.data()` 접근 패턴 일관성

**단점**:
- REST 표준과 다름 (201 Created, 204 No Content 미사용)
- HTTP 상태 코드만으로 성공/실패 판단 불가

**Option 2: ResponseEntity + ApiResponse 사용 (권장 ⭐)**
```java
// HTTP 상태 코드를 명시적으로 반환 (PR #75 GlobalExceptionHandler 수정 취지와 일치)
@PostMapping
public ResponseEntity<ApiResponse<CompanyCreateResponse>> createCompany(...) {
    var response = companyService.createCompany(request);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.success(response)); // 201 Created
}

@PatchMapping("/{companyId}")
public ResponseEntity<ApiResponse<CompanyUpdateResponse>> updateCompany(...) {
    var response = companyService.updateCompany(companyId, request);
    return ResponseEntity.ok(ApiResponse.success(response)); // 200 OK
}

@DeleteMapping("/{companyId}")
public ResponseEntity<ApiResponse<Void>> deleteCompany(...) {
    companyService.deleteCompany(companyId, userPrincipal.username());
    return ResponseEntity.ok(ApiResponse.noContent()); // 200 OK + message
}

// 또는 204 No Content (바디 없음)
@DeleteMapping("/{companyId}")
public ResponseEntity<Void> deleteCompany(...) {
    companyService.deleteCompany(companyId, userPrincipal.username());
    return ResponseEntity.noContent().build(); // 204 No Content (바디 없음)
}

@GetMapping("/{companyId}")
public ResponseEntity<ApiResponse<CompanyDetailResponse>> getCompanyDetail(...) {
    var response = companyService.getCompanyDetail(companyId);
    return ResponseEntity.ok(ApiResponse.success(response)); // 200 OK
}
```

**장점**:
- REST 표준 준수 (201, 204 등 적절한 상태 코드 사용)
- HTTP 상태 코드만으로도 성공/실패 판단 가능 (FeignClient 친화적)
- GlobalExceptionHandler 수정 취지와 일치 (에러도 상태 코드 반영)

**단점**:
- 응답 코드 중복 (ApiResponse 내부 code + HTTP 상태 코드)
- FeignClient 호출 시 `.body().data()` 패턴 필요

**권장**: **Option 2 채택** (GlobalExceptionHandler 수정 취지와 일치)

#### 2. DELETE 메서드 응답 불일치 (심각도: 높음)

**현재 코드**:
```java
@DeleteMapping("/{companyId}")
public ResponseEntity<ApiResponse<Void>> deleteCompany(...) {
    companyService.deleteCompany(companyId, userPrincipal.username());
    return ResponseEntity.noContent().build(); // ❌ 204 No Content (body 없음)
}
```

**문제점**:
- 반환 타입: `ResponseEntity<ApiResponse<Void>>` (body 있음)
- 실제 반환: `ResponseEntity.noContent().build()` (body 없음, 204 No Content)
- 타입과 실제 응답 불일치

**해결책**:
```java
// Option 1: 팀 표준 ApiResponse 사용 (200 OK)
@DeleteMapping("/{companyId}")
public ResponseEntity<ApiResponse<Void>> deleteCompany(...) {
    companyService.deleteCompany(companyId, userPrincipal.username());
    return ResponseEntity.ok(ApiResponse.noContent()); // 200 OK + message
}

// Option 2: REST 표준 204 No Content
@DeleteMapping("/{companyId}")
public ResponseEntity<Void> deleteCompany(...) {
    companyService.deleteCompany(companyId, userPrincipal.username());
    return ResponseEntity.noContent().build(); // 204 No Content (body 없음)
}
```

#### 3. HubClient 예외 처리 개선 (심각도: 중간)

**현재 코드** (`CompanyService.java`):
```java
public HubResponse fetchHub(UUID hubId) {
    try {
        return hubClient.getHub(hubId).data();
    } catch (FeignException.NotFound e) {
        throw new CustomException(ErrorCode.HUB_NOT_FOUND);
    }
    // 다른 FeignException은 처리되지 않음 (타임아웃, 네트워크 오류 등)
}
```

**문제점**:
- `FeignException.NotFound`만 처리
- 타임아웃, 네트워크 오류, 500 에러 등 미처리 → 예외 전파
- Service 계층에서 직접 FeignClient 호출 (계층 분리 위반)

**권장 수정**:
```java
// infrastructure.client 패키지에 Adapter 생성
package com.oneforlogis.company.infrastructure.client;

@Component
@RequiredArgsConstructor
public class HubClientAdapter {
    private final HubClient hubClient;
    
    public HubResponse getHub(UUID hubId) {
        try {
            ApiResponse<HubResponse> response = hubClient.getHub(hubId);
            if (response.data() == null) {
                throw new CustomException(ErrorCode.HUB_NOT_FOUND);
            }
            return response.data();
        } catch (FeignException.NotFound e) {
            log.warn("Hub not found: {}", hubId);
            throw new CustomException(ErrorCode.HUB_NOT_FOUND);
        } catch (FeignException.ServiceUnavailable | FeignException.InternalServerError e) {
            log.error("Hub service unavailable: {}", e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_API_FAILED);
        } catch (FeignException e) {
            log.error("FeignClient error: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.EXTERNAL_API_FAILED);
        }
    }
}

// CompanyService 수정
@RequiredArgsConstructor
public class CompanyService {
    private final HubClientAdapter hubClientAdapter; // FeignClient 직접 사용 X
    
    @Transactional
    public CompanyCreateResponse createCompany(CompanyCreateRequest request) {
        HubResponse hub = hubClientAdapter.getHub(request.hubId()); // Adapter 사용
        log.info("등록하는 업체 허브 ID: {} ({})", hub.name(), hub.id());
        // ...
    }
}
```

**장점**:
- FeignClient 예외 처리 로직 캡슐화
- Service 계층의 FeignClient 의존성 제거 (DIP 원칙)
- 다른 서비스에서도 재사용 가능한 Adapter 패턴

### ⚠️ 개선 권장 사항

#### 1. GlobalExceptionHandler 주석 스타일 통일

**현재 코드**:
```java
/**
 * HTTP 상태 코드가 실제 Response에 반영되도록
 */
// CustomException 처리
@ExceptionHandler(CustomException.class)
protected ResponseEntity<ApiResponse<Void>> handleBusinessException(...)
```

**문제점**:
- JavaDoc(`/** */`) + 단일 주석(`//`) 혼용
- 팀 컨벤션: `//` 단일 주석만 사용 (JavaDoc은 public API만)

**권장 수정**:
```java
// HTTP 상태 코드가 실제 Response에 반영되도록
// CustomException 처리
@ExceptionHandler(CustomException.class)
protected ResponseEntity<ApiResponse<Void>> handleBusinessException(...)
```

#### 2. 디버깅 로그 정리

**현재 코드** (`CompanyService.java`):
```java
log.info("등록하는 업체 허브 ID: {} ({})", hub.name(), hub.id());
```

**권장**:
- 개발 단계에서는 유용하나, 프로덕션 배포 전 제거 또는 `log.debug()`로 변경
- INFO 레벨은 운영상 중요한 이벤트만 (생성, 수정, 삭제 완료 등)

```java
// Option 1: 삭제
// 디버깅 완료 후 제거

// Option 2: DEBUG 레벨로 변경
log.debug("등록하는 업체 허브 ID: {} ({})", hub.name(), hub.id());

// Option 3: 운영 로그로 의미 있게 변경
log.info("업체 생성 완료: companyId={}, hubId={}, name={}", 
    savedCompany.getId(), hub.id(), request.name());
```

#### 3. ErrorCode 추가 필요

**현재**: `ErrorCode.HUB_NOT_FOUND`, `ErrorCode.EXTERNAL_API_FAILED`가 있는지 확인 필요

**추가 권장**:
```java
// common-lib ErrorCode.java
// Company
COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "업체를 찾을 수 없습니다."),
COMPANY_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 업체입니다."),

// External API
EXTERNAL_API_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "외부 API 호출에 실패했습니다."),
EXTERNAL_API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "외부 API 호출 시간이 초과되었습니다."),

// Hub (확인 필요)
HUB_NOT_FOUND(HttpStatus.NOT_FOUND, "허브를 찾을 수 없습니다."),
```

#### 4. HubClient Configuration 추가

**현재**: 기본 Feign 설정 사용

**권장**: Timeout, Retry, 로깅 설정 추가
```java
// infrastructure.config.FeignConfig.java
@Configuration
public class FeignConfig {
    
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
            5000,  // connectTimeout: 5초
            10000  // readTimeout: 10초
        );
    }
    
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(
            100,   // period: 재시도 간격
            1000,  // maxPeriod: 최대 재시도 간격
            3      // maxAttempts: 최대 재시도 횟수
        );
    }
    
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC; // NONE, BASIC, HEADERS, FULL
    }
}

// HubClient에 적용
@FeignClient(
    name = "hub-service",
    configuration = FeignConfig.class
)
public interface HubClient {
    // ...
}
```

#### 5. 통합 테스트 추가

**현재**: 수동 테스트만 진행

**권장**: FeignClient 통합 테스트 추가
```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CompanyServiceIntegrationTest {
    
    @Autowired
    private CompanyService companyService;
    
    @MockBean
    private HubClient hubClient; // WireMock 대신 MockBean 사용
    
    @Test
    @DisplayName("Company 생성 시 Hub 검증 - 성공")
    void createCompany_withValidHub_success() {
        // given
        UUID hubId = UUID.randomUUID();
        HubResponse hubResponse = new HubResponse(hubId, "서울허브", "서울시", null, null);
        when(hubClient.getHub(hubId))
            .thenReturn(ApiResponse.success(hubResponse));
        
        CompanyCreateRequest request = new CompanyCreateRequest(...);
        
        // when
        CompanyCreateResponse response = companyService.createCompany(request);
        
        // then
        assertThat(response).isNotNull();
        verify(hubClient, times(1)).getHub(hubId);
    }
    
    @Test
    @DisplayName("Company 생성 시 Hub 검증 - 실패 (존재하지 않는 허브)")
    void createCompany_withInvalidHub_throwsException() {
        // given
        UUID hubId = UUID.randomUUID();
        when(hubClient.getHub(hubId))
            .thenThrow(FeignException.NotFound.class);
        
        CompanyCreateRequest request = new CompanyCreateRequest(...);
        
        // when & then
        assertThatThrownBy(() -> companyService.createCompany(request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.HUB_NOT_FOUND);
    }
}
```

#### 6. 팀 전체 적용 가이드 작성

**현재**: company-service만 수정

**권장**: 다른 서비스에도 동일하게 적용 필요
```markdown
# docs/feign-client-guide.md

## FeignClient 사용 가이드

### 1. Client 인터페이스 정의
- 반환 타입: `ApiResponse<T>` 사용
- 에러 발생 시 FeignException 발생

### 2. Adapter 패턴 사용
- infrastructure.client 패키지에 Adapter 생성
- FeignException 처리 로직 캡슐화
- Service 계층에서는 Adapter만 사용

### 3. 예외 처리
- FeignException.NotFound → CustomException(ErrorCode.XXX_NOT_FOUND)
- FeignException.ServiceUnavailable → CustomException(ErrorCode.EXTERNAL_API_FAILED)
- 타임아웃, 네트워크 오류 등 모든 예외 처리

### 4. 설정
- Timeout, Retry 설정 필수
- 로깅 레벨 설정 (운영: BASIC, 개발: FULL)

### 5. 테스트
- MockBean 또는 WireMock 사용
- 성공/실패 케이스 모두 테스트
```

## 📊 종합 평가

| 항목 | 점수 | 평가 |
|------|------|------|
| 문제 파악 | ⭐⭐⭐⭐⭐ | OpenFeign 메커니즘 정확히 이해 |
| 해결 방안 | ⭐⭐⭐⭐⭐ | GlobalExceptionHandler 수정 우수 |
| 영향 범위 | ⭐⭐⭐⭐⭐ | 전체 MSA 통신에 필수적인 개선 |
| 코드 품질 | ⭐⭐⭐☆☆ | Controller 응답 타입 불일치 |
| 예외 처리 | ⭐⭐⭐☆☆ | NotFound만 처리, 타임아웃 등 미처리 |
| 문서화 | ⭐⭐⭐⭐☆ | PR Description에 상세한 설명 |

**총평**: 핵심 문제를 정확히 파악하고 GlobalExceptionHandler를 개선한 점은 매우 우수함. **전체 MSA 통신의 기반이 되는 중요한 수정**. Controller 응답 타입 통일과 예외 처리 강화 후 Approve 가능.

## ✅ Merge 전 체크리스트

### 필수 수정 (Blocking Issues)
- [ ] **Controller 응답 타입 통일** (ResponseEntity + ApiResponse 또는 ApiResponse만)
- [ ] **DELETE 메서드 수정** (타입과 실제 응답 일치)
- [ ] **팀 표준 논의 필요** (API 응답 형식 통일안 결정)

### 강력 권장
- [ ] HubClientAdapter 생성 (FeignException 처리 캡슐화)
- [ ] ErrorCode 추가 확인 (HUB_NOT_FOUND, EXTERNAL_API_FAILED)
- [ ] FeignClient Configuration 추가 (Timeout, Retry)
- [ ] 통합 테스트 추가 (FeignClient Mock)

### 선택 사항 (추후 개선)
- [ ] 디버깅 로그 정리 (INFO → DEBUG)
- [ ] 주석 스타일 통일
- [ ] FeignClient 사용 가이드 문서 작성 (docs/)
- [ ] 다른 서비스에도 동일하게 적용 (order, delivery, product 등)

## 🔗 Related Links
- PR: https://github.com/14th-anniv/one-for-logis/pull/75
- Issue #74: FeignClient 상태 코드 오류 해결
- Branch: `feature/#74-feignclient-fix`
- Related: GlobalExceptionHandler, HubClient, CompanyController

## 👥 Author
- @sonaanweb

## 💬 To Reviewer
> 다른 서비스 쪽에서도 일어날 오류 같아서 우선 중간 코드 올립니다.  
> 해결 과정이 적절한 지 확인해주세요!

**리뷰어 답변**:
- 문제 파악과 GlobalExceptionHandler 수정은 **매우 우수**합니다! 👍
- **전체 MSA 통신에 필수적인 개선**으로, 다른 서비스에도 적용 필요합니다.
- Controller 응답 타입 통일이 필요합니다. 팀 전체 표준화 논의 권장합니다.
- HubClientAdapter 패턴으로 FeignException 처리를 캡슐화하면 더욱 좋습니다.
- 수정 후 다른 서비스(order, delivery 등)에도 동일하게 적용해주세요!

## 🎯 팀 전체 적용 필요 사항

### 1. API 응답 형식 표준 결정
```
Option 1: ApiResponse만 사용 (현재 컨벤션)
  - 장점: 응답 형식 통일
  - 단점: REST 표준과 다름, HTTP 상태 코드 미활용

Option 2: ResponseEntity + ApiResponse (권장)
  - 장점: REST 표준 준수, FeignClient 친화적
  - 단점: 코드 중복 가능성
```

### 2. FeignClient 사용 표준
- Adapter 패턴 사용 (예외 처리 캡슐화)
- Timeout, Retry 설정 필수
- 통합 테스트 작성

### 3. GlobalExceptionHandler 적용
- 모든 서비스에 PR #75의 수정사항 적용
- ResponseEntity 반환으로 실제 HTTP 상태 코드 반영

### 4. 문서화
- `docs/api-standards.md`: API 응답 형식 표준
- `docs/feign-client-guide.md`: FeignClient 사용 가이드
- `docs/troubleshooting.md`: FeignClient 관련 이슈 추가

---
**리뷰어**: Claude (AI Code Reviewer)  
**리뷰 완료일**: 2025-11-11  
**상태**: 리뷰 완료 - 팀 표준 논의 필요 (Controller 응답 타입 통일)  
**중요도**: ⭐⭐⭐⭐⭐ (전체 MSA 통신 기반)
