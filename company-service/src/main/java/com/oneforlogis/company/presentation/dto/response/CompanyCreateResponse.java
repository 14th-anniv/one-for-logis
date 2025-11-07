package com.oneforlogis.company.presentation.dto.response;

import com.oneforlogis.company.domain.model.Company;
import com.oneforlogis.company.domain.model.CompanyType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "업체 등록 응답 DTO")
public record CompanyCreateResponse(

        @Schema(description = "등록 업체 ID", example = "42b1c3b0-0b1a-4b1a-9b1a-0b1a4b1a0b1a")
        UUID id,

        @Schema(description = "업체명", example = "스파르타 산업")
        String name,

        @Schema(description = "업체 타입", example = "SUPPLIER, RECEIVER")
        CompanyType type,

        @Schema(description = "소속 허브 ID", example = "31b1c3b0-0b1a-4b1a-9b1a-0b1a4b1a0b1a")
        UUID hubId,

        @Schema(description = "업체 주소", example = "서울특별시 강남구 테헤란로 123")
        String address,

        @Schema(description = "생성자", example = "userId")
        String createdBy,

        @Schema(description = "생성일시", example = "2025-11-06 T12:00:00")
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
