package com.oneforlogis.hub.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.hub.domain.model.HubRoute;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HubRouteCacheService {

    private static final String GRAPH_KEY_PREFIX = "hub:graph:";
    private static final String DIRECT_ROUTE_KEY_FORMAT = "hub:route:from:%s:to:%s";
    private static final String RELAY_ROUTE_KEY_ALL = "hub:path:*";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void syncOnCreate(HubRoute route) {
        invalidateRelayCacheOnly();
        updateGraphCache(route);
        updateDirectRouteCache(route);
    }

    public void syncOnUpdate(HubRoute route) {
        invalidateRelayCacheOnly();
        updateGraphCache(route);
        updateDirectRouteCache(route);
    }

    public void syncOnDelete(HubRoute route) {
        invalidateRelayCacheOnly();
        removeGraphCache(route);
        removeDirectRouteCache(route);
    }

    public void invalidateRelayCacheOnly() {
        Set<String> keys = redisTemplate.keys(RELAY_ROUTE_KEY_ALL);
        if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);
    }

    private void updateGraphCache(HubRoute route) {
        String key = GRAPH_KEY_PREFIX + route.getFromHubId();
        Map<String, Object> edgeData = new HashMap<>();
        edgeData.put("routeDistance", route.getRouteDistance());
        edgeData.put("routeTime", route.getRouteTime());

        try {
            String jsonValue = objectMapper.writeValueAsString(edgeData);
            redisTemplate.opsForHash().put(key, route.getToHubId().toString(), jsonValue);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.REDIS_SERIALIZATION_FAILED);
        }
    }

    private void removeGraphCache(HubRoute route) {
        String key = GRAPH_KEY_PREFIX + route.getFromHubId();
        redisTemplate.opsForHash().delete(key, route.getToHubId().toString());
    }

    private void updateDirectRouteCache(HubRoute route) {
        String key = String.format(DIRECT_ROUTE_KEY_FORMAT, route.getFromHubId(), route.getToHubId());
        try {
            String json = objectMapper.writeValueAsString(route);
            redisTemplate.opsForValue().set(key, json, Duration.ofDays(7));
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.REDIS_SERIALIZATION_FAILED);
        }
    }

    private void removeDirectRouteCache(HubRoute route) {
        String key = String.format(DIRECT_ROUTE_KEY_FORMAT, route.getFromHubId(), route.getToHubId());
        redisTemplate.delete(key);
    }
}