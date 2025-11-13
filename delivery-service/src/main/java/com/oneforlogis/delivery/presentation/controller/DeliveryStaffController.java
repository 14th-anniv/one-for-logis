package com.oneforlogis.delivery.presentation.controller;

import com.oneforlogis.common.api.ApiResponse;
import com.oneforlogis.delivery.application.dto.request.DeliveryStaffRequest;
import com.oneforlogis.delivery.application.service.DeliveryStaffService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deliveries")
public class DeliveryStaffController {

    private final DeliveryStaffService deliveryStaffService;

    @PostMapping("/{deliveryId}/staff")
    public ResponseEntity<ApiResponse<String>> registerDeliveryStaff(
            @PathVariable UUID deliveryId,
            @Valid @RequestBody DeliveryStaffRequest request
    ) {
        deliveryStaffService.register(deliveryId, request);
        return ResponseEntity.ok(ApiResponse.success("배송 담당자가 등록되었습니다."));
    }
}