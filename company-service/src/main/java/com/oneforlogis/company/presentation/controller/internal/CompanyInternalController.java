package com.oneforlogis.company.presentation.controller.internal;

import com.oneforlogis.company.application.CompanyService;
import com.oneforlogis.company.domain.model.Company;
import com.oneforlogis.company.presentation.controller.internal.dto.CompanyDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/companies")
@RequiredArgsConstructor
public class CompanyInternalController {

    private final CompanyService companyService;

    @GetMapping("/{companyId}")
    public CompanyDto getCompanyInternal(@PathVariable UUID companyId) {
        Company company = companyService.getCompanyById(companyId);
        return CompanyDto.fromEntity(company);
    }
}
