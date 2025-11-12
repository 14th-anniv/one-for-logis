package com.oneforlogis.notification.infrastructure.client.gemini;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

// Gemini API Client 단위 테스트 (MockWebServer 사용)
class GeminiApiClientTest {

    private MockWebServer mockWebServer;
    private GeminiApiClient geminiApiClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Mock Retry 설정
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(1)
                .build();
        Retry mockRetry = Retry.of("testRetry", retryConfig);

        // MockWebServer URL로 설정된 WebClient 생성
        String mockUrl = mockWebServer.url("/").toString();
        WebClient mockWebClient = WebClient.builder()
                .baseUrl(mockUrl)
                .build();

        geminiApiClient = new GeminiApiClient(mockWebClient, mockRetry);

        // API Key 주입
        ReflectionTestUtils.setField(geminiApiClient, "apiKey", "test-api-key");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Gemini API 호출 성공 테스트")
    void testGenerateContent_Success() throws InterruptedException {
        // Given
        String mockResponseBody = """
                {
                    "candidates": [{
                        "content": {
                            "parts": [{
                                "text": "최종 발송 시한: 2025-11-05 15:00"
                            }],
                            "role": "model"
                        },
                        "finishReason": "STOP",
                        "index": 0,
                        "safetyRatings": []
                    }]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        GeminiRequest request = GeminiRequest.createTextRequest("주문 정보를 기반으로 최종 발송 시한을 계산해주세요.");

        // When
        GeminiResponse response = geminiApiClient.generateContent(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCandidates()).hasSize(1);
        assertThat(response.getContent()).contains("최종 발송 시한");

        // 요청 검증
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isEqualTo("/models/gemini-2.5-flash-lite:generateContent");
        assertThat(recordedRequest.getHeader("x-goog-api-key")).isEqualTo("test-api-key");
        assertThat(recordedRequest.getHeader("Content-Type")).isEqualTo("application/json");
    }

    @Test
    @DisplayName("Gemini API 빈 응답 테스트")
    void testGenerateContent_EmptyResponse() {
        // Given
        String mockResponseBody = """
                {
                    "candidates": []
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        GeminiRequest request = GeminiRequest.createTextRequest("Test");

        // When
        GeminiResponse response = geminiApiClient.generateContent(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isNull();
    }

    @Test
    @DisplayName("Gemini API 네트워크 에러 테스트")
    void testGenerateContent_NetworkError() {
        // Given
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        GeminiRequest request = GeminiRequest.createTextRequest("Test");

        // When & Then
        try {
            geminiApiClient.generateContent(request);
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }
}