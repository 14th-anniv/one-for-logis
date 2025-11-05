package com.oneforlogis.hub.infrastructure.cache;

import com.oneforlogis.hub.domain.model.Hub;
import com.oneforlogis.hub.presentation.response.HubResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class HubCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String HUB_ID_KEY_PREFIX = "hub:id:";
    private static final String HUB_NAME_KEY_PREFIX = "hub:name:";

    public void saveHubCache(HubResponse hubResponse) {
        String idKey = HUB_ID_KEY_PREFIX + hubResponse.id();

        String encodedName = URLEncoder.encode(hubResponse.name(), StandardCharsets.UTF_8);
        String nameKey = HUB_NAME_KEY_PREFIX + encodedName;

        redisTemplate.opsForValue().set(idKey, hubResponse);
        redisTemplate.opsForValue().set(nameKey, hubResponse.id().toString());
    }

    public void deleteHubCache(UUID hubId) {
        HubResponse hubResponse = getHubCache(hubId);
        if (hubResponse != null) {
            String encodedName = URLEncoder.encode(hubResponse.name(), StandardCharsets.UTF_8);
            redisTemplate.delete(HUB_NAME_KEY_PREFIX + encodedName);
        }
        redisTemplate.delete(HUB_ID_KEY_PREFIX + hubId);
    }

    public HubResponse getHubCache(UUID hubId) {
        return (HubResponse) redisTemplate.opsForValue().get(HUB_ID_KEY_PREFIX + hubId);
    }

    public void refreshHubListCache(List<Hub> hubs) {
        Map<String, Object> combinedMap = new HashMap<>();
        for (Hub hub : hubs) {
            combinedMap.put(HUB_ID_KEY_PREFIX + hub.getId(), HubResponse.from(hub));
            combinedMap.put(HUB_NAME_KEY_PREFIX + URLEncoder.encode(hub.getName(), StandardCharsets.UTF_8), hub.getId().toString());
        }
        redisTemplate.opsForValue().multiSet(combinedMap);
    }
}