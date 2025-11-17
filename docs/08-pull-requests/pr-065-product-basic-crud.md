# PR #65: 상품 기본 CRUD 구현

## Issue Number
> closed #62

## 📝 Description

### Product Service - 기본 CRUD 기능
- 상품 등록, 수정, 삭제, 조회 API 구현
- 이름 검색 + 페이징 처리
- DDD 패키지 구조 적용
- 팀 표준 패턴 준수 (record DTO, SecurityConfigBase 상속)
- Eureka Server Dockerfile 추가

## 📊 변경 사항

### 변경 파일 (27개 파일, +682/-64)

#### Common-lib
- **ErrorCode**: `PRODUCT_NOT_FOUND` 에러 코드 추가

#### Product Service - Domain Layer
- **Product Entity**: 
  - BaseEntity 상속으로 공통 감사 필드 확보
  - `@GeneratedValue(strategy = GenerationType.UUID)` PK 자동 생성
  - 필수 필드: `name`, `quantity`, `price`, `hubId`, `companyId`
  - Factory method: `createProduct()`
  - 수정 메서드: `updateName()`, `updateQuantity()`, `updatePrice()`
  - 삭제: `deleteProduct()` - Soft Delete 패턴

- **ProductRepository**: 
  - Domain repository interface (infrastructure 독립성)
  - 메서드: `save()`, `findByIdAndDeletedFalse()`, `findByDeletedFalse()`, `findByNameContainingAndDeletedFalse()`

#### Application Layer
- **ProductService**: 
  - CRUD 비즈니스 로직
  - 페이징 검증 (size: 10, 30, 50 허용)
  - 정렬 처리 (기본값: createdAt DESC)
  - Null-safe 수정 로직 (변경할 필드만 업데이트)

- **Request DTOs** (`record` 타입):
  - `ProductCreateRequest`: 생성 요청 (Validation: @NotBlank, @NotNull, @PositiveOrZero)
  - `ProductUpdateRequest`: 수정 요청 (모든 필드 Optional)

- **Response DTOs** (`record` 타입):
  - `ProductCreateResponse`: 생성 응답 (id, name, quantity, price, hubId, companyId, createdBy, createdAt)
  - `ProductUpdateResponse`: 수정 응답 (id, name, quantity, price, hubId, companyId, updatedBy, updatedAt)
  - `ProductDetailResponse`: 단건 조회 응답 (모든 감사 필드 포함)
  - `ProductSearchResponse`: 검색 조회 응답 (간략 정보만)

#### Presentation Layer
- **ProductController**: 
  - 5개 엔드포인트 (`POST`, `PATCH`, `DELETE`, `GET`, `GET /search`)
  - Swagger 문서화 (`@Operation`, `@Tag`)
  - 권한 체크: `@PreAuthorize` (MASTER, HUB_MANAGER, COMPANY_MANAGER)
  - 응답 타입: `ResponseEntity<ApiResponse<T>>` (일부 혼용)

#### Infrastructure Layer
- **ProductJpaRepository**: Spring Data JPA interface
- **ProductRepositoryImpl**: Repository 인터페이스 구현체 (DDD 패턴)

#### Configuration
- **SecurityConfig**: SecurityConfigBase 상속 (팀 표준)
- **build.gradle**: Spring Security, Swagger, Validation 의존성 추가
- **Dockerfile**: Eureka Server Docker 이미지 빌드 설정 추가

#### Refactoring
- `JpaAuditConfig.java` 삭제 (common-lib으로 이동)
- 불필요한 placeholder 파일 삭제 (`request.java`, `response.java`, `ProductException.java` 등)
- Company Service 주석 정리

## 🌐 Test Result

### API 테스트 완료 (팀 노션 문서화)
- 상품 생성: `POST /api/v1/products` - 201 Created
- 상품 수정: `PATCH /api/v1/products/{productId}` - 200 OK
- 상품 삭제: `DELETE /api/v1/products/{productId}` - 200 OK
- 상품 단건 조회: `GET /api/v1/products/{productId}` - 200 OK
- 상품 전체 조회: `GET /api/v1/products?productName=모니터&page=0&size=10` - 200 OK

## 🔍 코드 리뷰 결과

### ✅ 잘된 점

#### 1. 팀 표준 패턴 철저히 준수
- `record` DTO 사용 (불변성 보장)
- SecurityConfigBase 상속
- DDD 패키지 구조 (domain.repository vs infrastructure.persistence)
- ApiResponse 래퍼 사용

