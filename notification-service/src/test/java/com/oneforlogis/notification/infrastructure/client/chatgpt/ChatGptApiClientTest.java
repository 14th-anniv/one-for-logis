package com.oneforlogis.notification.infrastructure.client.chatgpt;

/*
 * NOTE: ChatGPT는 현재 사용하지 않고 Gemini API로 대체되었습니다.
 * 향후 필요시 참고용으로 주석처리하여 보관합니다.
 */

/*
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ChatGPT API Client 단위 테스트 (MockWebServer 사용)
class ChatGptApiClientTest {

    private MockWebServer mockWebServer;
    private ChatGptApiClient chatGptApiClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Mock Retry 설정
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(1)
                .build();
        Retry mockRetry = Retry.of("testRetry", retryConfig);

        // ChatGptApiClient 생성
        WebClient.Builder webClientBuilder = WebClient.builder();
        chatGptApiClient = new ChatGptApiClient(webClientBuilder, mockRetry);

        // API Key 주입
        ReflectionTestUtils.setField(chatGptApiClient, "apiKey", "sk-test-key");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("ChatGPT API 호출 성공 테스트")
    void testGenerateCompletion_Success() throws InterruptedException {
        // Given
        String mockResponseBody = """
                {
                    "id": "chatcmpl-123",
                    "object": "chat.completion",
                    "created": 1677652288,
                    "model": "gpt-3.5-turbo",
                    "choices": [{
                        "index": 0,
                        "message": {
                            "role": "assistant",
                            "content": "Hello! How can I help you?"
                        },
                        "finish_reason": "stop"
                    }],
                    "usage": {
                        "prompt_tokens": 10,
                        "completion_tokens": 20,
                        "total_tokens": 30
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json"));

        ChatGptRequest request = ChatGptRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(List.of(
                        ChatGptRequest.Message.builder()
                                .role("user")
                                .content("Hello")
                                .build()
                ))
                .build();

        // When
        ChatGptResponse response = chatGptApiClient.generateCompletion(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("chatcmpl-123");
        assertThat(response.getModel()).isEqualTo("gpt-3.5-turbo");
        assertThat(response.getContent()).isEqualTo("Hello! How can I help you?");
        assertThat(response.getUsage()).isNotNull();
        assertThat(response.getUsage().getTotalTokens()).isEqualTo(30);

        // 요청 검증
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isEqualTo("/chat/completions");
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer sk-test-key");
    }

    @Test
    @DisplayName("ChatGPT API 네트워크 에러 테스트")
    void testGenerateCompletion_NetworkError() {
        // Given
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        ChatGptRequest request = ChatGptRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(List.of(
                        ChatGptRequest.Message.builder()
                                .role("user")
                                .content("Hello")
                                .build()
                ))
                .build();

        // When & Then
        try {
            chatGptApiClient.generateCompletion(request);
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }
}
*/