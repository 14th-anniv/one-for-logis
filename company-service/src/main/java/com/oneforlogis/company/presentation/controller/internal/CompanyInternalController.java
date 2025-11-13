package com.oneforlogis.company.presentation.controller.internal;

import com.oneforlogis.company.application.CompanyService;
import com.oneforlogis.company.domain.model.Company;
import com.oneforlogis.company.presentation.controller.internal.dto.CompanyDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Internal companies", description = "내부용 업체 조회 API")
@RestController
@RequestMapping("/api/v1/internal/companies")
@RequiredArgsConstructor
public class CompanyInternalController {

    private final CompanyService companyService;

    @Operation(summary = "업체 id로 단일 조회", description = "업체 ID로 단일 업체 정보를 조회합니다.")
    @GetMapping("/{companyId}")
    public CompanyDto getCompanyInternal(@PathVariable UUID companyId) {
        Company company = companyService.getCompanyById(companyId);
        return CompanyDto.fromEntity(company);
    }
}
