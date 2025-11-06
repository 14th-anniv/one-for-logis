package com.oneforlogis.hub.presentation.controller;

import com.oneforlogis.common.api.ApiResponse;
import com.oneforlogis.common.security.UserPrincipal;
import com.oneforlogis.hub.domain.service.HubService;
import com.oneforlogis.hub.presentation.request.HubCreateRequest;
import com.oneforlogis.hub.presentation.request.HubUpdateRequest;
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
    public ApiResponse<HubResponse> createHub(@RequestBody HubCreateRequest request) {
        return ApiResponse.created(hubService.createHub(request));
    }

    @Operation(summary = "허브 수정", description = "허브 정보를 수정합니다. 'MASTER' 권한이 필요합니다.")
    @PreAuthorize("hasRole('MASTER')")
    @PutMapping("/{hubId}")
    public ApiResponse<HubResponse> updateHub(@PathVariable UUID hubId, @RequestBody HubUpdateRequest request) {
        return ApiResponse.success(hubService.updateHub(hubId, request));
    }

    @Operation(summary = "허브 삭제", description = "허브를 논리적으로 삭제합니다. 'MASTER' 권한이 필요합니다.")
    @PreAuthorize("hasRole('MASTER')")
    @DeleteMapping("/{hubId}")
    public ApiResponse<Void> deleteHub(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable UUID hubId) {
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
}
