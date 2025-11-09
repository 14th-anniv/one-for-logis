package com.oneforlogis.hub.presentation.response;

import com.oneforlogis.hub.application.dto.HubEdge;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "허브 중간 경로 DTO")
public record RouteEdgeResponse(
        @Schema(description = "출발 허브 ID", example = "5fbaf44b-ddb7-4b35-8006-c04b6b730701")
        String fromHubId,
        @Schema(description = "도착 허브 ID", example = "845bd770-13d0-4337-a736-642186a6409b")
        String toHubId,
        @Schema(description = "허브 간 거리 (km 단위)", example = "35.27")
        BigDecimal distance,
        @Schema(description = "예상 소요 시간 (분 단위)", example = "42")
        Integer time
) {
        public static RouteEdgeResponse from(HubEdge edge) {
                return new RouteEdgeResponse(
                        edge.fromHubId().toString(),
                        edge.toHubId().toString(),
                        edge.routeDistance(),
                        edge.routeTime()
                );
        }
}