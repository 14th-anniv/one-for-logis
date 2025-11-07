package com.oneforlogis.notification.infrastructure.client.gemini;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// Gemini API 요청 DTO
@Getter
@Builder
public class GeminiRequest {

    private List<Content> contents; // Conversation contents

    @Getter
    @Builder
    public static class Content {
        private List<Part> parts;
    }

    @Getter
    @Builder
    public static class Part {
        private String text; // Text content
    }

    // Helper: Create simple text request
    public static GeminiRequest createTextRequest(String text) {
        return GeminiRequest.builder()
                .contents(List.of(
                        Content.builder()
                                .parts(List.of(
                                        Part.builder()
                                                .text(text)
                                                .build()
                                ))
                                .build()
                ))
                .build();
    }
}