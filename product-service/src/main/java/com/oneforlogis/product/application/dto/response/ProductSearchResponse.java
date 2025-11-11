package com.oneforlogis.product.application.dto.response;

import com.oneforlogis.product.domain.model.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "상품 검색 조회 응답 DTO")
public record ProductSearchResponse(

        @Schema(description = "상품 ID", example = "42b1c3b0-0b1a-4b1a-9b1a-0b1a4b1a0b1a")
        UUID id,

        @Schema(description = "상품명", example = "스파르타 모니터 27인치")
        String name,

        @Schema(description = "재고 수량", example = "100")
        Integer quantity,

        @Schema(description = "단가", example = "249000")
        BigDecimal price,

        @Schema(description = "소속 허브 ID", example = "31b1c3b0-0b1a-4b1a-9b1a-0b1a4b1a0b1a")
        UUID hubId,

        @Schema(description = "업체 ID", example = "11a1c3b0-2b1a-4b1a-9b1a-0b1a4b1a0b1a")
        UUID companyId
) {

    public static ProductSearchResponse from(Product product) {
        return new ProductSearchResponse(
                product.getId(),
                product.getName(),
                product.getQuantity(),
                product.getPrice(),
                product.getHubId(),
                product.getCompanyId()
        );
    }
}
