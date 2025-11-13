package com.oneforlogis.company.presentation.controller.internal.dto;

import com.oneforlogis.company.domain.model.Company;
import com.oneforlogis.company.domain.model.CompanyType;
import java.util.UUID;

public record CompanyDto(
        UUID id,
        CompanyType type,
        String name,
        String address
) {
    public static CompanyDto fromEntity(Company company) {
        return new CompanyDto(
                company.getId(),
                company.getType(),
                company.getName(),
                company.getAddress()
        );
    }
}
