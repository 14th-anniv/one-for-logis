package com.oneforlogis.notification.infrastructure.client;

import com.oneforlogis.notification.domain.model.ApiProvider;
import com.oneforlogis.notification.domain.service.ApiLogDomainService;
import com.oneforlogis.notification.infrastructure.client.gemini.GeminiApiClient;
import com.oneforlogis.notification.infrastructure.client.gemini.GeminiRequest;
import com.oneforlogis.notification.infrastructure.client.gemini.GeminiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

// Gemini API 클라이언트 Wrapper (자동 로깅 + 에러 핸들링)
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClientWrapper {

    private final GeminiApiClient geminiApiClient;
    private final ApiLogDomainService apiLogDomainService;

    // Gemini 요청 with auto-logging
    public GeminiResponse generateContent(GeminiRequest request, UUID messageId) {
        long startTime = System.currentTimeMillis();
        Integer httpStatus = null;
        boolean isSuccess = false;
        String errorCode = null;
        String errorMessage = null;
        GeminiResponse response = null;

        try {
            response = geminiApiClient.generateContent(request);

            if (response != null && response.getContent() != null) {
                isSuccess = true;
                httpStatus = 200;
            } else {
                httpStatus = 400;
                errorCode = "EMPTY_RESPONSE";
                errorMessage = "Gemini returned empty content";
            }

        } catch (Exception e) {
            log.error("[GeminiClientWrapper] Exception during Gemini API call: {}", e.getMessage(), e);
            httpStatus = 500;
            errorCode = e.getClass().getSimpleName();
            errorMessage = e.getMessage();

            // Fallback: 기본 응답 반환
            response = createFallbackResponse();

        } finally {
            long durationMs = System.currentTimeMillis() - startTime;

            // API 호출 로그 저장 (Gemini는 무료 티어라 비용 0)
            apiLogDomainService.logApiCall(
                    ApiProvider.GEMINI,
                    "generateContent",
                    request,
                    response,
                    httpStatus,
                    isSuccess,
                    errorCode,
                    errorMessage,
                    durationMs,
                    BigDecimal.ZERO, // Gemini 무료 티어
                    messageId
            );
        }

        return response;
    }

    // Fallback 응답 생성
    private GeminiResponse createFallbackResponse() {
        log.warn("[GeminiClientWrapper] Returning fallback response");
        return new GeminiResponse(); // 빈 응답
    }
}