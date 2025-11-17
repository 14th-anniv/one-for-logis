package com.oneforlogis.notification.infrastructure.client;

/*
 * NOTE: ChatGPT는 현재 사용하지 않고 Gemini API로 대체되었습니다.
 * 향후 필요시 참고용으로 주석처리하여 보관합니다.
 */

/*
import com.oneforlogis.notification.application.service.ExternalApiLogService;
import com.oneforlogis.notification.domain.model.ApiProvider;
import com.oneforlogis.notification.infrastructure.client.chatgpt.ChatGptApiClient;
import com.oneforlogis.notification.infrastructure.client.chatgpt.ChatGptRequest;
import com.oneforlogis.notification.infrastructure.client.chatgpt.ChatGptResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

// ChatGPT API 클라이언트 Wrapper (자동 로깅 + 에러 핸들링 + 비용 계산)
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatGptClientWrapper {

    private final ChatGptApiClient chatGptApiClient;
    private final ExternalApiLogService externalApiLogService;

    // ChatGPT 요청 with auto-logging
    public ChatGptResponse generateCompletion(ChatGptRequest request, UUID messageId) {
        long startTime = System.currentTimeMillis();
        Integer httpStatus = null;
        boolean isSuccess = false;
        String errorCode = null;
        String errorMessage = null;
        ChatGptResponse response = null;
        BigDecimal cost = BigDecimal.ZERO;

        try {
            response = chatGptApiClient.generateCompletion(request);

            if (response != null && response.getContent() != null) {
                isSuccess = true;
                httpStatus = 200;

                // 비용 계산 (예시: gpt-3.5-turbo 가격)
                cost = calculateCost(response, request.getModel());
            } else {
                httpStatus = 400;
                errorCode = "EMPTY_RESPONSE";
                errorMessage = "ChatGPT returned empty content";
            }

        } catch (Exception e) {
            log.error("[ChatGptClientWrapper] Exception during ChatGPT API call: {}", e.getMessage(), e);
            httpStatus = 500;
            errorCode = e.getClass().getSimpleName();
            errorMessage = e.getMessage();

            // Fallback: 기본 응답 반환
            response = createFallbackResponse();

        } finally {
            long durationMs = System.currentTimeMillis() - startTime;

            // API 호출 로그 저장
            externalApiLogService.logApiCall(
                    ApiProvider.CHATGPT,
                    "chat.completions",
                    request,
                    response,
                    httpStatus,
                    isSuccess,
                    errorCode,
                    errorMessage,
                    durationMs,
                    cost,
                    messageId
            );
        }

        return response;
    }

    // 비용 계산 (OpenAI 가격 기준, 2024년 1월)
    private BigDecimal calculateCost(ChatGptResponse response, String model) {
        if (response.getUsage() == null) {
            return BigDecimal.ZERO;
        }

        int promptTokens = response.getUsage().getPromptTokens();
        int completionTokens = response.getUsage().getCompletionTokens();

        // gpt-3.5-turbo: $0.001 per 1K prompt tokens, $0.002 per 1K completion tokens
        // gpt-4: $0.03 per 1K prompt tokens, $0.06 per 1K completion tokens
        BigDecimal promptCost;
        BigDecimal completionCost;

        if (model.contains("gpt-4")) {
            promptCost = BigDecimal.valueOf(promptTokens).multiply(BigDecimal.valueOf(0.03)).divide(BigDecimal.valueOf(1000), 6, BigDecimal.ROUND_HALF_UP);
            completionCost = BigDecimal.valueOf(completionTokens).multiply(BigDecimal.valueOf(0.06)).divide(BigDecimal.valueOf(1000), 6, BigDecimal.ROUND_HALF_UP);
        } else {
            promptCost = BigDecimal.valueOf(promptTokens).multiply(BigDecimal.valueOf(0.001)).divide(BigDecimal.valueOf(1000), 6, BigDecimal.ROUND_HALF_UP);
            completionCost = BigDecimal.valueOf(completionTokens).multiply(BigDecimal.valueOf(0.002)).divide(BigDecimal.valueOf(1000), 6, BigDecimal.ROUND_HALF_UP);
        }

        return promptCost.add(completionCost);
    }

    // Fallback 응답 생성
    private ChatGptResponse createFallbackResponse() {
        log.warn("[ChatGptClientWrapper] Returning fallback response");
        return new ChatGptResponse(); // 빈 응답
    }
}
*/