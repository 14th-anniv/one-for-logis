package com.oneforlogis.notification.domain.model;

/**
 * 외부 API 제공자
 */
public enum ApiProvider {
    /**
     * Slack API (chat.postMessage)
     */
    SLACK,

    /**
     * Google Gemini API (출발시간 계산, TSP 경로 최적화)
     */
    GEMINI,

    /**
     * Naver Maps Directions 5 API (경로 계산)
     */
    NAVER_MAPS
}
