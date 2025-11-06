package com.oneforlogis.hub.application.service;

import com.oneforlogis.hub.domain.model.HubRoute;
import com.oneforlogis.hub.domain.repository.HubRouteRepository;
import com.oneforlogis.hub.presentation.request.HubRouteCreateRequest;
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
    public HubRouteResponse createHubRoute(HubRouteCreateRequest request) {
        HubResponse fromHub = hubService.getHubById(request.fromHubId());
        HubResponse toHub = hubService.getHubById(request.toHubId());

        HubRoute hubRoute = HubRoute.create(request);
        hubRouteRepository.save(hubRoute);

        return HubRouteResponse.from(hubRoute, fromHub, toHub);
    }
}
