package com.oneforlogis.hub.presentation.controller;

import com.oneforlogis.common.api.ApiResponse;
import com.oneforlogis.hub.domain.service.HubService;
import com.oneforlogis.hub.presentation.request.HubCreateRequest;
import com.oneforlogis.hub.presentation.response.HubCreateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public ApiResponse<HubCreateResponse> createHub(@RequestBody HubCreateRequest request) {
        return ApiResponse.created(hubService.createHub(request));
    }
}
