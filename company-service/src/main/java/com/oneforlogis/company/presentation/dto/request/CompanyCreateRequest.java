package com.oneforlogis.company.presentation.dto.request;

import com.oneforlogis.company.domain.model.CompanyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "업체 등록 요청 DTO")
public record CompanyCreateRequest(

        @Schema(description = "업체명", example = "스파르타 산업")
        @NotBlank(message = "업체 이름을 입력해 주세요")
        String name,

        @Schema(description = "업체 타입", example = "SUPPLIER", allowableValues = {"SUPPLIER", "RECEIVER"})
        @NotNull(message = "생산/수령 업체 중 선택해 주세요")
        String type,

        @Schema(description = "소속 허브 ID", example = "31b1c3b0-0b1a-4b1a-9b1a-0b1a4b1a0b1a")
        @NotNull(message = "소속 허브를 입력해 주세요")
        UUID hubId,

        @Schema(description = "업체 주소", example = "서울특별시 강남구 테헤란로 123")
        @NotBlank(message = "주소를 작성해 주세요")
        String address
) {}
