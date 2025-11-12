package com.oneforlogis.product.application.dto.response;

import com.oneforlogis.product.domain.model.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "상품 수정 응답 DTO")
public record ProductUpdateResponse(

        @Schema(description = "상품 ID", example = "42b1c3b0-0b1a-4b1a-9b1a-0b1a4b1a0b1a")
        UUID id,

        @Schema(description = "상품명", example = "스파르타 모니터 27인치")
        String name,

        @Schema(description = "재고 수량", example = "120")
        Integer quantity,

        @Schema(description = "단가", example = "249000")
        BigDecimal price,

        @Schema(description = "소속 허브 ID", example = "31b1c3b0-0b1a-4b1a-9b1a-0b1a4b1a0b1a")
        UUID hubId,

        @Schema(description = "업체 ID", example = "11a1c3b0-2b1a-4b1a-9b1a-0b1a4b1a0b1a")
        UUID companyId,

        @Schema(description = "수정자", example = "admin")
        String updatedBy,

        @Schema(description = "수정일시", example = "2025-11-10T13:00:00")
        LocalDateTime updatedAt
) {
    public static ProductUpdateResponse from(Product product){
        return new ProductUpdateResponse(
                product.getId(),
                product.getName(),
                product.getQuantity(),
                product.getPrice(),
                product.getHubId(),
                product.getCompanyId(),
                product.getUpdatedBy(),
                product.getUpdatedAt()
        );
    }
}