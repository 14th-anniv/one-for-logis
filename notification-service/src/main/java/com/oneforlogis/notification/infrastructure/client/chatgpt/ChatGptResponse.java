package com.oneforlogis.notification.infrastructure.client.chatgpt;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// ChatGPT API 응답 DTO
@Getter
@NoArgsConstructor
public class ChatGptResponse {

    private String id; // Unique completion ID

    private String object; // "chat.completion"

    private Long created; // Unix timestamp

    private String model; // Model used

    private List<Choice> choices; // Response choices

    private Usage usage; // Token usage statistics

    @Getter
    @NoArgsConstructor
    public static class Choice {
        private Integer index;

        private Message message;

        @JsonProperty("finish_reason")
        private String finishReason; // "stop", "length", "content_filter"
    }

    @Getter
    @NoArgsConstructor
    public static class Message {
        private String role; // "assistant"

        private String content; // Generated text
    }

    @Getter
    @NoArgsConstructor
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }

    // Helper: Get first choice content
    public String getContent() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        return choices.get(0).getMessage().getContent();
    }
}