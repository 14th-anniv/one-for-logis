package com.oneforlogis.company.application.dto.response;

import com.oneforlogis.company.domain.model.Company;
import com.oneforlogis.company.domain.model.CompanyType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "업체 검색 조회 응답 DTO")
public record CompanySearchResponse(
        @Schema(description = "업체 ID", example = "42b1c3b0-0b1a-4b1a-9b1a-0b1a4b1a0b1a")
        UUID id,

        @Schema(description = "업체명", example = "스파르타 산업")
        String name,

        @Schema(description = "업체 타입", example = "SUPPLIER, RECEIVER")
        CompanyType type,

        @Schema(description = "소속 허브 ID", example = "31b1c3b0-0b1a-4b1a-9b1a-0b1a4b1a0b1a")
        UUID hubId,

        @Schema(description = "업체 주소", example = "서울특별시 강남구 테헤란로 123")
        String address
) {
    public static CompanySearchResponse from(Company company){
        return new CompanySearchResponse(
                company.getId(),
                company.getName(),
                company.getType(),
                company.getHubId(),
                company.getAddress()
        );
    }
}
