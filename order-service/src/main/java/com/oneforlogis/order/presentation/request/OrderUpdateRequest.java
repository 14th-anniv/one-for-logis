package com.oneforlogis.order.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

public record OrderUpdateRequest(
        @Schema(description = "요청사항 (없으면 변경 없음)", example = "문앞에 놔주세요")
        String requestNote,

        @Schema(description = "공급업체 회사 ID (주문 접수 상태일 경우만 수정 가능)", example = "f29bde56-9a3b-4e5a-841c-7d0df6cbef01")
        UUID supplierCompanyId,

        @Schema(description = "수신업체 회사 ID (주문 접수 상태일 경우만 수정 가능)", example = "12e35d99-2d9c-4fd3-9b43-2d7d9b5b7f63")
        UUID receiverCompanyId,

        @Schema(description = "주문 상품 목록 (주문 접수 상태일 경우만 수정 가능)")
        List<OrderItemUpdateRequest> items
) {
    public record OrderItemUpdateRequest(
            @Schema(description = "상품의 고유 ID", example = "9c65c0ab-1c4e-4529-9a59-187b69f3c1a3")
            UUID productId,

            @Schema(description = "주문 수량", example = "3")
            Integer quantity
    ) {}
}

