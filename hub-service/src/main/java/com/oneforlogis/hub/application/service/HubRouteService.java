package com.oneforlogis.hub.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.oneforlogis.common.api.PageResponse;
import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.hub.application.dto.DijkstraResult;
import com.oneforlogis.hub.domain.model.HubRoute;
import com.oneforlogis.hub.domain.model.RouteType;
import com.oneforlogis.hub.domain.repository.HubRouteRepository;
import com.oneforlogis.hub.application.dto.HubEdge;
import com.oneforlogis.hub.infrastructure.cache.HubRouteCacheService;
import com.oneforlogis.hub.presentation.request.HubRouteRequest;
import com.oneforlogis.hub.presentation.response.HubResponse;
import com.oneforlogis.hub.presentation.response.HubRouteResponse;
import com.oneforlogis.hub.presentation.response.ShortestRouteResponse;
import com.oneforlogis.hub.presentation.response.HubSimpleResponse;
import com.oneforlogis.hub.presentation.response.RouteEdgeResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubRouteService {

    private final HubRouteRepository hubRouteRepository;
    private final HubService hubService;
    private final HubRouteCacheService hubRouteCacheService;
    private final DijkstraService dijkstraService;

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

    @Transactional
    public void refreshRouteCache() {
        List<HubRoute> directRoutes = hubRouteRepository.findByDeletedFalseAndRouteType(RouteType.DIRECT);
        hubRouteCacheService.refreshRouteCaches(directRoutes);
    }

    public HubRouteResponse getHubRouteById(Long routeId) {
        HubRoute route = hubRouteRepository.findById(routeId)
                .orElseThrow(() -> new CustomException(ErrorCode.HUB_ROUTE_NOT_FOUND));
        if (route.isDeleted()) throw new CustomException(ErrorCode.HUB_ROUTE_DELETED);

        HubResponse fromHub = hubService.getHubById(route.getFromHubId());
        HubResponse toHub = hubService.getHubById(route.getToHubId());

        return HubRouteResponse.from(route, fromHub, toHub);
    }

    public HubRouteResponse getDirectRoute(UUID fromHubId, UUID toHubId) {
        HubRoute cached = hubRouteCacheService.getDirectRoute(fromHubId, toHubId);
        if (cached != null) {
            HubResponse fromHub = hubService.getHubById(fromHubId);
            HubResponse toHub = hubService.getHubById(toHubId);
            return HubRouteResponse.from(cached, fromHub, toHub);
        }

        HubRoute route = hubRouteRepository.findByFromHubIdAndToHubId(fromHubId, toHubId)
                .orElseThrow(() -> new CustomException(ErrorCode.HUB_ROUTE_NOT_FOUND));
        if (route.getRouteType() != RouteType.DIRECT) throw new CustomException(ErrorCode.HUB_ROUTE_NOT_DIRECT);

        hubRouteCacheService.syncOnCreate(route);

        HubResponse fromHub = hubService.getHubById(fromHubId);
        HubResponse toHub = hubService.getHubById(toHubId);
        return HubRouteResponse.from(route, fromHub, toHub);
    }

    public PageResponse<HubRouteResponse> getAllHubRoutes(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<HubRoute> routes = hubRouteRepository.findByDeletedFalse(pageable);

        List<UUID> hubIds = routes.stream()
                .flatMap(route -> Stream.of(route.getFromHubId(), route.getToHubId()))
                .distinct()
                .toList();

        Map<UUID, HubResponse> hubMap = hubService.getHubsBulk(hubIds);

        Page<HubRouteResponse> responsePage = routes.map(route -> {
            HubResponse fromHub = hubMap.get(route.getFromHubId());
            HubResponse toHub = hubMap.get(route.getToHubId());
            return HubRouteResponse.from(route, fromHub, toHub);
        });

        return PageResponse.fromPage(responsePage);
    }

    @Transactional
    public ShortestRouteResponse getShortestRoute(UUID fromHubId, UUID toHubId) {
        ShortestRouteResponse cached = hubRouteCacheService.getShortestRoute(fromHubId, toHubId);
        if (cached != null) return cached;

        HubRoute direct = hubRouteCacheService.getDirectRoute(fromHubId, toHubId);
        if (direct != null) {
            HubResponse fromHub = hubService.getHubById(direct.getFromHubId());
            HubResponse toHub = hubService.getHubById(direct.getToHubId());
            return ShortestRouteResponse.fromDirect(direct, fromHub, toHub);
        }

        Map<UUID, List<HubEdge>> graph = hubRouteCacheService.getGraph();
        DijkstraResult result = dijkstraService.findShortestPath(graph, fromHubId, toHubId);

        ObjectMapper mapper = new ObjectMapper();
        String pathJson;
        try {
            pathJson = mapper.writeValueAsString(result.pathNodes());
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.JSON_SERIALIZATION_FAILED);
        }

        HubRoute shortestRoute = HubRoute.createRelayRoute(fromHubId, toHubId, result, pathJson);
        hubRouteRepository.save(shortestRoute);

        List<UUID> allHubIds = Stream.concat(
                Stream.of(fromHubId, toHubId),
                result.pathNodes().stream()
        ).distinct().toList();

        Map<UUID, HubResponse> hubMap = hubService.getHubsBulk(allHubIds);

        HubResponse fromHub = hubMap.get(fromHubId);
        HubResponse toHub = hubMap.get(toHubId);

        List<HubSimpleResponse> pathNodes = result.pathNodes().stream()
                .map(hubMap::get)
                .map(HubSimpleResponse::of)
                .toList();

        List<RouteEdgeResponse> routeEdges = result.edges().stream()
                .map(RouteEdgeResponse::from)
                .toList();

        ShortestRouteResponse response = ShortestRouteResponse.from(shortestRoute, fromHub, toHub, pathNodes, routeEdges);

        hubRouteCacheService.saveShortestRouteCache(response);

        return response;
    }
}
