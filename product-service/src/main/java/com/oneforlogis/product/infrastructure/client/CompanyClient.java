package com.oneforlogis.product.infrastructure.client;

import com.oneforlogis.common.api.ApiResponse;
import com.oneforlogis.product.infrastructure.client.dto.CompanyResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "company-service", path = "/api/v1/internal/companies")
public interface CompanyClient {

    @GetMapping("/{companyId}")
    ApiResponse<CompanyResponse> getCompany(@PathVariable UUID companyId);
}