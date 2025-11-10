package com.oneforlogis.delivery.presentation.controller;

import com.oneforlogis.delivery.application.dto.DeliveryResponse;
import com.oneforlogis.delivery.application.dto.DeliverySearchCond;
import com.oneforlogis.delivery.application.service.DeliveryService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping("/{deliveryId}")
    public ResponseEntity<DeliveryResponse> getDeliveryById(
            @PathVariable UUID deliveryId
    ) {
        DeliveryResponse response = deliveryService.getOne(deliveryId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<DeliveryResponse>> searchDeliveries(
            @ModelAttribute DeliverySearchCond cond,
            Pageable pageable
    ) {
        Page<DeliveryResponse> page = deliveryService.search(cond, pageable);
        return ResponseEntity.ok(page);
    }
}