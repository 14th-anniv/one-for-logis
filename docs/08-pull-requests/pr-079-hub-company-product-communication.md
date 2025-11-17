# PR #79: 허브/업체/상품 서비스 통신 처리

## Issue Number
> closed #66

## 📝 Description

### Hub ↔ Company ↔ Product 서비스 간 통신
- **FeignClient 구현**: Hub, Company 유효성 검증
- **동기 통신 방식**: Product 생성 시 Hub/Company ID 검증
- **SecurityConfig permitAll**: 테스트용 임시 설정 (모든 요청 허용)
- **샘플 데이터 추가**: company.sql (업체 5개 샘플)
- **TODO**: 추후 비동기 방식 (Kafka/RabbitMQ) 전환 가능성

## 📊 변경 사항

### 변경 파일 (13개 파일, +99/-44)

#### Product Service - FeignClient 추가
- **HubClient**: 
  - `@FeignClient(name = "hub-service", path = "/api/v1/hubs")`
  - `getHub(UUID hubId)`: Hub 단건 조회
  - 반환 타입: `ApiResponse<HubResponse>`

- **CompanyClient**: 
  - `@FeignClient(name = "company-service", path = "/api/v1/companies")`
  - `getCompany(UUID companyId)`: Company 단건 조회
  - 반환 타입: `ApiResponse<CompanyResponse>`

- **DTO 추가**:
  - `HubResponse`: `id`, `name`, `address`, `lat`, `lon` (record 타입)
  - `CompanyResponse`: `id`, `name`, `hubId`, `address` (record 타입)

#### Product Service - 유효성 검증
- **ProductService**: 
  - `createProduct()`: Hub/Company 검증 로직 추가
  - `fetchHub(UUID hubId)`: Hub 존재 여부 확인
  - `fetchCompany(UUID companyId)`: Company 존재 여부 확인
  - `FeignException.NotFound` → `CustomException(ErrorCode.XXX_NOT_FOUND)`

#### Product Service - Entity 검증 로직 삭제
- **Product Entity**: 
  - `updateName()`, `updateQuantity()`, `updatePrice()` 검증 로직 제거
  - IllegalArgumentException 던지던 코드 삭제
  - **단순 setter 역할만 수행**

#### Configuration
- **SecurityConfig** (Company, Product):
  - `configureAuthorization()` 오버라이드
  - `permitAll()` 설정으로 모든 요청 허용 (테스트용)
  - TODO 주석 제거

- **build.gradle** (Product):
  - `spring-cloud-starter-loadbalancer` 의존성 추가

- **application.yml** (Company):
  - `spring.config.import: optional:file:.env[.properties]` 추가

#### Sample Data
- **company.sql**: 
  - 업체 5개 샘플 데이터 (SUPPLIER 타입)
  - 각 허브별 업체 1개씩 (서울, 경기북부, 경기남부, 부산, 대구)
  - UUID 자동 생성 (`gen_random_uuid()`)

#### Refactoring
- 불필요한 placeholder 파일 삭제:
  - `CompanyConfig.java` (2개)
  - `ProductClient.java` (infrastructure.client, infrastructure.config)

## 🌐 Test Result

### FeignClient 통신 테스트
- **존재하지 않는 Hub ID**: 404 Not Found 정상 반환
- **존재하지 않는 Company ID**: 404 Not Found 정상 반환
- **유효한 Hub/Company ID**: Product 생성 성공

### 테스트 스크린샷 확인
- Not Found 응답 확인 (3장)
- 유효성 검증 정상 동작

## 🔍 코드 리뷰 결과

### ✅ 잘된 점

#### 1. FeignClient 구현 정확 - PR #75 패턴 적용 ⭐⭐⭐⭐⭐
```java
@FeignClient(name = "hub-service", path = "/api/v1/hubs")
public interface HubClient {
    @GetMapping("/{hubId}")
    ApiResponse<HubResponse> getHub(@PathVariable UUID hubId);
}
```
- PR #75에서 지적한 패턴 정확히 반영
- `ApiResponse<T>` 반환 타입 사용
- path 속성으로 엔드포인트 명시

