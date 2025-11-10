package com.oneforlogis.product.application.dto.response;

import com.oneforlogis.product.domain.model.Product;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponse(

        UUID id,
        String name,
        Integer quantity,
        BigDecimal price,
        UUID hubId,
        UUID companyId,
        String createdBy,
        LocalDateTime createdAt
) {
    public static ProductResponse from(Product product){
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getQuantity(),
                product.getPrice(),
                product.getHubId(),
                product.getCompanyId(),
                product.getCreatedBy(),
                product.getCreatedAt()
        );
    }
}
