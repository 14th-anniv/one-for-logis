package com.oneforlogis.hub.presentation.controller.internal;

import com.oneforlogis.common.api.ApiResponse;
import com.oneforlogis.common.api.PageResponse;
import com.oneforlogis.hub.application.service.HubRouteService;
import com.oneforlogis.hub.presentation.response.HubRouteResponse;
import com.oneforlogis.hub.presentation.response.ShortestRouteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Internal HubRoutes", description = "내부용 허브 경로 조회 API")
@RestController
@RequestMapping("/api/v1/internal/hubs/routes")
@RequiredArgsConstructor
public class InternalHubRouteController {

    private final HubRouteService hubRouteService;

    @Operation(summary = "허브 경로 id로 단일 조회", description = "routeId로 허브 경로를 조회합니다. (캐시 미사용)")
    @GetMapping("/{routeId}")
    public ApiResponse<HubRouteResponse> getHubRoute(@PathVariable Long routeId) {
        return ApiResponse.success(hubRouteService.getHubRouteById(routeId));
    }

    @Operation(summary = "출발/도착 기준 직통 경로 조회", description = "fromHubId와 toHubId로 직통 연결된 경로를 조회합니다. 캐시를 우선 조회하며, 없으면 DB에서 조회합니다.")
    @GetMapping("/direct")
    public ApiResponse<HubRouteResponse> getDirectRoute(@RequestParam UUID from,
            @RequestParam UUID to) {
        return ApiResponse.success(hubRouteService.getDirectRoute(from, to));
    }

    @Operation(summary = "허브 경로 전체 조회", description = "모든 허브 경로를 페이지 형태로 조회합니다.")
    @GetMapping("/all")
    public ApiResponse<PageResponse<HubRouteResponse>> getAllHubRoutes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(hubRouteService.getAllHubRoutes(page, size));
    }

    @Operation(summary = "허브 간 최단 경로 조회", description = "출발지와 도착지를 기준으로 최단 경로를 계산하거나 캐시된 결과를 반환합니다.")
    @GetMapping("/shortest")
    public ApiResponse<ShortestRouteResponse> getShortestRoute(@RequestParam UUID fromHubId,
            @RequestParam UUID toHubId) {
        return ApiResponse.success(hubRouteService.getShortestRoute(fromHubId, toHubId));
    }
}