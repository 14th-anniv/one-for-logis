package com.oneforlogis.order.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record OrderCreateResponse(
        @Schema(description = "주문 ID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID orderId
) {}

