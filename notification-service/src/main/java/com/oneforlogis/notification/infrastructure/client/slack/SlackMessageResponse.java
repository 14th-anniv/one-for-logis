package com.oneforlogis.notification.infrastructure.client.slack;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Slack chat.postMessage API 응답 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlackMessageResponse {

    private boolean ok; // Success indicator

    private String channel; // Channel where message was posted

    private String ts; // Message timestamp (unique message ID)

    private String error; // Error code if ok=false

    @JsonProperty("warning")
    private String warning; // Warning message (optional)
}