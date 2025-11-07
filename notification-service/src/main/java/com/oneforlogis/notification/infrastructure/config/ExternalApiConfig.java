package com.oneforlogis.notification.infrastructure.config;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

// 외부 API 클라이언트 설정 (WebClient, Resilience4j Retry)
@Slf4j
@Configuration
public class ExternalApiConfig {

    // Slack WebClient Bean 등록
    @Bean
    public WebClient slackWebClient() {
        return WebClient.builder()
                .baseUrl("https://slack.com/api")
                .build();
    }

    // Gemini WebClient Bean 등록
    @Bean
    public WebClient geminiWebClient() {
        return WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    // Slack API Retry 설정 (3회 재시도, Exponential Backoff)
    @Bean
    public Retry slackRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3) // 최대 3회 재시도
                .intervalFunction(io.github.resilience4j.core.IntervalFunction.ofExponentialBackoff(1000, 2)) // 지수 백오프 (1초 * 2^n)
                .retryExceptions(Exception.class) // 모든 예외에 대해 재시도
                .build();

        Retry retry = Retry.of("slackRetry", config);

        retry.getEventPublisher()
                .onRetry(event -> log.warn("[Slack Retry] Attempt #{}: {}",
                        event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()))
                .onSuccess(event -> log.info("[Slack Retry] Success after {} attempts",
                        event.getNumberOfRetryAttempts()));

        return retry;
    }

    // Gemini API Retry 설정 (2회 재시도, Exponential Backoff)
    @Bean
    public Retry geminiRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(2) // 최대 2회 재시도
                .intervalFunction(io.github.resilience4j.core.IntervalFunction.ofExponentialBackoff(2000, 2)) // 지수 백오프 (2초 * 2^n)
                .retryExceptions(Exception.class)
                .build();

        Retry retry = Retry.of("geminiRetry", config);

        retry.getEventPublisher()
                .onRetry(event -> log.warn("[Gemini Retry] Attempt #{}: {}",
                        event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()))
                .onSuccess(event -> log.info("[Gemini Retry] Success after {} attempts",
                        event.getNumberOfRetryAttempts()));

        return retry;
    }
}