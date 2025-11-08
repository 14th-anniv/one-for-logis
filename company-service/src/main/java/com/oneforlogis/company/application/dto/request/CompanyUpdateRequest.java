package com.oneforlogis.company.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "업체 수정 요청 DTO")
public record CompanyUpdateRequest(

        @Schema(description = "수정 업체명", example = "스파르타 산업")
        String name,

        @Schema(description = "수정 업체 타입", example = "SUPPLIER", allowableValues = {"SUPPLIER", "RECEIVER"})
        String type,

        @Schema(description = "수정 업체 주소", example = "서울특별시 강남구 테헤란로 123")
        String address
) {}
