package com.oneforlogis.company.infrastructure.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * FeignClient 응답 바인딩 DTO (Json -> hub 데이터 -> 매핑. record) : 실제 데이터 DTO
 */
public record HubResponse(
        UUID id,
        String name,
        String address,
        BigDecimal lat,
        BigDecimal lon
) {}