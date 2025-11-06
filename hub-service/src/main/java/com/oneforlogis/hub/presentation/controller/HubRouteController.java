package com.oneforlogis.hub.presentation.controller;

import com.oneforlogis.hub.application.service.HubRouteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "HubRoutes", description = "허브 경로 관련 API")
@RestController
@RequestMapping("/api/v1/hubs/routes")
@RequiredArgsConstructor
public class HubRouteController {

    private final HubRouteService hubRouteService;
}
