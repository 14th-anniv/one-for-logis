package com.oneforlogis.hub.infrastructure.persistence;

import com.oneforlogis.hub.domain.model.HubRoute;
import com.oneforlogis.hub.domain.model.RouteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HubRouteJpaRepository extends JpaRepository<HubRoute, Long> {
    void deleteAllByRouteType(RouteType routeType);
}
