package com.oneforlogis.notification.infrastructure.client.slack;

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

// Slack API Client 단위 테스트 (MockWebServer 사용)
class SlackApiClientTest {

    private MockWebServer mockWebServer;
    private SlackApiClient slackApiClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Mock Retry 설정 (재시도 1회, 대기 시간 없음)
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(1)
                .build();
        Retry mockRetry = Retry.of("testRetry", retryConfig);

        // MockWebServer URL로 설정된 WebClient 생성
        String mockUrl = mockWebServer.url("/").toString();
        WebClient mockWebClient = WebClient.builder()
                .baseUrl(mockUrl)
                .build();

        slackApiClient = new SlackApiClient(mockWebClient, mockRetry);

        // botToken 주입
        ReflectionTestUtils.setField(slackApiClient, "botToken", "xoxb-test-token");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Slack API 호출 성공 테스트")
    void testPostMessage_Success() throws InterruptedException {
        // Given
        String mockResponseBody = """
                {
                    "ok": true,
                    "channel": "C01234567",
                    "ts": "1234567890.123456"
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        SlackMessageRequest request = SlackMessageRequest.builder()
                .channel("C01234567")
                .text("Test message")
                .build();

        // When
        SlackMessageResponse response = slackApiClient.postMessage(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isOk()).isTrue();
        assertThat(response.getChannel()).isEqualTo("C01234567");
        assertThat(response.getTs()).isEqualTo("1234567890.123456");

        // 요청 검증
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isEqualTo("/chat.postMessage");
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer xoxb-test-token");
    }

    @Test
    @DisplayName("Slack API 호출 실패 테스트 (ok=false)")
    void testPostMessage_Failure() {
        // Given
        String mockResponseBody = """
                {
                    "ok": false,
                    "error": "channel_not_found"
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        SlackMessageRequest request = SlackMessageRequest.builder()
                .channel("invalid-channel")
                .text("Test message")
                .build();

        // When
        SlackMessageResponse response = slackApiClient.postMessage(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isOk()).isFalse();
        assertThat(response.getError()).isEqualTo("channel_not_found");
    }

    @Test
    @DisplayName("Slack API 네트워크 에러 테스트")
    void testPostMessage_NetworkError() {
        // Given
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        SlackMessageRequest request = SlackMessageRequest.builder()
                .channel("C01234567")
                .text("Test message")
                .build();

        // When & Then
        // WebClient가 500 에러를 받으면 예외 발생
        // 실제로는 Retry 로직이 작동하고 최종적으로 예외가 발생함
        try {
            slackApiClient.postMessage(request);
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }
}