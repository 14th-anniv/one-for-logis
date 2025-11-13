package com.oneforlogis.delivery.presentation.controller;

import com.oneforlogis.common.api.ApiResponse;
import com.oneforlogis.delivery.application.dto.request.DeliveryStaffRequest;
import com.oneforlogis.delivery.application.dto.response.DeliveryStaffResponse;
import com.oneforlogis.delivery.application.service.DeliveryStaffService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deliveries-staff")
public class DeliveryStaffController {

    private final DeliveryStaffService deliveryStaffService;

    @PostMapping("/{deliveryId}")
    public ApiResponse<Void> registerDeliveryStaff(
            @PathVariable UUID deliveryId,
            @Valid @RequestBody DeliveryStaffRequest request
    ) {
        deliveryStaffService.register(deliveryId, request);
        return ApiResponse.success();
    }

    @GetMapping("/{hubId}")
    public ApiResponse<List<DeliveryStaffResponse>> getStaffByHub(
            @PathVariable UUID hubId,
            Pageable pageable
    ) {
        Page<DeliveryStaffResponse> page = deliveryStaffService.getStaffByHub(hubId, pageable);
        return ApiResponse.success(page.getContent());
    }

    @GetMapping("/{hubId}/next")
    public ApiResponse<DeliveryStaffResponse> getNextStaff(@PathVariable UUID hubId) {
        DeliveryStaffResponse res = deliveryStaffService.getNextStaff(hubId);
        return ApiResponse.success(res);
    }
}