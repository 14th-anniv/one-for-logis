# PR #52: 업체 조회 단건 | 전체(검색) 기능 구현

## Issue Number
> closed #43

## 📝 Description

### Feat
- 업체 조회 단건 API 구현 (ID 기반)
- 업체 전체(검색) 조회 API 구현 (이름 검색 + 페이징)
  - 검색 필터: 업체 이름 (부분 검색 지원, 미입력 시 전체 조회)
  - 정렬 조건: page, size, sortBy, isAsc
  - 유효 size: 10, 30, 50 (이외 값 입력 시 10으로 고정)
  - page: 음수 입력 시 0으로 보정
- `Controller (클라이언트 요청 param 전달) -> Service (Paging 처리 -> 검증 및 조건 처리) -> Repository (DB 조회)` 순 처리

### Refactor
- DTO 위치 이동: `presentation.dto` → `application.dto` (팀 DDD 컨벤션 적용)
- Entity에서 DTO 의존성 제거 (Service에서 파라미터 분해 후 전달)
- Repository 패턴 개선: Domain interface와 Infrastructure JPA 구현 분리

### Added
- `CompanyUpdateRequest/Response`: 업체 수정용 DTO
- `CompanyDetailResponse`: 업체 상세 조회 (audit 필드 포함)
- `CompanySearchResponse`: 업체 검색 결과 (기본 필드만)
- `ErrorCode.COMPANY_NOT_FOUND`: 업체 미존재 예외
- Repository 메서드:
  - `findByIdAndDeletedFalse(UUID id)`
  - `findByDeletedFalse(Pageable pageable)`
  - `findByNameContainingAndDeletedFalse(String name, Pageable pageable)`

## 📊 변경 사항

### 변경 파일 (16개 파일, +378/-25)
- **ErrorCode**: `COMPANY_NOT_FOUND` 추가
- **CompanyService**: 페이징 헬퍼 메서드, 조회/수정/삭제 로직 추가
- **CompanyRepository**: 조회용 메서드 4개 추가
- **CompanyJpaRepository**: Spring Data JPA Query Method 3개 추가
- **CompanyController**: 
  - ResponseEntity 래핑으로 HTTP 상태코드 명시 (201 CREATED, 200 OK 구분)
  - 조회/수정/삭제 엔드포인트 추가
- **Company Entity**: 업데이트 메서드 추가 (`updateName`, `updateType`, `updateAddress`, `deleteCompany`)

### Request Parameters (검색 API)
| 이름 | 타입 | 필수여부 | 기본값 | 설명 |
|------|------|---------|--------|------|
| companyName | String | false | null | 업체 이름 (부분 검색) |
| page | int | false | 0 | 페이지 번호 (0부터 시작) |
| size | int | false | 10 | 페이지 크기 (10/30/50만 허용) |
| sortBy | String | false | createdAt | 정렬 필드명 |
| isAsc | boolean | false | false | 정렬 방향 (true: 오름차순, false: 내림차순) |

## 🌐 Test Result

### 전체 조회
- 페이징 처리 확인 (size=10, page=0)
- 정렬 조건 동작 확인 (createdAt DESC)

### 단건 조회
- UUID 기반 조회 성공
- 삭제된 업체 필터링 확인 (soft delete)

## 🔍 코드 리뷰 결과

### ✅ 잘된 점
1. **DDD 구조 완벽 준수**
   - DTO를 application layer로 이동
   - Repository interface는 domain, 구현은 infrastructure
   - Entity 비즈니스 로직 캡슐화

2. **record 사용 (불변성 보장)**
   - 모든 Response DTO를 record로 구현
   - static factory method 패턴 적용

3. **Soft delete 일관성 유지**
   - 모든 조회 메서드에 `deleted=false` 필터 적용
   - Repository 네이밍 명확: `findByIdAndDeletedFalse`

4. **페이징 validation**
   - size: 10/30/50만 허용, 기본값 10
   - page: 음수 입력 시 0으로 보정
   - 재사용 가능한 `createPageable()` 헬퍼 메서드

5. **Partial Update 지원**
   - `CompanyUpdateRequest`: null 필드는 수정하지 않음 (PATCH 의미)
   - 각 필드별 업데이트 메서드 분리 (`updateName`, `updateType`, `updateAddress`)

6. **팀 컨벤션 준수**
   - 코멘트 스타일: `//` 사용
   - Swagger `@Schema` 문서화
   - ErrorCode enum 활용

### ⚠️ 개선 필요 사항

#### 1. sortBy 필드 검증 누락 (중요도: 높음)
**현재 코드**:
```java
@RequestParam(defaultValue = "createdAt") String sortBy
```
- 임의의 문자열 입력 가능 → SQL Injection 위험 또는 에러 발생

