package com.oneforlogis.hub.application.service;

import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.hub.domain.model.HubRoute;
import com.oneforlogis.hub.domain.repository.HubRouteRepository;
import com.oneforlogis.hub.presentation.request.HubRouteRequest;
import com.oneforlogis.hub.presentation.response.HubResponse;
import com.oneforlogis.hub.presentation.response.HubRouteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubRouteService {

    private final HubRouteRepository hubRouteRepository;
    private final HubService hubService;

    @Transactional
    public HubRouteResponse createHubRoute(HubRouteRequest request) {
        HubResponse fromHub = hubService.getHubById(request.fromHubId());
        HubResponse toHub = hubService.getHubById(request.toHubId());

        HubRoute hubRoute = HubRoute.create(request);
        hubRouteRepository.save(hubRoute);

        return HubRouteResponse.from(hubRoute, fromHub, toHub);
    }

    @Transactional
    public HubRouteResponse updateHubRoute(Long routeId, HubRouteRequest request) {
        HubRoute hubRoute = hubRouteRepository.findById(routeId)
                .orElseThrow(() -> new CustomException(ErrorCode.HUB_ROUTE_NOT_FOUND));
        if (hubRoute.isDeleted()) throw new CustomException(ErrorCode.HUB_ROUTE_DELETED);

        HubResponse fromHub = hubService.getHubById(request.fromHubId());
        HubResponse toHub = hubService.getHubById(request.toHubId());

        hubRoute.update(request);
        hubRouteRepository.flush();

        return HubRouteResponse.from(hubRoute, fromHub, toHub);
    }
}
