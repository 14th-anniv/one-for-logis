package com.oneforlogis.order.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record OrderUpdateResponse(
        @Schema(description = "주문 고유 ID", example = "5c7a9b6d-2f9c-4f1a-8f63-3b2e4f1c9a77")
        UUID orderId
) {}

