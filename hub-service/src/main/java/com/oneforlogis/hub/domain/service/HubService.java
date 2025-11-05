package com.oneforlogis.hub.domain.service;

import com.oneforlogis.hub.domain.model.Hub;
import com.oneforlogis.hub.domain.repository.HubRepository;
import com.oneforlogis.hub.presentation.request.HubCreateRequest;
import com.oneforlogis.hub.presentation.response.HubCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HubService {

    private final HubRepository hubRepository;

    public HubCreateResponse createHub(HubCreateRequest request) {
        Hub hub = Hub.create(request);
        hubRepository.save(hub);
        return HubCreateResponse.from(hub);
    }
}
