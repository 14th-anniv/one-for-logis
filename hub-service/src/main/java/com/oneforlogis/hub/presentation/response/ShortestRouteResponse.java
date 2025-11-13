package com.oneforlogis.hub.presentation.response;

import com.oneforlogis.hub.application.dto.DijkstraResult;
import com.oneforlogis.hub.domain.model.HubRoute;
import com.oneforlogis.hub.domain.model.RouteType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

public record ShortestRouteResponse(
        @Schema(description = "경로 ID (직통 경로만 O)", example = "10")
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

        @Schema(description = "전체 이동 거리 (km 단위)", example = "350.7")
        BigDecimal totalDistance,

        @Schema(description = "전체 소요 시간 (분 단위)", example = "252")
        Integer totalTime,

        @Schema(description = "경로 유형 (DIRECT, RELAY)", example = "DIRECT")
        RouteType routeType,

        @Schema(
                description = "경유 허브 노드 리스트 (방문 순서대로)",
                example = "[{\"id\":\"d0c14c9e-08f7-46c2-a4a6-c79abfa58f56\",\"name\":\"서울 센터\",\"address\":\"서울특별시 송파구 송파대로 55\"}]"
        )
        List<HubSimpleResponse> pathNodes,

        @Schema(
                description = "허브 간 이동 경로 정보 (각 구간 거리/시간 포함)",
                example = "[{\"id\":\"1\",\"fromHubId\":\"d0c14c9e-08f7-46c2-a4a6-c79abfa58f56\",\"toHubId\":\"845bd770-13d0-4337-a736-642186a6409b\",\"distance\":130.2,\"time\":92}]"
        )
        List<RouteEdgeResponse> routeEdges
) {
        public static ShortestRouteResponse fromResult(DijkstraResult result, HubResponse fromHub, HubResponse toHub, List<HubSimpleResponse> pathNodes, List<RouteEdgeResponse> routeEdges) {
                return new ShortestRouteResponse(
                        null,
                        HubSimpleResponse.of(fromHub),
                        HubSimpleResponse.of(toHub),
                        result.distance(),
                        result.time(),
                        RouteType.RELAY,
                        pathNodes,
                        routeEdges
                );
        }

        public static ShortestRouteResponse fromDirect(HubRoute route, HubResponse fromHub, HubResponse toHub) {
            return new ShortestRouteResponse(
                    route.getId(),
                    HubSimpleResponse.of(fromHub),
                    HubSimpleResponse.of(toHub),
                    route.getRouteDistance(),
                    route.getRouteTime(),
                    RouteType.DIRECT,
                    null,
                    null
            );
        }
}
