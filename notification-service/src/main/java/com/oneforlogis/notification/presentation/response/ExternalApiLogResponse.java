package com.oneforlogis.notification.presentation.response;

import com.oneforlogis.notification.domain.model.ApiProvider;
import com.oneforlogis.notification.domain.model.ExternalApiLog;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Schema(description = "외부 API 호출 로그 조회 응답 DTO")
public record ExternalApiLogResponse(
        @Schema(description = "로그 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "API 제공자 (SLACK, GEMINI, NAVER_MAPS)", example = "SLACK")
        ApiProvider apiProvider,

        @Schema(description = "API 메서드명", example = "chat.postMessage")
        String apiMethod,

        @Schema(description = "요청 데이터 (JSONB)")
        Map<String, Object> requestData,

        @Schema(description = "응답 데이터 (JSONB)")
        Map<String, Object> responseData,

        @Schema(description = "HTTP 상태 코드", example = "200")
        Integer httpStatus,

        @Schema(description = "성공 여부", example = "true")
        Boolean isSuccess,

        @Schema(description = "에러 코드")
        String errorCode,

        @Schema(description = "에러 메시지")
        String errorMessage,

        @Schema(description = "응답 시간 (milliseconds)", example = "1250")
        Long durationMs,

        @Schema(description = "API 호출 비용", example = "0.0025")
        BigDecimal cost,

        @Schema(description = "API 호출 시각 (ISO 8601 형식)", example = "2025-11-07T10:30:15")
        String calledAt,

        @Schema(description = "연관된 메시지 ID (UUID)", example = "650e8400-e29b-41d4-a716-446655440000")
        UUID messageId
) {
    public static ExternalApiLogResponse from(ExternalApiLog log) {
        return new ExternalApiLogResponse(
                log.getId(),
                log.getApiProvider(),
                log.getApiMethod(),
                log.getRequestData(),
                log.getResponseData(),
                log.getHttpStatus(),
                log.getIsSuccess(),
                log.getErrorCode(),
                log.getErrorMessage(),
                log.getDurationMs(),
                log.getCost(),
                log.getCalledAt().toString(),
                log.getMessageId()
        );
    }
}
