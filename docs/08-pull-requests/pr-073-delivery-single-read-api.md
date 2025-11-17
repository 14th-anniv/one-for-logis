# PR #73: 배송 단건 조회 API 구현

## Issue Number
> closed #70

## 📝 Description

### Delivery Service - 배송 단건 조회 API
- 배송 ID로 단일 배송 정보 조회 기능
- 존재하지 않는 배송 시 `CustomException(DELIVERY_NOT_FOUND)` 예외 처리
- `DeliveryResponse` DTO 반환
- 전역 예외 처리 적용 (`GlobalExceptionHandler`)
- Controller 단위 테스트 작성

## 📊 변경 사항

### 변경 파일 (5개 파일, +214/-3)

#### Common-lib
- **ErrorCode**: `DELIVERY_NOT_FOUND` 에러 코드 추가

#### Application Layer
- **DeliveryResponse**: 
  - 배송 정보 응답 DTO 추가
  - Builder 패턴 사용
  - `from()` factory method로 Entity → DTO 변환
  - null-safe 기본값 처리 (estimatedDistanceKm=0.0, estimatedDurationMin=0 등)

- **DeliveryService**: 
  - `getOne(UUID deliveryId)` 메서드 추가
  - 존재하지 않는 배송 시 `CustomException(DELIVERY_NOT_FOUND)` 발생

#### Domain Layer
- **DeliveryRepository**: 
  - `findByDeliveryId(UUID deliveryId)` 메서드 추가

#### Presentation Layer
- **DeliveryController**: 
  - `GET /api/v1/deliveries/{deliveryId}` 엔드포인트 추가
  - `ResponseEntity<DeliveryResponse>` 반환

- **DeliveryExceptionHandler**: 
  - `IllegalArgumentException` 핸들러 추가

#### Test
- **DeliveryControllerTest**: 
  - 단건 조회 성공 케이스
  - 단건 조회 실패 (존재하지 않는 ID) 케이스

## 🌐 Test Result

### 테스트 성공 확인
- 단건 조회 성공: `200 OK` + 배송 상세 정보 반환
- 단건 조회 실패: 존재하지 않는 ID 요청 시 `404 NOT_FOUND` + "해당 배송을 찾을 수 없습니다." 응답
- `DeliveryControllerTest` 2개 테스트 통과

## 🔍 코드 리뷰 결과

### ✅ 잘된 점

#### 1. 팀 표준 준수
- `CustomException(ErrorCode)` 사용으로 일관된 에러 처리
- `DomainVerb + Response` DTO 네이밍 컨벤션 준수
- Builder 패턴으로 안전한 객체 생성

#### 2. 테스트 커버리지
- Controller 단위 테스트 작성 (성공/실패 케이스)
- Mock을 활용한 격리된 테스트 환경

#### 3. Null-safe 처리
- Builder에서 null 체크 후 기본값 설정

### 🚨 Critical Issues (필수 수정)

#### 1. 중복 메서드 (심각도: 높음)

**현재 코드** (`DeliveryRepository.java`):
```java
Optional<Delivery> findByOrderId(UUID orderId);
Optional<Delivery> findByDeliveryId(UUID deliveryId); // ❌ 불필요
```

**문제점**:
- `findByDeliveryId(UUID deliveryId)`는 JPA의 기본 `findById(UUID id)`와 동일 기능
- 불필요한 코드 중복

**해결책**:
```java
// DeliveryRepository - 중복 메서드 삭제
Optional<Delivery> findByOrderId(UUID orderId);
// findByDeliveryId() 삭제

// DeliveryService 수정
public DeliveryResponse getOne(UUID deliveryId) {
    Delivery delivery = deliveryRepository.findById(deliveryId)
        .orElseThrow(() -> new CustomException(ErrorCode.DELIVERY_NOT_FOUND));
    return DeliveryResponse.from(delivery);
}
```

#### 2. Entity 타입 불일치 (심각도: 높음)

**현재 코드** (`DeliveryResponse.java`):
```java
public static DeliveryResponse from(Delivery d) {
    return DeliveryResponse.builder()
        .fromHubId(UUID.fromString(d.getStartHubId()))      // String → UUID
        .toHubId(UUID.fromString(d.getDestinationHubId()))  // String → UUID
        .deliveryStaffId(
            d.getDeliveryStaffId() != null 
                ? Long.valueOf(d.getDeliveryStaffId())      // String → Long
                : null)
        .build();
}
```

**문제점**:
- Entity는 `String` 타입인데 Response DTO는 `UUID`/`Long` 타입
- `UUID.fromString()`: IllegalArgumentException 위험
- `Long.valueOf(String)`: NumberFormatException 위험
- 타입 변환 오버헤드

