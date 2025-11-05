package com.oneforlogis.hub.presentation.response;

import com.oneforlogis.hub.domain.model.Hub;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "허브 생성 응답 DTO")
public record HubUpdateResponse(
        @Schema(description = "허브 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "허브명", example = "서울허브")
        String name,

        @Schema(description = "허브 주소", example = "서울특별시 강남구 테헤란로 123")
        String address,

        @Schema(description = "위도", example = "37.5666500")
        BigDecimal lat,

        @Schema(description = "경도", example = "126.9780000")
        BigDecimal lon,

        @Schema(description = "생성자", example = "user1")
        String createdBy,

        @Schema(description = "생성일시", example = "2025-11-05T15:00:00")
        LocalDateTime createdAt,

        @Schema(description = "수정자", example = "user1")
        String updatedBy,

        @Schema(description = "수정일시", example = "2025-12-15T20:00:00")
        LocalDateTime updatedAt
){
    public static HubUpdateResponse from(Hub hub) {
        return new HubUpdateResponse(
                hub.getId(),
                hub.getName(),
                hub.getAddress(),
                hub.getLat(),
                hub.getLon(),
                hub.getCreatedBy(),
                hub.getCreatedAt(),
                hub.getUpdatedBy(),
                hub.getUpdatedAt()
        );
    }
}
