package com.oneforlogis.hub.presentation.controller;

import com.oneforlogis.common.api.ApiResponse;
import com.oneforlogis.common.api.PageResponse;
import com.oneforlogis.common.security.UserPrincipal;
import com.oneforlogis.hub.application.service.HubService;
import com.oneforlogis.hub.presentation.request.HubRequest;
import com.oneforlogis.hub.presentation.request.HubRequest;
import com.oneforlogis.hub.presentation.response.HubResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@Tag(name = "Hubs", description = "허브 관리 API")
@RestController
@RequestMapping("/api/v1/hubs")
@RequiredArgsConstructor
public class HubController {

    private final HubService hubService;

    @Operation(summary = "신규 허브 생성", description = "새로운 물류 허브를 등록합니다. 'MASTER' 권한이 필요합니다.")
    @PreAuthorize("hasRole('MASTER')")
    @PostMapping
    public ApiResponse<HubResponse> createHub(@RequestBody HubRequest request) {
        return ApiResponse.created(hubService.createHub(request));
    }

    @Operation(summary = "허브 수정", description = "허브 정보를 수정합니다. 'MASTER' 권한이 필요합니다.")
    @PreAuthorize("hasRole('MASTER')")
    @PutMapping("/{hubId}")
    public ApiResponse<HubResponse> updateHub(@PathVariable UUID hubId,
            @RequestBody HubRequest request) {
        return ApiResponse.success(hubService.updateHub(hubId, request));
    }

    @Operation(summary = "허브 삭제", description = "허브를 논리적으로 삭제합니다. 'MASTER' 권한이 필요합니다.")
    @PreAuthorize("hasRole('MASTER')")
    @DeleteMapping("/{hubId}")
    public ApiResponse<Void> deleteHub(@AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID hubId) {
        hubService.deleteHub(userPrincipal.username(), hubId);
        return ApiResponse.success();
    }

    @Operation(summary = "허브 캐시 갱신", description = "허브 데이터 캐시를 갱신합니다. 'MASTER' 권한이 필요합니다.")
    @PreAuthorize("hasRole('MASTER')")
    @PostMapping("/cache/refresh")
    public ApiResponse<Void> refreshHubCache() {
        hubService.refreshHubCache();
        return ApiResponse.success("허브 캐시가 갱신되었습니다.");
    }

    @Operation(summary = "허브 id로 단일 조회", description = "허브 ID로 단일 허브 정보를 조회합니다.")
    @GetMapping("/{hubId}")
    public ApiResponse<HubResponse> getHubById(@PathVariable UUID hubId) {
        return ApiResponse.success(hubService.getHubById(hubId));
    }

    @Operation(summary = "허브 이름으로 조회", description = "허브 이름으로 단일 허브 정보를 조회합니다.")
    @GetMapping("/name/{hubName}")
    public ApiResponse<HubResponse> getHubByName(@PathVariable String hubName) {
        return ApiResponse.success(hubService.getHubByName(hubName));
    }

    @Operation(summary = "허브 전체 조회", description = "모든 허브를 페이지 형태로 조회합니다. (캐시 데이터 x)")
    @GetMapping
    public ApiResponse<PageResponse<HubResponse>> getAllHubs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(hubService.getAllHubs(page, size));
    }
}