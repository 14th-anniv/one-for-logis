package com.oneforlogis.hub.presentation.request;

import com.oneforlogis.hub.domain.model.RouteType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

public record HubRouteRequest(

    @Schema(description = "출발 허브 ID", example = "3f8b52d1-22c5-4a32-a7ff-59cf3f0567ad")
    UUID fromHubId,

    @Schema(description = "도착 허브 ID", example = "b01a97f2-81d4-4983-9727-cfd6116edc62")
    UUID toHubId,

    @Schema(description = "경로 거리 (km 단위)", example = "12.51")
    BigDecimal routeDistance,

    @Schema(description = "예상 소요 시간 (분 단위)", example = "35")
    Integer routeTime,

    @Schema(description = "경로 유형, DIRECT=직통 연결, RELAY=중계 허브를 거치는 유형", example = "DIRECT")
    RouteType routeType
) {}