**원인**:
- Entity 설계 단계에서 타입 설정 불일치
- hubId는 UUID인데 String으로 저장됨

**해결책**:
```java
// Delivery Entity 수정
@Column(name = "start_hub_id", nullable = false)
private UUID startHubId;  // String → UUID 변경

@Column(name = "destination_hub_id", nullable = false)
private UUID destinationHubId;  // String → UUID 변경

@Column(name = "delivery_staff_id")
private Long deliveryStaffId;  // String → Long 변경

// DeliveryResponse.from() 단순화
public static DeliveryResponse from(Delivery d) {
    return DeliveryResponse.builder()
        .fromHubId(d.getStartHubId())        // 타입 변환 불필요
        .toHubId(d.getDestinationHubId())    // 타입 변환 불필요
        .deliveryStaffId(d.getDeliveryStaffId())  // 타입 변환 불필요
        .build();
}
```

#### 3. Response DTO 기본값 처리 위치 (심각도: 중간)

**현재 코드** (`DeliveryResponse.java`):
```java
@Builder
public DeliveryResponse(...) {
    this.estimatedDistanceKm = (estimatedDistanceKm != null) ? estimatedDistanceKm : 0.0;
    this.estimatedDurationMin = (estimatedDurationMin != null) ? estimatedDurationMin : 0;
    this.arrivedDestinationHub = (arrivedDestinationHub != null) ? arrivedDestinationHub : false;
}
```

**문제점**:
- Builder 생성자에 비즈니스 로직 포함
- DTO가 Entity의 null 처리 책임을 짐
- Response 계층에서 도메인 규칙 결정

**권장 수정**:
```java
// Option 1: Entity에서 기본값 보장
@Column(name = "estimated_distance_km", nullable = false, columnDefinition = "DOUBLE PRECISION DEFAULT 0.0")
private Double estimatedDistanceKm = 0.0;

@Column(name = "estimated_duration_min", nullable = false, columnDefinition = "INT DEFAULT 0")
private Integer estimatedDurationMin = 0;

@Column(name = "arrived_destination_hub", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
private Boolean arrivedDestinationHub = false;

// DeliveryResponse Builder는 단순하게
@Builder
public DeliveryResponse(...) {
    this.estimatedDistanceKm = estimatedDistanceKm;
    this.estimatedDurationMin = estimatedDurationMin;
    this.arrivedDestinationHub = arrivedDestinationHub;
}

// Option 2: @Builder.Default 사용
@Builder
public class DeliveryResponse {
    private final UUID id;
    private final UUID orderId;
    @Builder.Default
    private final Double estimatedDistanceKm = 0.0;
    @Builder.Default
    private final Integer estimatedDurationMin = 0;
    @Builder.Default
    private final Boolean arrivedDestinationHub = false;
    // ...
}
```

#### 4. DeliveryExceptionHandler 불필요 (심각도: 중간)

**현재 코드** (`DeliveryExceptionHandler.java`):
```java
@RestControllerAdvice
public class DeliveryExceptionHandler {
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(...) {
        // ...
    }
}
```

**문제점**:
- `CustomException`을 사용하는데 `IllegalArgumentException` 핸들러 추가
- `GlobalExceptionHandler`와 중복
- 서비스별 예외 핸들러 분산 → 유지보수 어려움
- 실제 Service는 `CustomException`을 던지는데 테스트만 `IllegalArgumentException` 사용

**해결책**:
```bash
# DeliveryExceptionHandler.java 삭제
# GlobalExceptionHandler가 모든 예외 통합 처리
```

#### 5. 테스트 Mock 불일치 (심각도: 중간)

**현재 코드** (`DeliveryControllerTest.java`):
```java
@Test
@DisplayName("배송 단건 조회 실패 - 존재하지 않는 ID")
void getDeliveryById_notFound() throws Exception {
    UUID deliveryId = UUID.randomUUID();
    Mockito.when(deliveryService.getOne(any(UUID.class)))
        .thenThrow(new IllegalArgumentException("해당 배송을 찾을 수 없습니다."));
    
    mockMvc.perform(get("/api/v1/deliveries/{deliveryId}", deliveryId)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is4xxClientError());
}
```

**문제점**:
- 실제 Service는 `CustomException(ErrorCode.DELIVERY_NOT_FOUND)` 발생
- 테스트는 `IllegalArgumentException` 발생
- 테스트가 실제 동작을 정확히 검증하지 못함

