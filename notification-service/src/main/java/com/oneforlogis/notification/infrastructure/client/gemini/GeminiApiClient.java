package com.oneforlogis.notification.infrastructure.client.gemini;

import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

// Gemini API 클라이언트 (WebClient 기반)
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiApiClient {

    private static final String MODEL = "gemini-2.5-flash-lite";
    private static final String GENERATE_CONTENT_PATH = "/models/" + MODEL + ":generateContent";

    private final WebClient geminiWebClient;
    private final Retry geminiRetry;

    @Value("${external-api.gemini.api-key}")
    private String apiKey;

    // Gemini 요청 (동기 방식)
    public GeminiResponse generateContent(GeminiRequest request) {
        log.info("[GeminiApiClient] Generating content with model: {}", MODEL);

        return geminiWebClient
                .post()
                .uri(GENERATE_CONTENT_PATH)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeminiResponse.class)
                .transformDeferred(RetryOperator.of(geminiRetry)) // Resilience4j Retry
                .doOnError(error -> log.error("[GeminiApiClient] Failed to generate content: {}", error.getMessage()))
                .block(); // 동기 호출
    }

    // Gemini 요청 (비동기 방식)
    public Mono<GeminiResponse> generateContentAsync(GeminiRequest request) {
        log.info("[GeminiApiClient] Generating content (async) with model: {}", MODEL);

        return geminiWebClient
                .post()
                .uri(GENERATE_CONTENT_PATH)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeminiResponse.class)
                .transformDeferred(RetryOperator.of(geminiRetry))
                .doOnError(error -> log.error("[GeminiApiClient] Failed to generate content (async): {}", error.getMessage()));
    }
}