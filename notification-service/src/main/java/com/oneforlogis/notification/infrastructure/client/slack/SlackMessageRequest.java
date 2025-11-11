package com.oneforlogis.notification.infrastructure.client.slack;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

// Slack chat.postMessage API 요청 DTO
@Getter
@Builder
public class SlackMessageRequest {

    private String channel; // Slack channel ID or user ID

    private String text; // Message content

    @JsonProperty("username")
    private String username; // Bot display name (optional)

    @JsonProperty("icon_emoji")
    private String iconEmoji; // Bot icon emoji (optional, e.g., ":robot_face:")
}