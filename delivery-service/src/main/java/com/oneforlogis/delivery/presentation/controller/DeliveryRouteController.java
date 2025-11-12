package com.oneforlogis.delivery.presentation.controller;

import com.oneforlogis.delivery.application.dto.DeliveryRouteRequest;
import com.oneforlogis.delivery.application.dto.DeliveryRouteResponse;
import com.oneforlogis.delivery.application.service.DeliveryRouteService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deliveries/{deliveryId}/routes")
public class DeliveryRouteController {

    private final DeliveryRouteService deliveryRouteService;

    @PostMapping
    public ResponseEntity<DeliveryRouteResponse> appendRouteEvent(
            @PathVariable UUID deliveryId,
            @Valid @RequestBody DeliveryRouteRequest request
    ) {
        DeliveryRouteResponse response = deliveryRouteService.appendEvent(deliveryId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}