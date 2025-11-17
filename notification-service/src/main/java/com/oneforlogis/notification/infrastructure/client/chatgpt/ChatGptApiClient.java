package com.oneforlogis.notification.infrastructure.client.chatgpt;

/*
 * NOTE: ChatGPT는 현재 사용하지 않고 Gemini API로 대체되었습니다.
 * 향후 필요시 참고용으로 주석처리하여 보관합니다.
 */

/*
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

// ChatGPT API 클라이언트 (WebClient 기반)
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatGptApiClient {

    private static final String CHATGPT_API_BASE_URL = "https://api.openai.com/v1";
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final WebClient.Builder webClientBuilder;
    private final Retry chatGptRetry;

    @Value("${external-api.chatgpt.api-key}")
    private String apiKey;

    // ChatGPT 요청 (동기 방식)
    public ChatGptResponse generateCompletion(ChatGptRequest request) {
        log.info("[ChatGptApiClient] Generating completion with model: {}", request.getModel());

        return webClientBuilder
                .baseUrl(CHATGPT_API_BASE_URL)
                .build()
                .post()
                .uri(CHAT_COMPLETIONS_PATH)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatGptResponse.class)
                .transformDeferred(RetryOperator.of(chatGptRetry)) // Resilience4j Retry
                .doOnError(error -> log.error("[ChatGptApiClient] Failed to generate completion: {}", error.getMessage()))
                .block(); // 동기 호출
    }

    // ChatGPT 요청 (비동기 방식)
    public Mono<ChatGptResponse> generateCompletionAsync(ChatGptRequest request) {
        log.info("[ChatGptApiClient] Generating completion (async) with model: {}", request.getModel());

        return webClientBuilder
                .baseUrl(CHATGPT_API_BASE_URL)
                .build()
                .post()
                .uri(CHAT_COMPLETIONS_PATH)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatGptResponse.class)
                .transformDeferred(RetryOperator.of(chatGptRetry))
                .doOnError(error -> log.error("[ChatGptApiClient] Failed to generate completion (async): {}", error.getMessage()));
    }
}
*/