package com.oneforlogis.product.presentation.controller.internal.dto;

import com.oneforlogis.product.domain.model.Product;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductDto(
        UUID id,
        String name,
        Integer quantity,
        BigDecimal price,
        UUID hubId,
        UUID companyId
) {
    public static ProductDto fromEntity(Product product) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getQuantity(),
                product.getPrice(),
                product.getHubId(),
                product.getCompanyId());
    }
}
