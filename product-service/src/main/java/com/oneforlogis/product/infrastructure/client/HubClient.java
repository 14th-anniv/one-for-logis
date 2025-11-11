package com.oneforlogis.product.infrastructure.client;

import com.oneforlogis.common.api.ApiResponse;
import com.oneforlogis.product.infrastructure.client.dto.HubResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "hub-service", path = "/api/v1/hubs")
public interface HubClient {

    @GetMapping("/{hubId}")
    ApiResponse<HubResponse> getHub(@PathVariable UUID hubId);
}
