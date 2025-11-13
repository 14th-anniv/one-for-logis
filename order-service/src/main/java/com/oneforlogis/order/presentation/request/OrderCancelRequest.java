package com.oneforlogis.order.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record OrderCancelRequest(
        @Schema(description = "취소 사유", example = "고객 요청으로 취소", required = true)
        @NotBlank(message = "취소 사유는 필수입니다.")
        String reason
) {}

