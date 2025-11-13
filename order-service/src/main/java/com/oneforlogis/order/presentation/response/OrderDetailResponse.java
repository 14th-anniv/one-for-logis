package com.oneforlogis.order.presentation.response;

import com.oneforlogis.order.domain.model.Order;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderDetailResponse(
        @Schema(description = "주문 고유 ID", example = "5c7a9b6d-2f9c-4f1a-8f63-3b2e4f1c9a77")
        UUID orderId,

        @Schema(description = "가시적 주문번호", example = "ORD-20251103-001")
        String orderNo,

        @Schema(description = "공급업체 ID", example = "f29bde56-9a3b-4e5a-841c-7d0df6cbef01")
        UUID supplierCompanyId,

        @Schema(description = "수령업체 ID", example = "12e35d99-2d9c-4fd3-9b43-2d7d9b5b7f63")
        UUID receiverCompanyId,

        @Schema(description = "주문 상태", example = "PACKING")
        String status,

        @Schema(description = "요청사항", example = "문앞에 놔주세요")
        String requestNote,

        @Schema(description = "주문 상품 목록")
        List<OrderItemResponse> items,

        @Schema(description = "총 금액", example = "4800.00")
        BigDecimal totalAmount,

        @Schema(description = "연계된 배송 엔티티 ID", example = "a37b0b45-89a0-4a0d-bb29-5b63ad87f3b1")
        UUID deliveryId,

        @Schema(description = "생성 시각", example = "2025-11-03T13:00:00")
        String createdAt,

        @Schema(description = "수정 시각", example = "2025-11-03T13:10:00")
        String updatedAt
) {
    public record OrderItemResponse(
            @Schema(description = "상품 ID", example = "9c65c0ab-1c4e-4529-9a59-187b69f3c1a3")
            UUID productId,

            @Schema(description = "상품명", example = "포장박스")
            String productName,

            @Schema(description = "주문 시점의 상품 단가", example = "1000.00")
            BigDecimal unitPrice,

            @Schema(description = "수량", example = "3")
            Integer quantity,

            @Schema(description = "상품단가*수량", example = "3000.00")
            BigDecimal lineTotal
    ) {}

    public static OrderDetailResponse from(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getLineTotal()
                ))
                .toList();

        return new OrderDetailResponse(
                order.getId(),
                order.getOrderNo(),
                order.getSupplierCompanyId(),
                order.getReceiverCompanyId(),
                order.getStatus().name(),
                order.getRequestNote(),
                itemResponses,
                order.getTotalAmount(),
                order.getDeliveryId(),
                order.getCreatedAt() != null ? order.getCreatedAt().toString() : null,
                order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null
        );
    }
}

