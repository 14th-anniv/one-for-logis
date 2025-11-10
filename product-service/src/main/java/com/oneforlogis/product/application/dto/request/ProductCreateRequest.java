package com.oneforlogis.product.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

@Schema
public record ProductCreateRequest(

        @NotBlank(message = "상품 이름을 입력해 주세요")
        String name,

        @NotNull(message = "수량을 입력해 주세요")
        @PositiveOrZero(message = "재고는 0 이상이어야 합니다")
        Integer quantity,

        @NotNull(message = "단가를 입력해 주세요")
        @PositiveOrZero(message = "단가는 0 이상이어야 합니다")
        BigDecimal price,

        @NotNull(message = "소속 허브를 입력해 주세요")
        UUID hubId,

        @NotNull(message = "소속 업체를 입력해 주세요")
        UUID companyId
) {
}
