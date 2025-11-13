package com.oneforlogis.order.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record OrderStatusChangeRequest(
        @Schema(description = "변경할 목표 상태", example = "SHIPPED", required = true)
        @NotBlank(message = "변경할 상태는 필수입니다.")
        String toStatus,

        @Schema(description = "상태 변경 사유", example = "출고 완료")
        String reason
) {}

