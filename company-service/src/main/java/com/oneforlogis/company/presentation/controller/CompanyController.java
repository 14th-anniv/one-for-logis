package com.oneforlogis.company.presentation.controller;

import com.oneforlogis.common.api.ApiResponse;
import com.oneforlogis.company.application.dto.CompanyService;
import com.oneforlogis.company.presentation.dto.request.CompanyCreateRequest;
import com.oneforlogis.company.presentation.dto.response.CompanyCreateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
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

    // todo: security 제외하고 임의 작업 -> 개발 완료 후 연동 예정

    /**
     * 업체 등록
     */
    @Operation(summary = "업체 등록", description = "새로운 업체를 등록합니다. 'MASTER, HUB_MANAGER(담당 허브)' 권한이 필요합니다.")
    @PreAuthorize("hasRole('MASTER') or hasRole('HUB_MANAGER')")
    @PostMapping
    public ApiResponse<CompanyCreateResponse> createCompany(@RequestBody @Valid CompanyCreateRequest request){

        var response = companyService.createCompany(request);
        return ApiResponse.created(response);
    }
}
