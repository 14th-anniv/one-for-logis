package com.oneforlogis.product.infrastructure.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record HubResponse(
        UUID id,
        String name,
        String address,
        BigDecimal lat,
        BigDecimal lon
) {}