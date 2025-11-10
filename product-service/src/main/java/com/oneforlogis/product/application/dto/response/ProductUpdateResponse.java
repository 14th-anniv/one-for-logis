package com.oneforlogis.product.application.dto.response;

import com.oneforlogis.product.domain.model.Product;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductUpdateResponse(

        UUID id,
        String name,
        Integer quantity,
        BigDecimal price,
        UUID hubId,
        UUID companyId,
        String updatedBy,
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
