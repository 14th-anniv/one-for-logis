package com.oneforlogis.order.presentation.controller;

import com.oneforlogis.common.api.ApiResponse;
import com.oneforlogis.order.application.service.OrderService;
import com.oneforlogis.order.presentation.request.OrderCreateRequest;
import com.oneforlogis.order.presentation.response.OrderCreateResponse;
import com.oneforlogis.order.presentation.response.OrderDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Orders", description = "주문 관리 API")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "주문 생성", description = "새로운 주문을 생성합니다.")
    // TODO: 추후 Security/JWT 스펙 확정되면 활성화
    // @PreAuthorize("hasAnyRole('MASTER','SUPPLIER_MANAGER','HUB_MANAGER')")
    @PostMapping
    public ApiResponse<OrderCreateResponse> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        OrderCreateResponse response = orderService.createOrder(request);
        return ApiResponse.created(response);
    }

    @Operation(summary = "주문 상세 조회", description = "주문 ID로 주문 상세 정보를 조회합니다.")
    // TODO: 추후 Security/JWT 스펙 확정되면 활성화
    // @PreAuthorize("hasAnyRole('MASTER','SUPPLIER_MANAGER','HUB_MANAGER')")
    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailResponse> getOrderById(@PathVariable UUID orderId) {
        OrderDetailResponse response = orderService.getOrderById(orderId);
        return ApiResponse.success(response);
    }
}

