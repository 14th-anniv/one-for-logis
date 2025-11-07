package com.oneforlogis.hub.application.service;

import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.hub.domain.model.HubRoute;
import com.oneforlogis.hub.domain.model.RouteType;
import com.oneforlogis.hub.domain.repository.HubRouteRepository;
import com.oneforlogis.hub.infrastructure.cache.HubRouteCacheService;
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
    private final HubRouteCacheService hubRouteCacheService;

    @Transactional
    public HubRouteResponse createHubRoute(HubRouteRequest request) {
        HubResponse fromHub = hubService.getHubById(request.fromHubId());
        HubResponse toHub = hubService.getHubById(request.toHubId());

        HubRoute hubRoute = HubRoute.create(request);
        hubRouteRepository.save(hubRoute);
        hubRouteRepository.deleteAllByRouteType(RouteType.RELAY);
        hubRouteCacheService.syncOnCreate(hubRoute);

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
        hubRouteRepository.deleteAllByRouteType(RouteType.RELAY);
        hubRouteCacheService.syncOnUpdate(hubRoute);

        return HubRouteResponse.from(hubRoute, fromHub, toHub);
    }

    @Transactional
    public void deleteHubRoute(String userName, Long routeId) {
        HubRoute hubRoute = hubRouteRepository.findById(routeId)
                .orElseThrow(() -> new CustomException(ErrorCode.HUB_ROUTE_NOT_FOUND));
        if (hubRoute.isDeleted()) throw new CustomException(ErrorCode.HUB_ROUTE_DELETED);

        hubRoute.markAsDeleted(userName);
        hubRouteRepository.deleteAllByRouteType(RouteType.RELAY);
        hubRouteCacheService.syncOnDelete(hubRoute);
    }
}
