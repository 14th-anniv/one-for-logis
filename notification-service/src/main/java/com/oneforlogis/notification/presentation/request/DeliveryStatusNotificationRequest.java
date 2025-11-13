package com.oneforlogis.notification.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "배송 상태 변경 알림 요청 DTO - delivery-service에서 배송 상태 변경 시 호출")
public record DeliveryStatusNotificationRequest(
        @Schema(description = "배송 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440001")
        @NotNull(message = "배송 ID는 필수입니다.")
        UUID deliveryId,

        @Schema(description = "주문 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull(message = "주문 ID는 필수입니다.")
        UUID orderId,

        @Schema(description = "이전 배송 상태", example = "HUB_WAITING")
        @NotBlank(message = "이전 배송 상태는 필수입니다.")
        String previousStatus,

        @Schema(description = "현재 배송 상태", example = "HUB_MOVING")
        @NotBlank(message = "현재 배송 상태는 필수입니다.")
        String currentStatus,

        @Schema(description = "수신자 Slack ID (허브 관리자 또는 배송 담당자)", example = "U01234ABCDE")
        @NotBlank(message = "수신자 Slack ID는 필수입니다.")
        String recipientSlackId,

        @Schema(description = "수신자 이름", example = "김배송")
        @NotBlank(message = "수신자 이름은 필수입니다.")
        String recipientName
) {
}
