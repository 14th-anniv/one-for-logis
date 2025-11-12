package com.oneforlogis.notification.infrastructure.client.slack;

import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

// Slack API 클라이언트 (WebClient 기반)
@Slf4j
@Component
@RequiredArgsConstructor
public class SlackApiClient {

    private static final String POST_MESSAGE_PATH = "/chat.postMessage";

    private final WebClient slackWebClient;
    private final Retry slackRetry;

    @Value("${external-api.slack.bot-token}")
    private String botToken;

    // Slack 메시지 전송 (동기 방식)
    public SlackMessageResponse postMessage(SlackMessageRequest request) {
        log.info("[SlackApiClient] Posting message to channel: {}", maskSensitiveInfo(request.getChannel()));

        return slackWebClient
                .post()
                .uri(POST_MESSAGE_PATH)
                .header("Authorization", "Bearer " + botToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SlackMessageResponse.class)
                .transformDeferred(RetryOperator.of(slackRetry)) // Resilience4j Retry
                .doOnError(error -> log.error("[SlackApiClient] Failed to post message: {}", error.getMessage()))
                .block(); // 동기 호출
    }

    // Slack 메시지 전송 (비동기 방식)
    public Mono<SlackMessageResponse> postMessageAsync(SlackMessageRequest request) {
        log.info("[SlackApiClient] Posting message to channel (async): {}", maskSensitiveInfo(request.getChannel()));

        return slackWebClient
                .post()
                .uri(POST_MESSAGE_PATH)
                .header("Authorization", "Bearer " + botToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SlackMessageResponse.class)
                .transformDeferred(RetryOperator.of(slackRetry))
                .doOnError(error -> log.error("[SlackApiClient] Failed to post message (async): {}", error.getMessage()));
    }

    // 민감 정보 마스킹 (channel ID 일부 숨김)
    private String maskSensitiveInfo(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 4) + "****";
    }
}