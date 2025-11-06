package com.oneforlogis.notification.infrastructure.client.gemini;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// Gemini API 응답 DTO
@Getter
@NoArgsConstructor
public class GeminiResponse {

    private List<Candidate> candidates;

    private PromptFeedback promptFeedback;

    @Getter
    @NoArgsConstructor
    public static class Candidate {
        private Content content;
        private String finishReason; // "STOP", "MAX_TOKENS", "SAFETY"
        private Integer index;
        private List<SafetyRating> safetyRatings;
    }

    @Getter
    @NoArgsConstructor
    public static class Content {
        private List<Part> parts;
        private String role; // "model"
    }

    @Getter
    @NoArgsConstructor
    public static class Part {
        private String text;
    }

    @Getter
    @NoArgsConstructor
    public static class SafetyRating {
        private String category;
        private String probability;
    }

    @Getter
    @NoArgsConstructor
    public static class PromptFeedback {
        private List<SafetyRating> safetyRatings;
    }

    // Helper: Get first candidate text
    public String getContent() {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        Content content = candidates.get(0).getContent();
        if (content == null || content.getParts() == null || content.getParts().isEmpty()) {
            return null;
        }

        return content.getParts().get(0).getText();
    }
}