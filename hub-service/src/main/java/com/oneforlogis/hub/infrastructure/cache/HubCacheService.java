package com.oneforlogis.hub.infrastructure.cache;

import com.oneforlogis.hub.domain.model.Hub;
import com.oneforlogis.hub.presentation.response.HubResponse;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class HubCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String HUB_ID_KEY_PREFIX = "hub:id:";
    private static final String HUB_NAME_KEY_PREFIX = "hub:name:";
    private static final Duration HUB_CACHE_TTL = Duration.ofDays(7);

    public void saveHubCache(HubResponse hubResponse) {
        String idKey = HUB_ID_KEY_PREFIX + hubResponse.id();
        String encodedName = URLEncoder.encode(hubResponse.name(), StandardCharsets.UTF_8);
        String nameKey = HUB_NAME_KEY_PREFIX + encodedName;

        redisTemplate.opsForValue().set(idKey, hubResponse, HUB_CACHE_TTL);
        redisTemplate.opsForValue().set(nameKey, hubResponse.id().toString(), HUB_CACHE_TTL);
    }

    public void deleteHubCache(UUID hubId) {
        HubResponse hubResponse = getHubCache(hubId);
        if (hubResponse != null) {
            String encodedName = URLEncoder.encode(hubResponse.name(), StandardCharsets.UTF_8);
            redisTemplate.delete(HUB_NAME_KEY_PREFIX + encodedName);
        }
        redisTemplate.delete(HUB_ID_KEY_PREFIX + hubId);
    }

    public void refreshHubListCache(List<Hub> hubs) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Hub hub : hubs) {
                String idKey = HUB_ID_KEY_PREFIX + hub.getId();
                String nameKey = HUB_NAME_KEY_PREFIX + URLEncoder.encode(hub.getName(), StandardCharsets.UTF_8);
                byte[] idKeyBytes = redisTemplate.getStringSerializer().serialize(idKey);
                byte[] nameKeyBytes = redisTemplate.getStringSerializer().serialize(nameKey);
                byte[] valueBytes = ((RedisSerializer<Object>) redisTemplate.getValueSerializer())
                        .serialize(HubResponse.from(hub));

                connection.stringCommands().setEx(idKeyBytes, HUB_CACHE_TTL.getSeconds(), valueBytes);
                connection.stringCommands().setEx(nameKeyBytes, HUB_CACHE_TTL.getSeconds(), valueBytes);
            }
            return null;
        });
    }

    public HubResponse getHubCache(UUID hubId) {
        return (HubResponse) redisTemplate.opsForValue().get(HUB_ID_KEY_PREFIX + hubId);
    }

    public HubResponse getHubCacheByName(String name) {
        String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);
        String hubId = (String) redisTemplate.opsForValue().get(HUB_NAME_KEY_PREFIX + encodedName);
        if (hubId == null) return null;
        return getHubCache(UUID.fromString(hubId));
    }

    public Map<UUID, HubResponse> getHubsBulk(List<UUID> hubIds) {
        List<String> keys = hubIds.stream()
                .map(id -> HUB_ID_KEY_PREFIX + id)
                .toList();

        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        Map<UUID, HubResponse> result = new HashMap<>();

        for (int i = 0; i < hubIds.size(); i++) {
            Object value = values != null ? values.get(i) : null;
            if (value instanceof HubResponse hubResponse) {
                result.put(hubIds.get(i), hubResponse);
            }
        }
        return result;
    }
}