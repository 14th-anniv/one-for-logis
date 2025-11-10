package com.oneforlogis.notification.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "수동 메시지 발송 요청 DTO - 인증된 사용자가 직접 Slack 메시지 발송")
public record ManualNotificationRequest(
        @Schema(description = "수신자 Slack ID", example = "U01234ABCDE")
        @NotBlank(message = "수신자 Slack ID는 필수입니다.")
        String recipientSlackId,

        @Schema(description = "수신자 이름", example = "김담당")
        @NotBlank(message = "수신자 이름은 필수입니다.")
        String recipientName,

        @Schema(description = "메시지 내용", example = "긴급 배송 건이 추가되었습니다. 확인 부탁드립니다.")
        @NotBlank(message = "메시지 내용은 필수입니다.")
        String messageContent
) {
}
