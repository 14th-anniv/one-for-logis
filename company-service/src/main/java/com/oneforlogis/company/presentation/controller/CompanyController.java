package com.oneforlogis.company.presentation.controller;

import com.oneforlogis.common.api.ApiResponse;
import com.oneforlogis.common.security.UserPrincipal;
import com.oneforlogis.company.application.CompanyService;
import com.oneforlogis.company.application.dto.request.CompanyCreateRequest;
import com.oneforlogis.company.application.dto.request.CompanyUpdateRequest;
import com.oneforlogis.company.application.dto.response.CompanyCreateResponse;
import com.oneforlogis.company.application.dto.response.CompanyDetailResponse;
import com.oneforlogis.company.application.dto.response.CompanyUpdateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.bind.annotation.RestController;

@Tag(name="Companies", description = "업체 관리 API")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanyService companyService;

    // todo: security 제외하고 임의 작업 -> 개발 완료 후 로직 추가 [담당 허브/업체만 update]

    /**
     * 업체 등록
     */
    @Operation(summary = "업체 등록", description = "새로운 업체를 등록합니다. 'MASTER, HUB_MANAGER(담당 허브)' 권한이 필요합니다.")
    @PreAuthorize("hasRole('MASTER') or hasRole('HUB_MANAGER')")
    @PostMapping
    public ResponseEntity<ApiResponse<CompanyCreateResponse>> createCompany(@RequestBody @Valid CompanyCreateRequest request){

        var response = companyService.createCompany(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    /**
     * 업체 정보 수정
     */
    @Operation(summary = "업체 수정", description = "업체 정보를 수정합니다. 'MASTER, HUB_MANAGER(담당 허브), COMPANY_MANAGER(담당 업체)' 권한이 필요합니다.")
    @PreAuthorize("hasRole('MASTER') or hasRole('HUB_MANAGER') or hasRole('COMPANY_MANAGER')")
    @PatchMapping("/{companyId}")
    public ResponseEntity<ApiResponse<CompanyUpdateResponse>> updateCompany(@PathVariable UUID companyId,
            @RequestBody @Valid CompanyUpdateRequest request){

        var response = companyService.updateCompany(companyId, request);
        return ResponseEntity.ok().body(ApiResponse.success(response));
    }

    /**
     * 업체 정보 삭제
     * + noContent도 메세지를 띄움 (ok) 처리
     */
    @Operation(summary = "업체 삭제", description = "업체를 삭제합니다. 'MASTER, HUB_MANAGER(담당 허브)' 권한이 필요합니다.")
    @PreAuthorize("hasRole('MASTER') or hasRole('HUB_MANAGER')")
    @DeleteMapping("/{companyId}")
    public ResponseEntity<ApiResponse<Void>> deleteCompany(@PathVariable UUID companyId,
            @AuthenticationPrincipal UserPrincipal userPrincipal){

        String userName = userPrincipal.username();
        companyService.deleteCompany(companyId, userName);
        return ResponseEntity.ok().body(ApiResponse.noContent());
    }

    /**
     * 업체 단건 조회 - ALL
     */
    @GetMapping("/{companyId}")
    @Operation(summary = "업체 단건 조회", description = "업체 ID로 단일 업체 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<CompanyDetailResponse>> getCompanyDetail(@PathVariable UUID companyId) {
        var response = companyService.getCompanyDetail(companyId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
