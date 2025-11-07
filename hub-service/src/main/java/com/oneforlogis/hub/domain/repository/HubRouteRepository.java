package com.oneforlogis.hub.domain.repository;

import com.oneforlogis.hub.domain.model.HubRoute;
import com.oneforlogis.hub.domain.model.RouteType;
import java.util.Optional;

public interface HubRouteRepository {
    void save(HubRoute hubRoute);
    void flush();
    Optional<HubRoute> findById(Long id);
    void deleteAllByRouteType(RouteType routeType);
}
