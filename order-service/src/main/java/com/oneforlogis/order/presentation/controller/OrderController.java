package com.oneforlogis.order.presentation.controller;

import com.oneforlogis.common.api.ApiResponse;
import com.oneforlogis.common.api.PageResponse;
import com.oneforlogis.order.application.service.OrderService;
import com.oneforlogis.order.presentation.request.OrderCreateRequest;
import com.oneforlogis.order.presentation.request.OrderStatusChangeRequest;
import com.oneforlogis.order.presentation.request.OrderUpdateRequest;
import com.oneforlogis.order.presentation.response.OrderCreateResponse;
import com.oneforlogis.order.presentation.response.OrderDetailResponse;
import com.oneforlogis.order.presentation.response.OrderStatusChangeResponse;
import com.oneforlogis.order.presentation.response.OrderSummaryResponse;
import com.oneforlogis.order.presentation.response.OrderUpdateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @Operation(summary = "주문 목록 조회", description = "필터링 및 페이지네이션을 지원하는 주문 목록을 조회합니다. 기본 정렬은 createdAt DESC입니다.")
    // TODO: 추후 Security/JWT 스펙 확정되면 활성화
    // @PreAuthorize("hasAnyRole('MASTER','SUPPLIER_MANAGER','HUB_MANAGER')")
    @GetMapping
    public ApiResponse<PageResponse<OrderSummaryResponse>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) String receiverId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "createdAt,DESC") String sort
    ) {
        PageResponse<OrderSummaryResponse> response = orderService.getOrders(
                status, supplierId, receiverId, startDate, endDate, page, size, sort
        );
        return ApiResponse.success(response);
    }

    @Operation(summary = "주문 상태 변경", description = "주문의 상태를 변경합니다. 상태 전이 규칙에 따라 유효한 상태 변경만 허용됩니다.")
    // TODO: 추후 Security/JWT 스펙 확정되면 활성화
    // @PreAuthorize("hasAnyRole('MASTER','SUPPLIER_MANAGER','HUB_MANAGER')")
    @PatchMapping("/{orderId}/status")
    public ApiResponse<OrderStatusChangeResponse> changeStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderStatusChangeRequest request
    ) {
        OrderStatusChangeResponse response = orderService.changeStatus(orderId, request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "주문 정보 수정",
            description = "주문 정보를 수정합니다. " +
                    "PENDING 상태일 경우에만 수정이 가능합니다. " +
                    "단, requestNote 필드만 DELIVERED, CANCELED가 아니라면 수정 가능합니다. " +
                    "상태가 CANCELED 또는 DELIVERED인 주문은 수정할 수 없습니다."
    )
    // TODO: 추후 Security/JWT 스펙 확정되면 활성화
    // @PreAuthorize("hasAnyRole('MASTER','SUPPLIER_MANAGER','HUB_MANAGER')")
    @PatchMapping("/{orderId}")
    public ApiResponse<OrderUpdateResponse> updateOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderUpdateRequest request
    ) {
        OrderUpdateResponse response = orderService.updateOrder(orderId, request);
        return ApiResponse.success(response);
    }
}

