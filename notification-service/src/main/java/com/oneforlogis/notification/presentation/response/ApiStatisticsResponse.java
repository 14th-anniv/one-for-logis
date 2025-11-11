package com.oneforlogis.notification.presentation.response;

import com.oneforlogis.notification.domain.model.ApiProvider;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * API 통계 조회 응답 DTO
 */
@Schema(description = "API 통계 조회 응답")
public record ApiStatisticsResponse(
        @Schema(description = "API 제공자", example = "SLACK")
        ApiProvider apiProvider,

        @Schema(description = "총 호출 수", example = "150")
        long totalCalls,

        @Schema(description = "성공한 호출 수", example = "145")
        long successCalls,

        @Schema(description = "실패한 호출 수", example = "5")
        long failedCalls,

        @Schema(description = "성공률 (0-100)", example = "96.67")
        double successRate,

        @Schema(description = "평균 응답 시간 (ms)", example = "234.5")
        double avgResponseTime,

        @Schema(description = "최소 응답 시간 (ms)", example = "120")
        long minResponseTime,

        @Schema(description = "최대 응답 시간 (ms)", example = "1500")
        long maxResponseTime,

        @Schema(description = "총 비용 (Gemini API만 해당)", example = "0.0042")
        BigDecimal totalCost
) {
    public static ApiStatisticsResponse of(
            ApiProvider apiProvider,
            long totalCalls,
            long successCalls,
            long failedCalls,
            double avgResponseTime,
            long minResponseTime,
            long maxResponseTime,
            BigDecimal totalCost
    ) {
        double successRate = totalCalls > 0 ? (successCalls * 100.0 / totalCalls) : 0.0;

        return new ApiStatisticsResponse(
                apiProvider,
                totalCalls,
                successCalls,
                failedCalls,
                Math.round(successRate * 100.0) / 100.0, // 소수점 2자리
                Math.round(avgResponseTime * 100.0) / 100.0, // 소수점 2자리
                minResponseTime,
                maxResponseTime,
                totalCost != null ? totalCost : BigDecimal.ZERO
        );
    }
}
