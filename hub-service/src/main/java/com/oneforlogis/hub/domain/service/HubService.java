package com.oneforlogis.hub.domain.service;

import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.hub.domain.model.Hub;
import com.oneforlogis.hub.domain.repository.HubRepository;
import com.oneforlogis.hub.presentation.request.HubCreateRequest;
import com.oneforlogis.hub.presentation.request.HubUpdateRequest;
import com.oneforlogis.hub.presentation.response.HubResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HubService {

    private final HubRepository hubRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @CachePut(value = "hub", key = "#result.id")
    @Transactional
    public HubResponse createHub(HubCreateRequest request) {
        Hub hub = Hub.create(request);
        hubRepository.save(hub);
        return HubResponse.from(hub);
    }

    @CachePut(value = "hub", key = "#hubId")
    @Transactional
    public HubResponse updateHub(UUID hubId, HubUpdateRequest request) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new CustomException(ErrorCode.HUB_NOT_FOUND));
        if (hub.isDeleted()) {
            throw new CustomException(ErrorCode.HUB_ALREADY_DELETED);
        }
        hub.update(request);
        hubRepository.flush();
        return HubResponse.from(hub);
    }

    @CacheEvict(value = "hub", key = "#hubId")
    @Transactional
    public void deleteHub(String userName, UUID hubId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new CustomException(ErrorCode.HUB_NOT_FOUND));
        if (hub.isDeleted()) {
            throw new CustomException(ErrorCode.HUB_ALREADY_DELETED);
        }
        hub.markAsDeleted(userName);
    }

    @Transactional
    public void refreshHubCache() {
        List<Hub> hubs = hubRepository.findByDeletedFalse();
        Map<String, HubResponse> hubMap = hubs.stream()
                .collect(Collectors.toMap(
                        hub -> "hub:" + hub.getId(),
                        HubResponse::from
                ));
        redisTemplate.opsForValue().multiSet(hubMap);
    }
}
