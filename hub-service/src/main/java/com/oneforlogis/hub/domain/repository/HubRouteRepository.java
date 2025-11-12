package com.oneforlogis.hub.domain.repository;

import com.oneforlogis.hub.domain.model.HubRoute;
import com.oneforlogis.hub.domain.model.RouteType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HubRouteRepository {
    void save(HubRoute hubRoute);
    void flush();
    Optional<HubRoute> findById(Long id);
    void deleteAllByRouteType(RouteType routeType);
    Optional<HubRoute> findByFromHubIdAndToHubId(UUID fromHubId, UUID toHubId);
    Page<HubRoute> findByDeletedFalse(Pageable pageable);
    List<HubRoute> findByDeletedFalseAndRouteType(RouteType routeType);
}
