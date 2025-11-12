package com.oneforlogis.notification.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

@Schema(description = "주문 알림 요청 DTO - order-service에서 주문 생성 시 호출")
public record OrderNotificationRequest(
        @Schema(description = "주문 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull(message = "주문 ID는 필수입니다.")
        UUID orderId,

        @Schema(description = "주문자 정보 (이름 / 이메일)", example = "김말숙 / msk@seafood.world")
        @NotBlank(message = "주문자 정보는 필수입니다.")
        String ordererInfo,

        @Schema(description = "공급업체명", example = "건조 식품 가공 업체")
        @NotBlank(message = "공급업체명은 필수입니다.")
        String requestingCompanyName,

        @Schema(description = "수령업체명", example = "수산물 도매 업체")
        @NotBlank(message = "수령업체명은 필수입니다.")
        String receivingCompanyName,

        @Schema(description = "상품 정보 (상품명 + 수량)", example = "마른 오징어 50박스")
        @NotBlank(message = "상품 정보는 필수입니다.")
        String productInfo,

        @Schema(description = "요청 사항 (납품 기한 등)", example = "12월 12일 3시까지는 보내주세요!")
        String requestDetails,

        @Schema(description = "출발 허브명", example = "경기 북부 센터")
        @NotBlank(message = "출발 허브명은 필수입니다.")
        String departureHub,

        @Schema(description = "경유 허브 목록", example = "[\"대전광역시 센터\", \"부산광역시 센터\"]")
        List<String> waypoints,

        @Schema(description = "도착 허브명", example = "부산광역시 센터")
        @NotBlank(message = "도착 허브명은 필수입니다.")
        String destinationHub,

        @Schema(description = "최종 배송지 주소", example = "부산시 사하구 낙동대로 1번길 1 해산물월드")
        @NotBlank(message = "최종 배송지 주소는 필수입니다.")
        String destinationAddress,

        @Schema(description = "배송 담당자 정보 (이름 / 슬랙ID)", example = "고길동 / kdk@sparta.world")
        @NotBlank(message = "배송 담당자 정보는 필수입니다.")
        String deliveryPersonInfo,

        @Schema(description = "발송 허브 관리자 Slack ID", example = "U01234ABCDE")
        @NotBlank(message = "수신자 Slack ID는 필수입니다.")
        String recipientSlackId,

        @Schema(description = "발송 허브 관리자 이름", example = "김관리")
        @NotBlank(message = "수신자 이름은 필수입니다.")
        String recipientName
) {
}
