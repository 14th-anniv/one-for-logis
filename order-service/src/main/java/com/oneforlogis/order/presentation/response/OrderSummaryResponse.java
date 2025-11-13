package com.oneforlogis.order.presentation.response;

import com.oneforlogis.order.domain.model.Order;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record OrderSummaryResponse(
        @Schema(description = "주문 ID", example = "b8e3b4b9-1a3c-4a2f-9d8b-3d2a1e7f0c2b")
        UUID orderId,

        @Schema(description = "주문번호(가시용)", example = "ORD-20251103-001")
        String orderNo,

        @Schema(description = "주문 상태", example = "PENDING")
        String status,

        @Schema(description = "공급업체 ID", example = "f29bde56-9a3b-4e5a-841c-7d0df6cbef01")
        UUID supplierCompanyId,

        @Schema(description = "수령업체 ID", example = "12e35d99-2d9c-4fd3-9b43-2d7d9b5b7f63")
        UUID receiverCompanyId,

        @Schema(description = "생성 시각", example = "2025-11-03T13:00:00")
        String createdAt
) {
    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNo(),
                order.getStatus().name(),
                order.getSupplierCompanyId(),
                order.getReceiverCompanyId(),
                order.getCreatedAt() != null ? order.getCreatedAt().toString() : null
        );
    }
}

