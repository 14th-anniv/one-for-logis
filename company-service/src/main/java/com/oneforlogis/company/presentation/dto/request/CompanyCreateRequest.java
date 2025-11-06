package com.oneforlogis.company.presentation.dto.request;

import com.oneforlogis.company.domain.model.CompanyType;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CompanyCreateRequest(

        @NotBlank(message = "업체 이름을 입력해 주세요")
        String name,
        @NotBlank(message = "생산/수령 업체 중 선택해 주세요")
        CompanyType type,
        @NotBlank(message = "소속 허브를 입력해 주세요")
        UUID hubId,
        @NotBlank(message = "주소를 작성해 주세요")
        String address
) {}
