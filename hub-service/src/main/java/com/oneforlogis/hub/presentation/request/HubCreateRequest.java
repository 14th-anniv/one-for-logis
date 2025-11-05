package com.oneforlogis.hub.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record HubCreateRequest(
        @Schema(description = "허브명 (예: 서울허브, 대전허브 등)", example = "서울허브")
        String name,

        @Schema(description = "허브 주소(도로명) - 지도/배송 라벨 표기용", example = "서울특별시 강남구 테헤란로 123")
        String address,

        @Schema(description = "위도 (예: 37.5666500) - 지도 매핑, 거리 계산 기준값", example = "37.5666500")
        BigDecimal lat,

        @Schema(description = "경도 (예: 126.9780000) - 지도 매핑, 거리 계산 기준값", example = "126.9780000")
        BigDecimal lon
) {}
