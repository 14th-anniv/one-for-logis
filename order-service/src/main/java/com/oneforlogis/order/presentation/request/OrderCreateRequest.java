package com.oneforlogis.order.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record OrderCreateRequest(
        @Schema(description = "공급업체 회사 ID", example = "f29bde56-9a3b-4e5a-841c-7d0df6cbef01")
        @NotNull(message = "공급업체 회사 ID는 필수입니다.")
        UUID supplierCompanyId,

        @Schema(description = "수신업체 회사 ID", example = "12e35d99-2d9c-4fd3-9b43-2d7d9b5b7f63")
        @NotNull(message = "수신업체 회사 ID는 필수입니다.")
        UUID receiverCompanyId,

        @Schema(description = "요청 사항 (선택)", example = "문앞에 놔주세요")
        String requestNote,

        @Schema(description = "주문 상품 목록")
        @NotEmpty(message = "주문 항목은 최소 1개 이상 필요합니다.")
        @Valid
        List<OrderItemRequest> items
) {
    public record OrderItemRequest(
            @Schema(description = "상품의 고유 ID", example = "9c65c0ab-1c4e-4529-9a59-187b69f3c1a3")
            @NotNull(message = "상품 ID는 필수입니다.")
            UUID productId,

            @Schema(description = "상품 이름", example = "포장박스")
            @NotNull(message = "상품명은 필수입니다.")
            String productName,

            @Schema(description = "주문 수량", example = "3")
            @NotNull(message = "수량은 필수입니다.")
            Integer quantity
    ) {}
}

