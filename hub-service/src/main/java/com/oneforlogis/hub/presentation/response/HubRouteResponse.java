package com.oneforlogis.hub.presentation.response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneforlogis.hub.domain.model.HubRoute;
import com.oneforlogis.hub.domain.model.RouteType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record HubRouteResponse(
        @Schema(description = "경로 ID", example = "1")
        Long id,

        @Schema(description = "출발 허브 ID", example = "7a8c6e44-81d5-a3b7-3b76-c85a807f0388")
        UUID fromHubId,
        @Schema(description = "출발 허브 이름", example = "서울 센터")
        String fromHubName,
        @Schema(description = "출발 허브 주소", example = "서울특별시 송파구 송파대로 55")
        String fromHubAddress,

        @Schema(description = "도착 허브 ID", example = "3f2bce91-69b7-4d82-b0d0-88a144d97a11")
        UUID toHubId,
        @Schema(description = "도착 허브 이름", example = "경기 남부 센터")
        String toHubName,
        @Schema(description = "도착 허브 주소", example = "경기도 이천시 덕평로 257-21")
        String toHubAddress,

        @Schema(description = "허브 간 거리 (km 단위)", example = "35.27")
        BigDecimal routeDistance,
        @Schema(description = "예상 소요 시간 (분 단위)", example = "42")
        Integer routeTime,

        @Schema(description = "경로 유형 (DIRECT, RELAY)", example = "DIRECT")
        RouteType routeType,

        @Schema(description = "경유 허브 목록 (출발지~도착지 순서)", example = "[\"서울\", \"경기남부\", \"대전\"]")
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
                fromHub.id(),
                fromHub.name(),
                fromHub.address(),
                toHub.id(),
                toHub.name(),
                toHub.address(),
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