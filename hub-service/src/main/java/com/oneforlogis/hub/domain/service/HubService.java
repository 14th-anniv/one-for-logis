package com.oneforlogis.hub.domain.service;

import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.hub.domain.model.Hub;
import com.oneforlogis.hub.domain.repository.HubRepository;
import com.oneforlogis.hub.presentation.request.HubCreateRequest;
import com.oneforlogis.hub.presentation.request.HubUpdateRequest;
import com.oneforlogis.hub.presentation.response.HubCreateResponse;
import com.oneforlogis.hub.presentation.response.HubUpdateResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HubService {

    private final HubRepository hubRepository;

    @Transactional
    public HubCreateResponse createHub(HubCreateRequest request) {
        Hub hub = Hub.create(request);
        hubRepository.save(hub);
        return HubCreateResponse.from(hub);
    }

    @Transactional
    public HubUpdateResponse updateHub(UUID hubId, HubUpdateRequest request) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new CustomException(ErrorCode.HUB_NOT_FOUND));
        if (hub.isDeleted()) {
            throw new CustomException(ErrorCode.HUB_ALREADY_DELETED);
        }
        hub.update(request);
        hubRepository.flush();
        return HubUpdateResponse.from(hub);
    }

    @Transactional
    public void deleteHub(String userName, UUID hubId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new CustomException(ErrorCode.HUB_NOT_FOUND));
        if (hub.isDeleted()) {
            throw new CustomException(ErrorCode.HUB_ALREADY_DELETED);
        }
        hub.markAsDeleted(userName);
    }
}
