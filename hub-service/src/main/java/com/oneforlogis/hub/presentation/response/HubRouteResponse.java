package com.oneforlogis.hub.presentation.response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneforlogis.hub.domain.model.HubRoute;
import com.oneforlogis.hub.domain.model.RouteType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "허브 경로 응답 DTO")
public record HubRouteResponse(
        @Schema(description = "경로 ID", example = "1")
        Long id,

        @Schema(
                description = "출발 허브",
                example = "{\"id\":\"d0c14c9e-08f7-46c2-a4a6-c79abfa58f56\",\"name\":\"서울 센터\",\"address\":\"서울특별시 송파구 송파대로 55\"}"
        )
        HubSimpleResponse fromHub,

        @Schema(
                description = "도착 허브",
                example = "{\"id\":\"845bd770-13d0-4337-a736-642186a6409b\",\"name\":\"대전광역시 센터\",\"address\":\"대전 서구 둔산로 100\"}"
        )
        HubSimpleResponse toHub,

        @Schema(description = "허브 간 거리 (km 단위)", example = "35.27")
        BigDecimal routeDistance,
        @Schema(description = "예상 소요 시간 (분 단위)", example = "42")
        Integer routeTime,

        @Schema(description = "경로 유형 (DIRECT, RELAY)", example = "DIRECT")
        RouteType routeType,

        @Schema(description = "경유 허브 목록 (출발지~도착지 순서)", example = "[\"d0c14c9e-08f7-46c2-a4a6-c79abfa58f56\", \"845bd770-13d0-4337-a736-642186a6409b\"]")
        List<UUID> pathNodes,

        @Schema(description = "생성자", example = "user1")
        String createdBy,
        @Schema(description = "생성일시", example = "2025-11-05T15:00:00")
        String createdAt,

        @Schema(description = "수정자", example = "user2")
        String updatedBy,
        @Schema(description = "수정일시", example = "2025-12-15T20:00:00")
        String updatedAt
) {
    public static HubRouteResponse from(HubRoute route, HubResponse fromHub, HubResponse toHub) {
        List<UUID> pathList = null;
        try {
            String raw = route.getPathNodes();
            if (raw != null && !raw.isBlank() && !raw.equals("[]")) {
                ObjectMapper mapper = new ObjectMapper();
                pathList = mapper.readValue(raw, new TypeReference<List<UUID>>() {});
            }
        } catch (Exception e) {
            pathList = List.of();
        }

        return new HubRouteResponse(
                route.getId(),
                HubSimpleResponse.of(fromHub),
                HubSimpleResponse.of(toHub),
                route.getRouteDistance(),
                route.getRouteTime(),
                route.getRouteType(),
                pathList,
                route.getCreatedBy(),
                route.getCreatedAt().toString(),
                route.getUpdatedBy(),
                route.getUpdatedAt().toString()
        );
    }
}