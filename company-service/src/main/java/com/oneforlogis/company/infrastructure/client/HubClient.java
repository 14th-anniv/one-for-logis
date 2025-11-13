package com.oneforlogis.company.infrastructure.client;

import com.oneforlogis.common.api.ApiResponse;
import com.oneforlogis.company.infrastructure.client.dto.HubResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * company <-> hub-service 통신 (외부 호출용 FeignClient)
 */
@FeignClient(name = "hub-service", path = "/api/v1/internal/hubs")
public interface HubClient {

    @GetMapping("/{hubId}")
    ApiResponse<HubResponse> getHub(@PathVariable UUID hubId);
}
