package com.oneforlogis.hub.presentation.controller;

import com.oneforlogis.common.api.ApiResponse;
import com.oneforlogis.common.security.UserPrincipal;
import com.oneforlogis.hub.application.service.HubRouteService;
import com.oneforlogis.hub.presentation.request.HubRouteRequest;
import com.oneforlogis.hub.presentation.response.HubRouteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "HubRoutes", description = "허브 경로 관련 API")
@RestController
@RequestMapping("/api/v1/hubs/routes")
@RequiredArgsConstructor
public class HubRouteController {

    private final HubRouteService hubRouteService;

    @Operation(summary = "신규 허브 경로 생성", description = "새로운 물류 허브 경로를 등록합니다. 'MASTER' 권한이 필요합니다.")
    @PreAuthorize("hasRole('MASTER')")
    @PostMapping
    public ApiResponse<HubRouteResponse> createHubRoute(@RequestBody HubRouteRequest request) {
        return ApiResponse.created(hubRouteService.createHubRoute(request));
    }

    @Operation(summary = "허브 경로 수정", description = "허브 경로 정보를 수정합니다. 'MASTER' 권한이 필요합니다.")
    @PreAuthorize("hasRole('MASTER')")
    @PutMapping("/{routeId}")
    public ApiResponse<HubRouteResponse> updateHubRoute(@PathVariable Long routeId, @RequestBody HubRouteRequest request) {
        return ApiResponse.success(hubRouteService.updateHubRoute(routeId, request));
    }

    @Operation(summary = "허브 경로 삭제", description = "허브 경로를 논리적으로 삭제합니다. 'MASTER' 권한이 필요합니다.")
    @PreAuthorize("hasRole('MASTER')")
    @DeleteMapping("/{routeId}")
    public ApiResponse<Void> deleteHubRoute(@AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long routeId) {
        hubRouteService.deleteHubRoute(userPrincipal.username(), routeId);
        return ApiResponse.success();
    }
}