**권장 수정**:
```java
private static final List<String> ALLOWED_SORT_FIELDS = 
    List.of("createdAt", "updatedAt", "name");

private Pageable createPageable(int page, int size, String sortBy, boolean isAsc) {
    String validatedSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) 
        ? sortBy : "createdAt";
    // ...
}
```

#### 2. DELETE 응답 코드 불일치 (중요도: 중간)
**현재 코드**:
```java
// 204 No Content 의미의 응답을 200 OK로 반환
return ResponseEntity.ok().body(ApiResponse.noContent());
```

**권장 수정** (택 1):
```java
// Option 1: 진짜 204 반환
return ResponseEntity.noContent().build();

// Option 2: 200 + 성공 메시지 (현재 팀 스타일 유지)
return ResponseEntity.ok().body(ApiResponse.success("업체가 삭제되었습니다."));
```

#### 3. CompanyUpdateRequest validation 부족 (중요도: 중간)
**현재 코드**:
```java
public record CompanyUpdateRequest(
    String name,
    String type,
    String address
) {}
```

**권장 수정**:
```java
public record CompanyUpdateRequest(
    @Size(min = 1, max = 100, message = "업체명은 1-100자여야 합니다")
    String name,
    
    @Pattern(regexp = "SUPPLIER|RECEIVER", message = "유효하지 않은 업체 타입입니다")
    String type,
    
    @Size(max = 255, message = "주소는 255자 이하여야 합니다")
    String address
) {}
```

#### 4. Update 로직 address 검증 누락 (중요도: 낮음)
**현재 코드**:
```java
if (request.address() != null) {
    company.updateAddress(request.address());
}
```
- 빈 문자열("")도 업데이트됨

**권장 수정**:
```java
if (request.address() != null && !request.address().isBlank()) {
    company.updateAddress(request.address());
}
```

#### 5. 검색 성능 고려 (중요도: 낮음, 추후 대응)
- `findByNameContainingAndDeletedFalse`: `%name%` LIKE 검색 → 인덱스 미활용
- 대용량 데이터 시 성능 저하 가능
- **대응**: `name` 컬럼 인덱스 추가 또는 Full-text search 도입 검토

### 🎯 로컬 환경 이슈
- `CompanyRepositoryImpl.java`: 로컬 파일이 PR 브랜치와 불일치
- **원인**: fetch는 되었지만 로컬 체크아웃 안 됨
- **해결**: `git checkout origin/feature/#43-get-company -- company-service/src/main/java/com/oneforlogis/company/infrastructure/persistence/`

## 📊 종합 평가

| 항목 | 점수 | 평가 |
|------|------|------|
| DDD 구조 준수 | ⭐⭐⭐⭐⭐ | 완벽한 레이어 분리 |
| 팀 컨벤션 준수 | ⭐⭐⭐⭐⭐ | record, 패키지 구조, 코멘트 스타일 |
| 비즈니스 로직 | ⭐⭐⭐⭐☆ | Soft delete, 페이징 우수 |
| 에러 처리 | ⭐⭐⭐⭐☆ | CustomException 활용 |
| 성능 최적화 | ⭐⭐⭐☆☆ | LIKE 검색 인덱스 고려 필요 |
| Validation | ⭐⭐⭐☆☆ | sortBy, address 검증 보완 필요 |

**총평**: 전반적으로 매우 우수한 코드. DDD 원칙과 팀 컨벤션을 완벽히 준수. sortBy 검증만 추가하면 즉시 Approve 가능.

## ✅ Merge 전 체크리스트

### 필수 수정 (Approve 조건)
- [ ] `sortBy` 필드 화이트리스트 검증 추가
- [ ] DELETE 응답 코드 통일 (204 or 200+message)
- [ ] dev 브랜치 머지 (현재 dirty 상태 해결)

### 권장 수정 (선택)
- [ ] CompanyUpdateRequest validation 강화 (`@Size`, `@Pattern`)
- [ ] address 빈 문자열 검증 추가
- [ ] Controller 파라미터 `@Parameter` 문서화 강화

### 추후 고려 (별도 이슈)
- [ ] 검색 성능 개선 (DB 인덱스 추가 논의)
- [ ] Hub 정보 조인 시 N+1 방지 전략
- [ ] 전체 조회 시 캐싱 적용 (Redis) 검토

## 🔗 Related Links
- PR: https://github.com/14th-anniv/one-for-logis/pull/52
- Issue #43: 업체 조회 기능 구현
- Branch: `feature/#43-get-company`

## 👥 Reviewers
- Sp-PJS
- GoodNyong
- dain391
- AlkongDalkonge

---
**작성자**: @sonaanweb  
**리뷰 완료일**: 2025-11-10  
**상태**: 리뷰 완료 - 수정 요청 (sortBy validation)
