package com.oneforlogis.company.presentation.dto.response;

import com.oneforlogis.company.domain.model.Company;
import com.oneforlogis.company.domain.model.CompanyType;
import java.time.LocalDateTime;
import java.util.UUID;

public record CompanyCreateResponse(

        UUID id,
        String name,
        CompanyType type,
        UUID hubId,
        String address,

        String createdBy,
        LocalDateTime createdAt
) {
    public static CompanyCreateResponse from(Company company){
        return new CompanyCreateResponse(
                company.getId(),
                company.getName(),
                company.getType(),
                company.getHubId(),
                company.getAddress(),
                company.getCreatedBy(),
                company.getCreatedAt()
        );
    }
}