**해결책**:
```java
@Test
@DisplayName("배송 단건 조회 실패 - 존재하지 않는 ID")
void getDeliveryById_notFound() throws Exception {
    // given
    UUID deliveryId = UUID.randomUUID();
    Mockito.when(deliveryService.getOne(any(UUID.class)))
        .thenThrow(new CustomException(ErrorCode.DELIVERY_NOT_FOUND));
    
    // when & then
    mockMvc.perform(get("/api/v1/deliveries/{deliveryId}", deliveryId)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("배송을 찾을 수 없습니다."));
}
```

#### 6. Controller 응답 타입 불일치 (심각도: 높음)

**현재 코드** (`DeliveryController.java`):
```java
@GetMapping("/{deliveryId}")
public ResponseEntity<DeliveryResponse> getDeliveryById(
        @PathVariable UUID deliveryId
) {
    DeliveryResponse response = deliveryService.getOne(deliveryId);
    return ResponseEntity.ok(response);
}
```

**문제점**:
- 팀 표준 `ApiResponse` 미사용
- 다른 서비스(hub-service, company-service)와 응답 형식 불일치
- 에러 응답은 `ApiResponse`인데 성공 응답만 다름

**권장 수정**:
```java
// DeliveryController.java
@GetMapping("/{deliveryId}")
public ApiResponse<DeliveryResponse> getDeliveryById(
        @PathVariable UUID deliveryId
) {
    DeliveryResponse response = deliveryService.getOne(deliveryId);
    return ApiResponse.success(response);
}
```

### ⚠️ 개선 권장 사항

#### 1. Swagger 문서화 누락

**현재 코드**:
```java
@GetMapping("/{deliveryId}")
public ResponseEntity<DeliveryResponse> getDeliveryById(...) {
    // ...
}
```

**권장 수정**:
```java
@Operation(
    summary = "배송 단건 조회", 
    description = "배송 ID로 배송 정보를 조회합니다."
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "404", description = "배송을 찾을 수 없음")
})
@GetMapping("/{deliveryId}")
public ApiResponse<DeliveryResponse> getDeliveryById(
        @Parameter(description = "배송 ID", required = true) 
        @PathVariable UUID deliveryId
) {
    // ...
}
```

#### 2. DeliveryResponse 필드 누락 확인

**현재 코드** (`DeliveryResponse.java`):
```java
public static DeliveryResponse from(Delivery d) {
    return DeliveryResponse.builder()
        .id(d.getDeliveryId())
        .orderId(d.getOrderId())
        .status(d.getStatus().name())
        .fromHubId(UUID.fromString(d.getStartHubId()))
        .toHubId(UUID.fromString(d.getDestinationHubId()))
        .destinationHubArrivedAt(null)  // 항상 null
        .deliveryStaffId(...)
        .receiverName(d.getReceiverName())
        .receiverAddress(d.getReceiverAddress())
        .receiverSlackId(d.getReceiverSlackId())
        .build();
}
```

**문제점**:
- `estimatedDistanceKm`, `estimatedDurationMin`, `arrivedDestinationHub` 필드가 from() 메서드에 누락
- 항상 기본값(0.0, 0, false)만 반환
- Entity에 해당 필드가 있다면 매핑 필요

**권장 수정**:
```java
public static DeliveryResponse from(Delivery d) {
    return DeliveryResponse.builder()
        .id(d.getDeliveryId())
        .orderId(d.getOrderId())
        .status(d.getStatus().name())
        .fromHubId(d.getStartHubId())
        .toHubId(d.getDestinationHubId())
        .estimatedDistanceKm(d.getEstimatedDistanceKm())
        .estimatedDurationMin(d.getEstimatedDurationMin())
        .arrivedDestinationHub(d.getArrivedDestinationHub())
        .destinationHubArrivedAt(d.getDestinationHubArrivedAt())
        .deliveryStaffId(d.getDeliveryStaffId())
        .receiverName(d.getReceiverName())
        .receiverAddress(d.getReceiverAddress())
        .receiverSlackId(d.getReceiverSlackId())
        .build();
}
```

#### 3. 테스트 커버리지 확장

**현재**: Controller 단위 테스트만 존재

**추가 권장**:
```java
// DeliveryServiceTest.java
@Test
@DisplayName("배송 조회 성공")
void getOne_success() {
    // given
    UUID deliveryId = UUID.randomUUID();
    Delivery mockDelivery = Delivery.builder()...build();
    when(deliveryRepository.findById(deliveryId))
        .thenReturn(Optional.of(mockDelivery));
    
    // when
    DeliveryResponse response = deliveryService.getOne(deliveryId);
    
    // then
    assertThat(response.getId()).isEqualTo(deliveryId);
    verify(deliveryRepository, times(1)).findById(deliveryId);
}

@Test
@DisplayName("배송 조회 실패 - 존재하지 않는 ID")
void getOne_notFound() {
    // given
    UUID deliveryId = UUID.randomUUID();
    when(deliveryRepository.findById(deliveryId))
        .thenReturn(Optional.empty());
    
    // when & then
    assertThatThrownBy(() -> deliveryService.getOne(deliveryId))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DELIVERY_NOT_FOUND);
}
```

