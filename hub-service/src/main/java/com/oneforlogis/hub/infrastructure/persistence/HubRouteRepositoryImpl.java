package com.oneforlogis.hub.infrastructure.persistence;

import com.oneforlogis.hub.domain.model.HubRoute;
import com.oneforlogis.hub.domain.model.RouteType;
import com.oneforlogis.hub.domain.repository.HubRouteRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class HubRouteRepositoryImpl implements HubRouteRepository {

    private final HubRouteJpaRepository jpaRepository;

    @Override
    public void save(HubRoute hubRoute) {
        jpaRepository.save(hubRoute);
    }

    @Override
    public void flush() {
        jpaRepository.flush();
    }

    @Override
    public Optional<HubRoute> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public void deleteAllByRouteType(RouteType routeType) {
        jpaRepository.deleteAllByRouteType(routeType);
    }
}
