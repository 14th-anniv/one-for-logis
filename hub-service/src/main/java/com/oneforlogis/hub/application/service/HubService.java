package com.oneforlogis.hub.application.service;

import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.hub.domain.model.Hub;
import com.oneforlogis.hub.domain.repository.HubRepository;
import com.oneforlogis.hub.infrastructure.cache.HubCacheService;
import com.oneforlogis.hub.presentation.request.HubCreateRequest;
import com.oneforlogis.hub.presentation.request.HubUpdateRequest;
import com.oneforlogis.hub.presentation.response.HubResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HubService {

    private final HubRepository hubRepository;
    private final HubCacheService hubCacheService;

    @Transactional
    public HubResponse createHub(HubCreateRequest request) {
        Hub hub = Hub.create(request);
        hubRepository.save(hub);
        HubResponse response = HubResponse.from(hub);
        hubCacheService.saveHubCache(response);
        return response;
    }

    @Transactional
    public HubResponse updateHub(UUID hubId, HubUpdateRequest request) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new CustomException(ErrorCode.HUB_NOT_FOUND));
        if (hub.isDeleted()) throw new CustomException(ErrorCode.HUB_ALREADY_DELETED);

        hub.update(request);
        hubRepository.flush();
        HubResponse response = HubResponse.from(hub);
        hubCacheService.saveHubCache(response);
        return response;
    }

    @Transactional
    public void deleteHub(String userName, UUID hubId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new CustomException(ErrorCode.HUB_NOT_FOUND));
        if (hub.isDeleted()) throw new CustomException(ErrorCode.HUB_ALREADY_DELETED);
        hub.markAsDeleted(userName);
        hubCacheService.deleteHubCache(hubId);
    }

    @Transactional
    public void refreshHubCache() {
        List<Hub> hubs = hubRepository.findByDeletedFalse();
        hubCacheService.refreshHubListCache(hubs);
    }
}