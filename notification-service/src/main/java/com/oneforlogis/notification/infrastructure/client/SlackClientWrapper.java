package com.oneforlogis.notification.infrastructure.client;

import com.oneforlogis.notification.domain.model.ApiProvider;
import com.oneforlogis.notification.domain.service.ApiLogDomainService;
import com.oneforlogis.notification.infrastructure.client.slack.SlackApiClient;
import com.oneforlogis.notification.infrastructure.client.slack.SlackMessageRequest;
import com.oneforlogis.notification.infrastructure.client.slack.SlackMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

// Slack API 클라이언트 Wrapper (자동 로깅 + 에러 핸들링)
@Slf4j
@Component
@RequiredArgsConstructor
public class SlackClientWrapper {

    private final SlackApiClient slackApiClient;
    private final ApiLogDomainService apiLogDomainService;

    // Slack 메시지 전송 with auto-logging
    public SlackMessageResponse postMessage(SlackMessageRequest request, UUID messageId) {
        long startTime = System.currentTimeMillis();
        Integer httpStatus = null;
        boolean isSuccess = false;
        String errorCode = null;
        String errorMessage = null;
        SlackMessageResponse response = null;

        try {
            response = slackApiClient.postMessage(request);

            if (response != null && response.isOk()) {
                isSuccess = true;
                httpStatus = 200;
            } else {
                httpStatus = 400;
                errorCode = response != null ? response.getError() : "UNKNOWN_ERROR";
                errorMessage = "Slack API returned ok=false";
            }

        } catch (Exception e) {
            log.error("[SlackClientWrapper] Exception during Slack API call: {}", e.getMessage(), e);
            httpStatus = 500;
            errorCode = e.getClass().getSimpleName();
            errorMessage = e.getMessage();

            // Fallback: 기본 응답 반환
            response = createErrorResponse(errorCode, errorMessage);

        } finally {
            long durationMs = System.currentTimeMillis() - startTime;

            // API 호출 로그 저장
            apiLogDomainService.logApiCall(
                    ApiProvider.SLACK,
                    "chat.postMessage",
                    request,
                    response,
                    httpStatus,
                    isSuccess,
                    errorCode,
                    errorMessage,
                    durationMs,
                    BigDecimal.ZERO, // Slack API는 무료
                    messageId
            );
        }

        return response;
    }

    // 에러 응답 생성 (Fallback)
    private SlackMessageResponse createErrorResponse(String errorCode, String errorMessage) {
        SlackMessageResponse errorResponse = new SlackMessageResponse();
        // Note: SlackMessageResponse는 final 필드라 직접 설정 불가 - 실제로는 Builder 패턴 필요
        log.warn("[SlackClientWrapper] Returning fallback error response: {} - {}", errorCode, errorMessage);
        return errorResponse;
    }
}