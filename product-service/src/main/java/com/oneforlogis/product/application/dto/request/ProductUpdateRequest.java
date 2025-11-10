package com.oneforlogis.product.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

@Schema(description = "상품 수정 요청 DTO")
public record ProductUpdateRequest(

        @Schema(description = "상품 이름")
        String name,

        @Schema(description = "재고 수량 (0 이상)")
        @PositiveOrZero(message = "재고는 0 이상이어야 합니다.")
        Integer quantity,

        @Schema(description = "단가 (0 이상)")
        @PositiveOrZero(message = "단가는 0 이상이어야 합니다.")
        BigDecimal price
) {}
