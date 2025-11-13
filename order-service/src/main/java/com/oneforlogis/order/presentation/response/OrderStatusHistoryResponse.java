package com.oneforlogis.order.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderStatusHistoryResponse(
        @Schema(description = "이력 고유 ID", example = "a6d1c8f2-9b8e-4c9d-a2f1-7c3b9c1d2e4f")
        UUID historyId,

        @Schema(description = "변경 전 상태", example = "PACKING")
        String fromStatus,

        @Schema(description = "변경 후 상태", example = "SHIPPED")
        String toStatus,

        @Schema(description = "변경 사유", example = "출고 완료")
        String reason,

        @Schema(description = "변경 시각(정렬 기준)", example = "2025-11-03T14:40:00")
        LocalDateTime changedAt
) {
    public static OrderStatusHistoryResponse from(
            com.oneforlogis.order.domain.model.OrderStatusHistory history) {
        return new OrderStatusHistoryResponse(
                history.getId(),
                history.getFromStatus().name(),
                history.getToStatus().name(),
                history.getReason(),
                history.getCreatedAt()
        );
    }
}

