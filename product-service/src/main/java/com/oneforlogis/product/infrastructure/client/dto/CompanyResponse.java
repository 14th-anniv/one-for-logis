package com.oneforlogis.product.infrastructure.client.dto;

import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String name,
        UUID hubId,
        String address
) {}