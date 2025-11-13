package com.oneforlogis.hub.presentation.controller.internal;

import com.oneforlogis.common.api.ApiResponse;
import com.oneforlogis.common.api.PageResponse;
import com.oneforlogis.hub.application.service.HubService;
import com.oneforlogis.hub.presentation.response.HubResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Internal Hubs", description = "내부용 허브 조회 API")
@RestController
@RequestMapping("/api/v1/internal/hubs")
@RequiredArgsConstructor
public class InternalHubController {

    private final HubService hubService;

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
