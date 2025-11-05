package com.oneforlogis.hub.application.service;

import com.oneforlogis.common.api.PageResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
        if (hub.isDeleted()) throw new CustomException(ErrorCode.HUB_DELETED);

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
        if (hub.isDeleted()) throw new CustomException(ErrorCode.HUB_DELETED);
        hub.markAsDeleted(userName);
        hubCacheService.deleteHubCache(hubId);
    }

    @Transactional
    public void refreshHubCache() {
        List<Hub> hubs = hubRepository.findByDeletedFalse();
        hubCacheService.refreshHubListCache(hubs);
    }

    @Transactional(readOnly = true)
    public HubResponse getHubById(UUID hubId) {
        HubResponse cached = hubCacheService.getHubCache(hubId);
        if (cached != null) return cached;

        Hub hub = hubRepository.findByIdAndDeletedFalse(hubId)
                .orElseThrow(() -> new CustomException(ErrorCode.HUB_NOT_FOUND));

        HubResponse response = HubResponse.from(hub);
        hubCacheService.saveHubCache(response);
        return response;
    }

    @Transactional(readOnly = true)
    public HubResponse getHubByName(String hubName) {
        HubResponse cached = hubCacheService.getHubCacheByName(hubName);
        if (cached != null) return cached;

        Hub hub = hubRepository.findByNameAndDeletedFalse(hubName)
                .orElseThrow(() -> new CustomException(ErrorCode.HUB_NOT_FOUND));

        HubResponse response = HubResponse.from(hub);
        hubCacheService.saveHubCache(response);
        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<HubResponse> getAllHubs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Hub> pageData = hubRepository.findByDeletedFalse(pageable);
        return PageResponse.fromPage(pageData.map(HubResponse::from));
    }
}