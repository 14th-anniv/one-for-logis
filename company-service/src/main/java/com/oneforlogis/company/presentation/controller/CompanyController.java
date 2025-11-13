package com.oneforlogis.company.presentation.controller;

import com.oneforlogis.common.api.ApiResponse;
import com.oneforlogis.common.api.PageResponse;
import com.oneforlogis.common.security.UserPrincipal;
import com.oneforlogis.company.application.CompanyService;
import com.oneforlogis.company.application.dto.request.CompanyCreateRequest;
import com.oneforlogis.company.application.dto.request.CompanyUpdateRequest;
import com.oneforlogis.company.application.dto.response.CompanyCreateResponse;
import com.oneforlogis.company.application.dto.response.CompanyDetailResponse;
import com.oneforlogis.company.application.dto.response.CompanySearchResponse;
import com.oneforlogis.company.application.dto.response.CompanyUpdateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name="Companies", description = "업체 관리 API")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanyService companyService;

    /**
     * 업체 등록
     */
    @Operation(summary = "업체 등록", description = "새로운 업체를 등록합니다. 'MASTER, HUB_MANAGER' 권한이 필요합니다.")
    @PreAuthorize("hasRole('MASTER') or hasRole('HUB_MANAGER')")
    @PostMapping
    public ResponseEntity<ApiResponse<CompanyCreateResponse>> createCompany(@RequestBody @Valid CompanyCreateRequest request){
        var response = companyService.createCompany(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    /**
     * 업체 정보 수정
     */
    @Operation(summary = "업체 수정", description = "업체 정보를 수정합니다. 'MASTER, HUB_MANAGER, COMPANY_MANAGER' 권한이 필요합니다.")
    @PreAuthorize("hasRole('MASTER') or hasRole('HUB_MANAGER') or hasRole('COMPANY_MANAGER')")
    @PatchMapping("/{companyId}")
    public ApiResponse<CompanyUpdateResponse> updateCompany(@PathVariable UUID companyId,
            @RequestBody @Valid CompanyUpdateRequest request){
        var response = companyService.updateCompany(companyId, request);
        return ApiResponse.success(response);
    }

    /**
     * 업체 정보 삭제
     */
    @Operation(summary = "업체 삭제", description = "업체를 삭제합니다. 'MASTER, HUB_MANAGER' 권한이 필요합니다.")
    @PreAuthorize("hasRole('MASTER') or hasRole('HUB_MANAGER')")
    @DeleteMapping("/{companyId}")
    public ResponseEntity<ApiResponse<Void>> deleteCompany(@PathVariable UUID companyId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        companyService.deleteCompany(companyId, userPrincipal.username());
        return ResponseEntity.noContent().build();
    }

    /**
     * 업체 단건 조회
     */
    @Operation(summary = "업체 단건 조회", description = "업체 ID로 단일 업체 정보를 조회합니다.")
    @GetMapping("/{companyId}")
    public ApiResponse<CompanyDetailResponse> getCompanyDetail(@PathVariable UUID companyId) {
        var response = companyService.getCompanyDetail(companyId);
        return ApiResponse.success(response);
    }

    /**
     * 업체 검색 기반 조회 (전체 & 이름)
     * @param companyName 업체 이름 ( null = 전체 조회 )
     * @param page 페이지 번호 (0부터 시작)
     * @param size 한 페이지에 가져올 개수 (유효 size: 10, 30, 50; 이외 값 입력시 10으로 고정)
     * @param sortBy 정렬할 필드명 (ex. "createdAt")
     * @param isAsc 정렬 방향 (true: 오름차순, false: 내림차순(default))
     * @return 페이징된 DTO
     */
    @Operation(summary = "업체 전체(검색) 조회", description = "업체 리스트를 조회합니다.")
    @GetMapping
    public ApiResponse<PageResponse<CompanySearchResponse>> getCompanies(
            @RequestParam(required = false) String companyName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "false") boolean isAsc
    ) {
        Page<CompanySearchResponse> companyPage = companyService.getCompanies(companyName, page, size, sortBy, isAsc)
                .map(CompanySearchResponse::from);
        return ApiResponse.success(PageResponse.fromPage(companyPage));
    }
}