#### 4. DeliveryResponse 불변성 강화

**현재 코드**:
```java
@Getter
public class DeliveryResponse {
    private final UUID id;
    // ...
}
```

**권장**: 팀 표준에 따라 `record` 사용 (hub-service, company-service 패턴)
```java
public record DeliveryResponse(
    UUID id,
    UUID orderId,
    String status,
    UUID fromHubId,
    UUID toHubId,
    Double estimatedDistanceKm,
    Integer estimatedDurationMin,
    Boolean arrivedDestinationHub,
    LocalDateTime destinationHubArrivedAt,
    Long deliveryStaffId,
    String receiverName,
    String receiverAddress,
    String receiverSlackId
) {
    public static DeliveryResponse from(Delivery d) {
        return new DeliveryResponse(
            d.getDeliveryId(),
            d.getOrderId(),
            d.getStatus().name(),
            d.getStartHubId(),
            d.getDestinationHubId(),
            d.getEstimatedDistanceKm(),
            d.getEstimatedDurationMin(),
            d.getArrivedDestinationHub(),
            d.getDestinationHubArrivedAt(),
            d.getDeliveryStaffId(),
            d.getReceiverName(),
            d.getReceiverAddress(),
            d.getReceiverSlackId()
        );
    }
}
```

## 📊 종합 평가

| 항목 | 점수 | 평가 |
|------|------|------|
| 기능 구현 완성도 | ⭐⭐⭐⭐☆ | 기본 조회 기능 정상 동작 |
| 에러 처리 | ⭐⭐⭐☆☆ | CustomException 사용, 테스트 불일치 |
| 팀 표준 준수 | ⭐⭐⭐☆☆ | ApiResponse 미사용, DTO 패턴 불일치 |
| 테스트 커버리지 | ⭐⭐⭐☆☆ | Controller 테스트만 존재 |
| 코드 품질 | ⭐⭐⭐☆☆ | Entity 타입 불일치, 중복 메서드 |
| 문서화 | ⭐⭐☆☆☆ | Swagger 문서화 누락 |

**총평**: 기본 기능은 정상 작동하나, Entity 타입 불일치와 팀 표준 ApiResponse 미사용 등 개선 필요 사항 다수. 수정 후 Approve 가능.

## ✅ Merge 전 체크리스트

### 필수 수정 (Blocking Issues)
- [ ] **중복 메서드 삭제** (`findByDeliveryId()` → `findById()` 사용)
- [ ] **Entity 타입 일치** (hubId: String → UUID, staffId: String → Long)
- [ ] **DeliveryExceptionHandler 삭제** (GlobalExceptionHandler 사용)
- [ ] **Controller ApiResponse 적용** (팀 표준 준수)
- [ ] **테스트 Mock 수정** (IllegalArgumentException → CustomException)

### 강력 권장
- [ ] DeliveryResponse 필드 누락 확인 및 매핑 추가
- [ ] Swagger 문서화 추가 (`@Operation`, `@ApiResponses`)
- [ ] Service 계층 단위 테스트 추가
- [ ] Entity 기본값 설정 (Builder 로직 제거)

### 선택 사항 (추후 개선)
- [ ] DeliveryResponse를 `record`로 변경 (팀 표준 패턴)
- [ ] 통합 테스트 추가 (TestContainers)
- [ ] 성능 테스트 (대량 데이터 조회)

## 🔗 Related Links
- PR: https://github.com/14th-anniv/one-for-logis/pull/73
- Issue #70: 배송 단건 조회 API
- Branch: `feature/#70-delivery-single-read`

## 👥 Author
- @dain391

## 💬 To Reviewer
> `DeliveryResponse.from()` 변환 구조 개선 필요 여부 확인 부탁드립니다.  
> PR 확인되면 목록 조회, 수정, 삭제도 바로 올리겠습니다!

**리뷰어 답변**:
- `DeliveryResponse.from()` 타입 변환 문제가 Critical합니다. Entity 타입을 UUID/Long으로 수정하세요.
- Controller에 ApiResponse 적용 필수입니다 (팀 표준).
- DeliveryExceptionHandler는 삭제하고 GlobalExceptionHandler 사용하세요.
- 위 수정 후 목록 조회/수정/삭제 PR 올려주세요! 👍

---
**리뷰어**: Claude (AI Code Reviewer)  
**리뷰 완료일**: 2025-11-11  
**상태**: 리뷰 완료 - Critical 수정 필요 (Entity 타입, ApiResponse, 중복 메서드)