#### 2. 페이징 검증 로직 우수
```java
private Pageable createPageable(int page, int size, String sortBy, boolean isAsc) {
    int validatedSize = List.of(10, 30, 50).contains(size) ? size : 10;
    int validatedPage = Math.max(page, 0); // 음수 방지
    Sort.Direction direction = isAsc ? Sort.Direction.ASC : Sort.Direction.DESC;
    return PageRequest.of(validatedPage, validatedSize, Sort.by(direction, sortBy));
}
```
- 팀 표준 페이징 규칙 준수
- 음수 페이지 방어 코드
- 잘못된 size 요청 시 기본값 처리

#### 3. Null-safe 수정 로직
- 변경할 필드만 선택적으로 업데이트
- Null 체크 후 메서드 호출

#### 4. DTO Validation 명확
- `@NotBlank`, `@NotNull`, `@PositiveOrZero` 적절히 사용
- Swagger description으로 문서화

### 🚨 Critical Issues (필수 수정)

#### 1. Controller 응답 타입 혼용 (심각도: 높음)

**현재 코드** (`ProductController.java`):
```java
// 혼재된 응답 타입
@PostMapping
public ResponseEntity<ApiResponse<ProductCreateResponse>> createProduct(...) // ResponseEntity + ApiResponse

@PatchMapping("/{productId}")
public ResponseEntity<ApiResponse<ProductUpdateResponse>> updateProduct(...) // ResponseEntity + ApiResponse

@DeleteMapping("/{productId}")
public ResponseEntity<ApiResponse<Void>> deleteProduct(...) // ResponseEntity + ApiResponse

@GetMapping("/{productId}")
public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(...) // ResponseEntity + ApiResponse

@GetMapping
public ResponseEntity<ApiResponse<PageResponse<ProductSearchResponse>>> getProducts(...) // ResponseEntity + ApiResponse
```

**문제점**:
- 모든 엔드포인트가 `ResponseEntity<ApiResponse<T>>` 형식 사용
- **Company Service와 일치**하지만, **PR #75 논의 필요** (팀 전체 표준화)
- HTTP 상태 코드를 일부만 명시 (POST: 201, DELETE: 200)

**권장 수정** (PR #75 GlobalExceptionHandler 수정 취지 반영):
```java
// 일관된 ResponseEntity + ApiResponse 사용
@PostMapping
public ResponseEntity<ApiResponse<ProductCreateResponse>> createProduct(...) {
    var response = productService.createProduct(request);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.success(response)); // 201 Created
}

@PatchMapping("/{productId}")
public ResponseEntity<ApiResponse<ProductUpdateResponse>> updateProduct(...) {
    var response = productService.updateProduct(productId, request);
    return ResponseEntity.ok(ApiResponse.success(response)); // 200 OK
}

@DeleteMapping("/{productId}")
public ResponseEntity<ApiResponse<Void>> deleteProduct(...) {
    productService.deleteProduct(productId, userPrincipal.username());
    return ResponseEntity.ok(ApiResponse.noContent()); // 200 OK
}

// 또는 REST 표준
@DeleteMapping("/{productId}")
public ResponseEntity<Void> deleteProduct(...) {
    productService.deleteProduct(productId, userPrincipal.username());
    return ResponseEntity.noContent().build(); // 204 No Content
}
```

#### 2. Entity 검증 예외 타입 불일치 (심각도: 중간)

**현재 코드** (`Product.java`):
```java
public void updateName(String name) {
    if (name == null || name.isBlank()) {
        throw new IllegalArgumentException("상품명은 비워둘 수 없습니다."); // ❌
    }
    this.name = name;
}

public void updateQuantity(Integer quantity) {
    if (quantity == null || quantity < 0) {
        throw new IllegalArgumentException("재고는 0 이상이어야 합니다."); // ❌
    }
    this.quantity = quantity;
}
```

**문제점**:
- 주석에 "임시 예외 처리 (IllegalArgumentException 변경 예정)" 명시
- 팀 표준 `CustomException` 미사용
- GlobalExceptionHandler가 처리하지만 일관성 부족

**해결책**:
```java
// common-lib ErrorCode에 추가
PRODUCT_INVALID_NAME(HttpStatus.BAD_REQUEST, "상품명은 비워둘 수 없습니다."),
PRODUCT_INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "재고는 0 이상이어야 합니다."),
PRODUCT_INVALID_PRICE(HttpStatus.BAD_REQUEST, "단가는 0 이상이어야 합니다."),

// Product Entity 수정
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

#### 3. FeignClient 통신 미구현 (심각도: 높음)

**현재 상태**:
- Product 생성 시 `hubId`, `companyId` 검증 로직 없음
- 존재하지 않는 Hub/Company ID로 Product 생성 가능

**권장 추가** (PR #75 패턴 적용):
```java
// infrastructure.client.HubClient
@FeignClient(name = "hub-service")
public interface HubClient {
    @GetMapping("/api/v1/hubs/{hubId}")
    ApiResponse<HubResponse> getHub(@PathVariable UUID hubId);
}

// infrastructure.client.CompanyClient
@FeignClient(name = "company-service")
public interface CompanyClient {
    @GetMapping("/api/v1/companies/{companyId}")
    ApiResponse<CompanyResponse> getCompany(@PathVariable UUID companyId);
}

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
            throw new CustomException(ErrorCode.HUB_NOT_FOUND);
        } catch (FeignException e) {
            throw new CustomException(ErrorCode.EXTERNAL_API_FAILED);
        }
    }
}

