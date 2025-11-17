package com.oneforlogis.notification.infrastructure.client.chatgpt;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// ChatGPT API 요청 DTO
@Getter
@Builder
public class ChatGptRequest {

    private String model; // e.g., "gpt-3.5-turbo", "gpt-4"

    private List<Message> messages; // Conversation messages

    @Builder.Default
    private Double temperature = 0.7; // Randomness (0.0-2.0)

    @Builder.Default
    private Integer maxTokens = 1000; // Max response tokens

    @Getter
    @Builder
    public static class Message {
        private String role; // "system", "user", or "assistant"
        private String content; // Message content
    }
}