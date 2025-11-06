package com.oneforlogis.company.presentation.controller;

import com.oneforlogis.common.api.ApiResponse;
import com.oneforlogis.company.application.dto.CompanyService;
import com.oneforlogis.company.presentation.dto.request.CompanyCreateRequest;
import com.oneforlogis.company.presentation.dto.response.CompanyCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanyService companyService;

    // todo: security 제외하고 작업 -> 연동 예정

    /**
     * 업체 등록
     */
    @PostMapping
    public ApiResponse<CompanyCreateResponse> createCompany(@RequestBody @Valid CompanyCreateRequest request){

        var response = companyService.createCompany(request);
        return ApiResponse.created(response);
    }
}
