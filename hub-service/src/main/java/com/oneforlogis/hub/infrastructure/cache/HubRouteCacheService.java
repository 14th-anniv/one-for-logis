package com.oneforlogis.hub.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.hub.application.dto.HubEdge;
import com.oneforlogis.hub.domain.model.HubRoute;
import com.oneforlogis.hub.presentation.response.ShortestRouteResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HubRouteCacheService {

    private static final String GRAPH_KEY = "hub:graph:";
    private static final String DIRECT_ROUTE_KEY = "hub:route:";
    private static final String RELAY_ROUTE_KEY = "hub:path:";
    private static final String KEY_FORMAT = "from:%s:to:%s";

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
        Set<String> keys = redisTemplate.keys(RELAY_ROUTE_KEY + "*");
        if (keys != null && !keys.isEmpty())
            redisTemplate.delete(keys);
    }

    public void invalidateDirectAndGraphCache() {
        Set<String> routeKeys = redisTemplate.keys(DIRECT_ROUTE_KEY + "*");
        Set<String> graphKeys = redisTemplate.keys(GRAPH_KEY + "*");
        if (routeKeys != null && !routeKeys.isEmpty()) redisTemplate.delete(routeKeys);
        if (graphKeys != null && !graphKeys.isEmpty()) redisTemplate.delete(graphKeys);
    }

    private void updateGraphCache(HubRoute route) {
        String key = GRAPH_KEY + route.getFromHubId();
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
        String key = GRAPH_KEY + route.getFromHubId();
        redisTemplate.opsForHash().delete(key, route.getToHubId().toString());
    }

    private void updateDirectRouteCache(HubRoute route) {
        String key = String.format(DIRECT_ROUTE_KEY + KEY_FORMAT, route.getFromHubId(), route.getToHubId());
        try {
            String json = objectMapper.writeValueAsString(route);
            redisTemplate.opsForValue().set(key, json);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.REDIS_SERIALIZATION_FAILED);
        }
    }

    private void removeDirectRouteCache(HubRoute route) {
        String key = String.format(DIRECT_ROUTE_KEY + KEY_FORMAT, route.getFromHubId(), route.getToHubId());
        redisTemplate.delete(key);
    }

    public HubRoute getDirectRoute(UUID fromHubId, UUID toHubId) {
        String key = String.format(DIRECT_ROUTE_KEY + KEY_FORMAT, fromHubId, toHubId);
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) return null;

        try {
            return objectMapper.readValue(json, HubRoute.class);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.REDIS_DESERIALIZATION_FAILED);
        }
    }

    public ShortestRouteResponse getShortestRoute(UUID fromHubId, UUID toHubId) {
        String key = String.format(RELAY_ROUTE_KEY + KEY_FORMAT, fromHubId, toHubId);
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) return null;

        try {
            return objectMapper.readValue(json, ShortestRouteResponse.class);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.REDIS_DESERIALIZATION_FAILED);
        }
    }

    public Map<UUID, List<HubEdge>> getGraph() {
        Map<UUID, List<HubEdge>> graph = new HashMap<>();
        Set<String> keys = redisTemplate.keys(GRAPH_KEY + "*");
        if (keys == null || keys.isEmpty()) return graph;

        for (String key : keys) {
            String fromHubId = key.replace(GRAPH_KEY, "");
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

            List<HubEdge> edges = entries.entrySet().stream()
                .map(entry -> {
                    UUID toHubId = UUID.fromString(entry.getKey().toString());
                    try {
                        HubEdge edge = objectMapper.readValue(entry.getValue().toString(), HubEdge.class);
                        return new HubEdge(UUID.fromString(fromHubId), toHubId, edge.routeDistance(), edge.routeTime());
                    } catch (JsonProcessingException e) {
                        throw new CustomException(ErrorCode.REDIS_DESERIALIZATION_FAILED);
                    }
                })
                .toList();

            graph.put(UUID.fromString(fromHubId), edges);
        }

        return graph;
    }

    public void saveShortestRouteCache(ShortestRouteResponse response) {
        String key = String.format(RELAY_ROUTE_KEY + KEY_FORMAT, response.fromHub().id(), response.toHub().id());
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, Duration.ofDays(7));
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.REDIS_SERIALIZATION_FAILED);
        }
    }

    public void refreshRouteCaches(List<HubRoute> directRoutes) {
        invalidateRelayCacheOnly();
        invalidateDirectAndGraphCache();

        Map<String, String> directBatch = buildDirectRouteBatch(directRoutes);
        Map<String, Map<String, String>> graphBatch = buildGraphBatch(directRoutes);

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            var serializer = redisTemplate.getStringSerializer();
            directBatch.forEach((key, value) ->
                    connection.stringCommands().set(serializer.serialize(key), serializer.serialize(value))
            );
            graphBatch.forEach((key, edges) ->
                    edges.forEach((field, value) ->
                            connection.hashCommands().hSet(serializer.serialize(key), serializer.serialize(field), serializer.serialize(value))
                    )
            );
            return null;
        });
    }

    private Map<String, String> buildDirectRouteBatch(List<HubRoute> directRoutes) {
        Map<String, String> batch = new HashMap<>();
        for (HubRoute route : directRoutes) {
            try {
                String key = String.format(DIRECT_ROUTE_KEY + KEY_FORMAT, route.getFromHubId(), route.getToHubId());
                batch.put(key, objectMapper.writeValueAsString(route));
            } catch (JsonProcessingException e) {
                throw new CustomException(ErrorCode.REDIS_SERIALIZATION_FAILED);
            }
        }
        return batch;
    }

    private Map<String, Map<String, String>> buildGraphBatch(List<HubRoute> directRoutes) {
        Map<String, Map<String, String>> batch = new HashMap<>();
        for (HubRoute route : directRoutes) {
            String graphKey = GRAPH_KEY + route.getFromHubId();
            batch.computeIfAbsent(graphKey, k -> new HashMap<>());
            try {
                batch.get(graphKey).put(
                        route.getToHubId().toString(),
                        objectMapper.writeValueAsString(Map.of(
                                "routeDistance", route.getRouteDistance(),
                                "routeTime", route.getRouteTime()
                        ))
                );
            } catch (JsonProcessingException e) {
                throw new CustomException(ErrorCode.REDIS_SERIALIZATION_FAILED);
            }
        }
        return batch;
    }
}