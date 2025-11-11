package com.oneforlogis.product.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "상품 생성 요청 DTO")
public record ProductCreateRequest(

        @Schema(description = "상품 이름", example = "스파르타 모니터")
        @NotBlank(message = "상품 이름을 입력해 주세요.")
        String name,

        @Schema(description = "상품 재고 수량 (0 이상)", example = "스파르타 모니터")
        @NotNull(message = "수량을 입력해 주세요.")
        @PositiveOrZero(message = "재고는 0 이상이어야 합니다.")
        Integer quantity,

        @Schema(description = "상품 단가 (0 이상)", example = "250000")
        @NotNull(message = "단가를 입력해 주세요.")
        @PositiveOrZero(message = "단가는 0 이상이어야 합니다.")
        BigDecimal price,

        @Schema(description = "소속 허브", example = "31b1c3b0-0b1a-4b1a-9b1a-0b1a4b1a0b1a")
        @NotNull(message = "소속 허브를 입력해 주세요.")
        UUID hubId,

        @Schema(description = "소속 업체", example = "54b1c3b0-0b1a-4b1a-9b1a-0b1a4b1a0b1a")
        @NotNull(message = "소속 업체를 입력해 주세요.")
        UUID companyId
) {
}