#### 2. DTO record 타입 사용
```java
public record HubResponse(
    UUID id,
    String name,
    String address,
    BigDecimal lat,
    BigDecimal lon
) {}
```
- 팀 표준 패턴 준수
- 불변성 보장

#### 3. 예외 처리 정확
```java
public void fetchHub(UUID hubId) {
    try {
        hubClient.getHub(hubId);
    } catch (FeignException.NotFound e) {
        throw new CustomException(ErrorCode.HUB_NOT_FOUND);
    }
}
```
- FeignException.NotFound → CustomException 변환
- PR #75 리뷰 반영

#### 4. Sample Data 추가
- 업체 샘플 데이터로 테스트 용이성 향상
- 각 허브별 1개씩 균형있게 배치

### 🚨 Critical Issues (필수 수정)

#### 1. Product Entity 검증 로직 완전 삭제 (심각도: 매우 높음)

**변경 전** (PR #65):
```java
public void updateName(String name) {
    if (name == null || name.isBlank()) {
        throw new IllegalArgumentException("상품명은 비워둘 수 없습니다.");
    }
    this.name = name;
}
```

**변경 후** (PR #79):
```java
public void updateName(String name) {
    this.name = name;  // ❌ 검증 로직 완전 삭제
}
```

**문제점**:
- **도메인 로직 완전 제거**: Entity의 불변식(invariant) 보장 불가
- null, 빈 문자열, 음수 값 모두 허용됨 → **데이터 무결성 위험**
- PR #65 리뷰에서 "CustomException으로 변경" 권장했으나 **아예 삭제함**
- DDD 패턴 위반: Entity가 자신의 상태를 지키지 못함

**올바른 수정 방향** (PR #65 리뷰 반영):
```java
public void updateName(String name) {
    if (name == null || name.isBlank()) {
        throw new CustomException(ErrorCode.PRODUCT_INVALID_NAME);
    }
    this.name = name;
}

public void updateQuantity(Integer quantity) {
    if (quantity == null || quantity < 0) {
        throw new CustomException(ErrorCode.PRODUCT_INVALID_QUANTITY);
    }
    this.quantity = quantity;
}

public void updatePrice(BigDecimal price) {
    if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
        throw new CustomException(ErrorCode.PRODUCT_INVALID_PRICE);
    }
    this.price = price;
}
```

**ErrorCode 추가 필요**:
```java
// common-lib ErrorCode.java
PRODUCT_INVALID_NAME(HttpStatus.BAD_REQUEST, "상품명은 비워둘 수 없습니다."),
PRODUCT_INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "재고는 0 이상이어야 합니다."),
PRODUCT_INVALID_PRICE(HttpStatus.BAD_REQUEST, "단가는 0 이상이어야 합니다."),
```

#### 2. FeignClient 예외 처리 불완전 (심각도: 중간)

**현재 코드** (`ProductService.java`):
```java
public void fetchHub(UUID hubId) {
    try {
        hubClient.getHub(hubId);
    } catch (FeignException.NotFound e) {
        throw new CustomException(ErrorCode.HUB_NOT_FOUND);
    }
    // 다른 FeignException 미처리: 타임아웃, 네트워크 오류, 500 에러 등
}
```

**문제점**:
- `FeignException.NotFound`만 처리
- 타임아웃, 네트워크 오류, 500 에러 등 미처리 → 예외 전파
- PR #75 리뷰에서 권장한 Adapter 패턴 미적용

**권장 수정** (PR #75 패턴):
```java
// infrastructure.client.HubClientAdapter
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

// CompanyClientAdapter도 동일하게 구현

// ProductService 수정
@RequiredArgsConstructor
public class ProductService {
    private final HubClientAdapter hubClientAdapter;
    private final CompanyClientAdapter companyClientAdapter;
    private final ProductRepository productRepository;
    
    @Transactional
    public ProductCreateResponse createProduct(ProductCreateRequest request) {
        // Adapter 사용 (FeignClient 직접 사용 X)
        hubClientAdapter.getHub(request.hubId());
        companyClientAdapter.getCompany(request.companyId());
        
        Product product = Product.createProduct(...);
        return ProductCreateResponse.from(productRepository.save(product));
    }
}
```

#### 3. SecurityConfig permitAll - 프로덕션 위험 (심각도: 높음)

**현재 코드**:
```java
@Override
protected void configureAuthorization(...) {
    auth.requestMatchers("/api/v1/**").permitAll(); // ❌ 모든 요청 허용
}
```

**문제점**:
- **모든 API 엔드포인트 인증/인가 없이 접근 가능**
- 테스트용으로 추가했으나 **프로덕션 배포 시 심각한 보안 위험**
- `@PreAuthorize` 어노테이션이 무의미해짐
- 삭제, 수정 등 민감한 작업도 누구나 실행 가능

**권장 수정**:
```java
// Option 1: 테스트 프로파일에서만 허용
@Profile("local")
@Configuration
@EnableMethodSecurity
public class SecurityConfig extends SecurityConfigBase {
    @Override
    protected void configureAuthorization(...) {
        auth.requestMatchers("/api/v1/**").permitAll();
    }
}

// Option 2: 특정 엔드포인트만 허용
@Override
protected void configureAuthorization(...) {
    auth
        .requestMatchers("/api/v1/products/{id}").permitAll()  // 조회만 허용
        .requestMatchers("/api/v1/companies/{id}").permitAll() // 조회만 허용
        .anyRequest().authenticated();  // 나머지는 인증 필요
}

// Option 3: 삭제하고 @PreAuthorize 활용
// SecurityConfig에서 permitAll 제거
// Controller에서 @PreAuthorize로 세밀한 권한 제어
```

**권장**: **Option 1 (Profile 분리)** - 로컬에서만 허용, 프로덕션에서는 인증 필수

#### 4. FeignClient 반환값 미사용 (심각도: 낮음)

**현재 코드**:
```java
public void fetchHub(UUID hubId) {
    try {
        hubClient.getHub(hubId);  // ❌ 반환값 사용 안함
    } catch (FeignException.NotFound e) {
        throw new CustomException(ErrorCode.HUB_NOT_FOUND);
    }
}
```

**문제점**:
- `ApiResponse<HubResponse>` 반환값을 받지만 사용하지 않음
- 존재 여부만 확인하는 목적이지만 비효율적
- Hub 정보를 로그에 남기거나 검증에 활용 가능

**개선안**:
```java
// Option 1: 반환값 활용
public HubResponse fetchHub(UUID hubId) {
    try {
        ApiResponse<HubResponse> response = hubClient.getHub(hubId);
        HubResponse hub = response.data();
        log.info("Product 생성 시 Hub 검증: hubId={}, hubName={}", hub.id(), hub.name());
        return hub;
    } catch (FeignException.NotFound e) {
        throw new CustomException(ErrorCode.HUB_NOT_FOUND);
    }
}

// Option 2: 별도 존재 확인 API 추가 (HubService에)
@GetMapping("/{hubId}/exists")
public ApiResponse<Boolean> existsHub(@PathVariable UUID hubId) {
    return ApiResponse.success(hubRepository.existsById(hubId));
}

// ProductService
public void validateHub(UUID hubId) {
    Boolean exists = hubClient.existsHub(hubId).data();
    if (!exists) {
        throw new CustomException(ErrorCode.HUB_NOT_FOUND);
    }
}
```

### ⚠️ 개선 권장 사항

#### 1. LoadBalancer 의존성 사용 목적 불명확

**추가된 의존성**:
```gradle
implementation 'org.spring-cloud-starter-loadbalancer'
```

**문제점**:
- 로드 밸런서가 필요한 상황인지 불분명
- Eureka를 사용하면 기본 포함됨
- 명시적 추가 이유 확인 필요

**확인 필요**:
- Eureka 설정이 제대로 되어 있는지
- 로드 밸런싱이 필요한 상황인지
- 불필요하면 제거

#### 2. company.sql 파일 위치

**현재 위치**:
```
company-service/company.sql
```

**권장 위치**:
```
company-service/src/main/resources/db/migration/company.sql
또는
company-service/src/main/resources/data.sql
```

**이유**:
- 프로젝트 표준 리소스 경로
- `spring.sql.init.data-locations` 설정 가능
- 버전 관리 및 배포 시 명확한 경로

#### 3. 삭제된 파일 정리 확인

**삭제된 파일들**:
- `CompanyConfig.java` (2개)
- `ProductClient.java` (infrastructure.client, infrastructure.config)

**확인 사항**:
- Git에서 완전히 삭제되었는지 확인
- 다른 곳에서 import하는 곳 없는지 확인

#### 4. FeignClient Configuration 추가

**현재**: 기본 설정만 사용

**권장 추가** (PR #75 패턴):
```java
// config.FeignConfig.java
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
            100,   // period
            1000,  // maxPeriod
            3      // maxAttempts
        );
    }
    
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}

// FeignClient에 적용
@FeignClient(
    name = "hub-service", 
    path = "/api/v1/hubs",
    configuration = FeignConfig.class
)
public interface HubClient {
    // ...
}
```

#### 5. 통합 테스트 추가

**현재**: 수동 테스트만 진행

**권장 추가**:
```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductServiceIntegrationTest {
    
    @Autowired
    private ProductService productService;
    
    @MockBean
    private HubClient hubClient;
    
    @MockBean
    private CompanyClient companyClient;
    
    @Test
    @DisplayName("상품 생성 시 Hub 검증 - 성공")
    void createProduct_withValidHub_success() {
        // given
        UUID hubId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        
        HubResponse hubResponse = new HubResponse(hubId, "서울허브", "서울시", null, null);
        CompanyResponse companyResponse = new CompanyResponse(companyId, "A업체", hubId, "서울시");
        
        when(hubClient.getHub(hubId))
            .thenReturn(ApiResponse.success(hubResponse));
        when(companyClient.getCompany(companyId))
            .thenReturn(ApiResponse.success(companyResponse));
        
        ProductCreateRequest request = new ProductCreateRequest(...);
        
        // when
        ProductCreateResponse response = productService.createProduct(request);
        
        // then
        assertThat(response).isNotNull();
        verify(hubClient, times(1)).getHub(hubId);
        verify(companyClient, times(1)).getCompany(companyId);
    }
    
    @Test
    @DisplayName("상품 생성 시 Hub 검증 - 실패 (존재하지 않는 Hub)")
    void createProduct_withInvalidHub_throwsException() {
        // given
        UUID hubId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        
        when(hubClient.getHub(hubId))
            .thenThrow(FeignException.NotFound.class);
        
        ProductCreateRequest request = new ProductCreateRequest(...);
        
        // when & then
        assertThatThrownBy(() -> productService.createProduct(request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.HUB_NOT_FOUND);
    }
}
```

#### 6. 비동기 전환 계획 문서화

**Description에 언급**:
> 추후 비동기 방식(kafka / rabbitMQ) 메세지로 발전 가능성

**권장**:
- Issue로 등록하여 추적
- 비동기 전환 시 고려사항 문서화
  - 최종 일관성(Eventual Consistency)
  - 보상 트랜잭션(Compensating Transaction)
  - Saga 패턴 적용

## 📊 종합 평가

| 항목 | 점수 | 평가 |
|------|------|------|
| FeignClient 구현 | ⭐⭐⭐⭐⭐ | PR #75 패턴 정확히 반영 |
| 예외 처리 | ⭐⭐⭐☆☆ | NotFound만 처리, Adapter 미적용 |
| 도메인 로직 | ⭐☆☆☆☆ | Entity 검증 로직 완전 삭제 (심각) |
| 보안 | ⭐⭐☆☆☆ | permitAll로 모든 요청 허용 (위험) |
| 테스트 | ⭐⭐☆☆☆ | 수동 테스트만, 자동화 테스트 없음 |
| 샘플 데이터 | ⭐⭐⭐⭐☆ | company.sql 추가로 테스트 용이 |
| 팀 표준 준수 | ⭐⭐⭐⭐☆ | record DTO, ApiResponse 사용 |

**총평**: FeignClient 통신 구현은 PR #75 패턴을 정확히 반영하여 우수하나, **Product Entity의 검증 로직을 완전히 삭제하여 도메인 무결성 위험**이 매우 높음. SecurityConfig permitAll 설정도 프로덕션 배포 시 보안 위험. 해당 이슈들을 반드시 수정 후 Merge 권장.

## ✅ Merge 전 체크리스트

### 필수 수정 (Blocking Issues)
- [ ] **Product Entity 검증 로직 복원** - CustomException으로 변경 (PR #65 리뷰 반영)
- [ ] **ErrorCode 추가** (PRODUCT_INVALID_NAME, PRODUCT_INVALID_QUANTITY, PRODUCT_INVALID_PRICE)
- [ ] **SecurityConfig permitAll 제거 또는 Profile 분리** (보안 위험)
- [ ] **FeignClient 예외 처리 강화** (타임아웃, 네트워크 오류 등)

### 강력 권장
- [ ] ClientAdapter 패턴 적용 (PR #75 권장사항)
- [ ] FeignClient Configuration 추가 (Timeout, Retry)
- [ ] 통합 테스트 추가 (MockBean 사용)
- [ ] company.sql 파일 위치 이동 (resources/db/migration/)

### 선택 사항 (추후 개선)
- [ ] FeignClient 반환값 활용 또는 exists API 추가
- [ ] LoadBalancer 의존성 필요성 확인
- [ ] 비동기 전환 계획 Issue 등록
- [ ] 삭제 파일 완전 제거 확인

## 🔗 Related Links
- PR: https://github.com/14th-anniv/one-for-logis/pull/79
- Issue #66: 허브/업체/상품 서비스 통신 처리
- Branch: `feature/#66-hub-company-product-communication`
- Related: 
  - PR #75 (FeignClient 패턴 - 참고 기준)
  - PR #65 (상품 CRUD - Entity 검증 로직 이슈)

## 👥 Author
- @sonaanweb

## 💬 To Reviewer
> 리뷰 받고 싶은 포인트를 작성합니다.

**리뷰어 답변**:
- **FeignClient 구현은 PR #75 패턴을 정확히 반영했습니다!** 👍
  - `ApiResponse<T>` 반환 타입 사용
  - record DTO 사용
  - FeignException.NotFound 처리

- **그러나 Critical한 문제들이 있습니다** ⚠️⚠️⚠️
  
  1. **Product Entity 검증 로직 완전 삭제** (매우 심각)
     - null, 빈 문자열, 음수 모두 허용 → 데이터 무결성 위험
     - PR #65에서 "CustomException으로 변경" 권장했으나 아예 삭제함
     - 반드시 복원 필요 (CustomException 사용)
  
  2. **SecurityConfig permitAll** (보안 위험)
     - 모든 API를 인증 없이 접근 가능
     - 프로덕션 배포 시 심각한 보안 문제
     - Profile 분리 또는 제거 필수
  
  3. **FeignClient 예외 처리 불완전**
     - NotFound만 처리, 타임아웃/네트워크 오류 미처리
     - Adapter 패턴 적용 권장 (PR #75 참고)

- **권장사항**:
  1. Product Entity 검증 로직 복원 + ErrorCode 추가
  2. SecurityConfig permitAll 제거 또는 @Profile("local")로 제한
  3. ClientAdapter 패턴 적용
  4. 통합 테스트 추가
  5. 위 수정 후 Approve 가능합니다!

## 🎯 개선 우선순위

### 1단계: Critical 이슈 해결 (필수)
1. **Product Entity 검증 로직 복원**
2. **SecurityConfig permitAll 제거/제한**
3. FeignClient 예외 처리 강화

### 2단계: 아키텍처 개선 (강력 권장)
1. ClientAdapter 패턴 적용
2. FeignClient Configuration
3. 통합 테스트 추가

### 3단계: 추후 개선 (선택)
1. 비동기 전환 계획
2. 성능 최적화
3. 모니터링 추가

---
**리뷰어**: Claude (AI Code Reviewer)  
**리뷰 완료일**: 2025-11-11  
**상태**: 리뷰 완료 - Critical 수정 필수 (Entity 검증, Security 설정)  
**중요도**: ⭐⭐⭐⭐⭐ (서비스 간 통신 핵심, 도메인 무결성 위험)