// ProductService 수정
@Transactional
public ProductCreateResponse createProduct(ProductCreateRequest request) {
    // Hub, Company 검증 추가
    hubClientAdapter.getHub(request.hubId());
    companyClientAdapter.getCompany(request.companyId());
    
    Product product = Product.createProduct(
        request.name(),
        request.quantity(),
        request.price(),
        request.hubId(),
        request.companyId()
    );
    
    Product savedProduct = productRepository.save(product);
    return ProductCreateResponse.from(savedProduct);
}
```

### ⚠️ 개선 권장 사항

#### 1. 단위 테스트 누락

**현재**: API 테스트만 수동 진행 (Postman)

**추가 권장**:
```java
// ProductServiceTest.java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    
    @Mock
    ProductRepository productRepository;
    
    @Mock
    HubClientAdapter hubClientAdapter;
    
    @Mock
    CompanyClientAdapter companyClientAdapter;
    
    @InjectMocks
    ProductService productService;
    
    @Test
    @DisplayName("상품 생성 성공")
    void createProduct_success() {
        // given
        UUID hubId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        ProductCreateRequest request = new ProductCreateRequest(
            "스파르타 모니터", 100, new BigDecimal("249000"), hubId, companyId
        );
        
        when(hubClientAdapter.getHub(hubId))
            .thenReturn(new HubResponse(hubId, "서울허브", "서울시", null, null));
        when(companyClientAdapter.getCompany(companyId))
            .thenReturn(new CompanyResponse(companyId, "스파르타", "서울시", hubId));
        when(productRepository.save(any(Product.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        
        // when
        ProductCreateResponse response = productService.createProduct(request);
        
        // then
        assertThat(response.name()).isEqualTo("스파르타 모니터");
        assertThat(response.quantity()).isEqualTo(100);
        verify(hubClientAdapter, times(1)).getHub(hubId);
        verify(companyClientAdapter, times(1)).getCompany(companyId);
    }
    
    @Test
    @DisplayName("상품 조회 실패 - 존재하지 않는 ID")
    void getProductDetail_notFound() {
        // given
        UUID productId = UUID.randomUUID();
        when(productRepository.findByIdAndDeletedFalse(productId))
            .thenReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> productService.getProductDetail(productId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }
}
```

#### 2. Controller TODO 주석 제거 또는 구현

**현재 코드**:
```java
// todo: 상품 관리 기본 crud 개발 후 테스트하며 로직 추가 (hub, company 체크 도메인 규칙 등)
```

**권장**:
- FeignClient 구현 완료 후 TODO 삭제
- 또는 Issue로 등록 후 TODO 주석 제거

#### 3. Swagger 예외 응답 문서화

**현재 코드**:
```java
@Operation(summary = "상품 단건 조회", description = "상품 ID로 단일 상품 정보를 조회합니다.")
@GetMapping("/{productId}")
public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(...) {
    // ...
}
```

**권장 추가**:
```java
@Operation(
    summary = "상품 단건 조회", 
    description = "상품 ID로 단일 상품 정보를 조회합니다."
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음")
})
@GetMapping("/{productId}")
public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(
        @Parameter(description = "상품 ID", required = true) 
        @PathVariable UUID productId) {
    // ...
}
```

#### 4. BigDecimal 비교 개선

**현재 코드**:
```java
if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
    throw new IllegalArgumentException("단가는 0 이상이어야 합니다.");
}
```

**권장**:
```java
// BigDecimal 상수 사용
private static final BigDecimal ZERO = BigDecimal.ZERO;

public void updatePrice(BigDecimal price) {
    if (price == null || price.compareTo(ZERO) < 0) {
        throw new CustomException(ErrorCode.PRODUCT_INVALID_PRICE);
    }
    this.price = price;
}
```

#### 5. 권한 체크 로직 보완 (추후 작업)

**현재**:
```java
@PreAuthorize("hasRole('MASTER') or hasRole('HUB_MANAGER') or hasRole('COMPANY_MANAGER')")
```

**보완 필요** (Issue 등록 권장):
- HUB_MANAGER: 본인이 관리하는 허브의 상품만 수정/삭제 가능
- COMPANY_MANAGER: 본인 업체의 상품만 수정/삭제 가능
- Service 레이어에서 권한 검증 로직 추가 필요

```java
// 추후 구현 예시
@Transactional
public ProductUpdateResponse updateProduct(UUID productId, ProductUpdateRequest request, 
                                           UserPrincipal user) {
    Product product = getProductById(productId);
    
    // 권한 체크
    if (user.hasRole("HUB_MANAGER") && !product.getHubId().equals(user.getHubId())) {
        throw new CustomException(ErrorCode.FORBIDDEN_ACCESS);
    }
    if (user.hasRole("COMPANY_MANAGER") && !product.getCompanyId().equals(user.getCompanyId())) {
        throw new CustomException(ErrorCode.FORBIDDEN_ACCESS);
    }
    
    // 수정 로직
    // ...
}
```

#### 6. Eureka Dockerfile 위치 확인

**변경 사항**:
```
+ eureka-server/Dockerfile
```

**확인 필요**:
- Dockerfile 내용이 올바른지 확인
- Docker Compose 설정과 일치하는지 확인

## 📊 종합 평가

| 항목 | 점수 | 평가 |
|------|------|------|
| 기능 구현 완성도 | ⭐⭐⭐⭐☆ | 기본 CRUD 정상 동작 |
| 팀 표준 준수 | ⭐⭐⭐⭐⭐ | record DTO, DDD 패턴, SecurityConfigBase |
| 페이징 처리 | ⭐⭐⭐⭐⭐ | 검증 로직 우수 |
| FeignClient 통신 | ⭐☆☆☆☆ | 미구현 (TODO 상태) |
| 예외 처리 | ⭐⭐⭐☆☆ | IllegalArgumentException 사용 (임시) |
| 테스트 커버리지 | ⭐⭐☆☆☆ | 수동 API 테스트만 존재 |
| 문서화 | ⭐⭐⭐⭐☆ | Swagger 기본 문서화 완료 |

**총평**: 팀 표준 패턴을 철저히 준수하고 페이징 처리 로직이 우수함. 기본 CRUD는 정상 작동하나, FeignClient 통신 및 권한 체크 로직 추후 보완 필요.

## ✅ Merge 전 체크리스트

### 필수 수정 (Blocking Issues)
- [ ] **FeignClient 구현** (Hub, Company 검증 로직 추가) - PR #75 패턴 적용
- [ ] **Entity 예외 타입 변경** (IllegalArgumentException → CustomException)
- [ ] **ErrorCode 추가** (PRODUCT_INVALID_NAME, PRODUCT_INVALID_QUANTITY, PRODUCT_INVALID_PRICE)

### 강력 권장
- [ ] Controller 응답 타입 통일 (팀 표준 논의 후 결정)
- [ ] 단위 테스트 추가 (ProductService, Product Entity)
- [ ] Swagger 예외 응답 문서화 (`@ApiResponses`)
- [ ] TODO 주석 처리 (구현 완료 또는 Issue 등록)

### 선택 사항 (추후 개선)
- [ ] 권한 체크 로직 보완 (HUB_MANAGER, COMPANY_MANAGER 소유권 검증)
- [ ] BigDecimal 상수 사용
- [ ] 통합 테스트 추가 (TestContainers)
- [ ] Eureka Dockerfile 검증

## 🔗 Related Links
- PR: https://github.com/14th-anniv/one-for-logis/pull/65
- Issue #62: 상품 기본 CRUD
- Branch: `feature/#62-product-crud`
- Related: PR #75 (FeignClient 패턴)

## 👥 Author
- @sonaanweb

## 💬 To Reviewer
> 업체, 상품 모두 기본 CRUD 기능은 구현해두어 다른 모듈과 연결해보며 테스트 진행하는 동시에 추가로 보완할 예정입니다.  
> `✅ OpenFeign 통신 + 권한 체크 + 예외 처리 수정 + 캐싱처리등 `  
> 패키지 구조나 빠진 기능은 없을 지 점검 부탁 드립니다.

**리뷰어 답변**:
- 패키지 구조는 DDD 패턴 잘 준수했습니다! 👍
- 기본 CRUD 기능 정상 동작 확인했습니다.
- **FeignClient 통신은 PR #75 패턴 참고하여 구현하세요** (HubClientAdapter, CompanyClientAdapter)
- Entity 예외를 CustomException으로 변경하세요 (ErrorCode 추가 필요)
- 단위 테스트 추가를 강력히 권장합니다.
- 권한 체크 로직은 Issue로 등록하여 추후 작업하세요.

---
**리뷰어**: Claude (AI Code Reviewer)  
**리뷰 완료일**: 2025-11-11  
**상태**: 리뷰 완료 - FeignClient 구현 및 예외 처리 개선 필요
