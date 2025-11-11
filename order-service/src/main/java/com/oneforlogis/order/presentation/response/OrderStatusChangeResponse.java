package com.oneforlogis.order.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record OrderStatusChangeResponse(
        @Schema(description = "주문 고유 ID", example = "5c7a9b6d-2f9c-4f1a-8f63-3b2e4f1c9a77")
        UUID orderId,

        @Schema(description = "변경 전 상태", example = "PACKING")
        String fromStatus,

        @Schema(description = "변경 후 상태", example = "SHIPPED")
        String toStatus,

        @Schema(description = "변경 시각", example = "2025-11-03T13:00:00")
        String changedAt
) {}

