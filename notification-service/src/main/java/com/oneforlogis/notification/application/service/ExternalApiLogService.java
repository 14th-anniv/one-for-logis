package com.oneforlogis.notification.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneforlogis.notification.domain.model.ApiProvider;
import com.oneforlogis.notification.domain.model.ExternalApiLog;
import com.oneforlogis.notification.domain.repository.ExternalApiLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

// 외부 API 호출 로그 애플리케이션 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalApiLogService {

    private final ExternalApiLogRepository apiLogRepository;
    private final ObjectMapper objectMapper;

    // API 호출 로그 생성 및 저장
    @Transactional
    public ExternalApiLog logApiCall(
            ApiProvider provider,
            String apiMethod,
            Object requestData,
            Object responseData,
            Integer httpStatus,
            boolean isSuccess,
            String errorCode,
            String errorMessage,
            long durationMs,
            BigDecimal cost,
            UUID messageId
    ) {
        try {
            // 민감 정보 마스킹
            Map<String, Object> maskedRequest = maskSensitiveInfo(requestData);
            Map<String, Object> maskedResponse = maskSensitiveInfo(responseData);

            ExternalApiLog log = ExternalApiLog.builder()
                    .apiProvider(provider)
                    .apiMethod(apiMethod)
                    .requestData(maskedRequest)
                    .responseData(maskedResponse)
                    .httpStatus(httpStatus)
                    .isSuccess(isSuccess)
                    .errorCode(errorCode)
                    .errorMessage(errorMessage)
                    .durationMs(durationMs)
                    .cost(cost)
                    .calledAt(LocalDateTime.now())
                    .messageId(messageId)
                    .build();

            return apiLogRepository.save(log);

        } catch (Exception e) {
            log.error("[ExternalApiLogService] Failed to log API call: {}", e.getMessage());
            // 로깅 실패해도 예외를 던지지 않음 (비즈니스 로직에 영향 없도록)
            return null;
        }
    }

    // 민감 정보 마스킹 (API Key, Token 등)
    @SuppressWarnings("unchecked")
    private Map<String, Object> maskSensitiveInfo(Object data) throws JsonProcessingException {
        if (data == null) {
            return null;
        }

        String json = objectMapper.writeValueAsString(data);

        // API 키 마스킹 (예: "sk-abc123..." → "sk-****")
        json = json.replaceAll("(sk-)[\\w\\d]+", "$1****");
        json = json.replaceAll("(xoxb-)[\\w\\d-]+", "$1****");
        json = json.replaceAll("(AIza)[\\w\\d-]+", "$1****"); // Gemini API 키

        // Authorization 헤더 마스킹
        json = json.replaceAll("(\"Authorization\":\\s*\"Bearer\\s+)[^\"]+", "$1****");
        json = json.replaceAll("(\"authorization\":\\s*\"Bearer\\s+)[^\"]+", "$1****");

        // 비밀번호 마스킹
        json = json.replaceAll("(\"password\":\\s*\")[^\"]+", "$1****");

        // JSON을 Map으로 변환하여 반환
        return objectMapper.readValue(json, Map.class);
    }

    /**
     * 모든 외부 API 로그 조회
     * - MASTER 권한만 조회 가능
     */
    @Transactional(readOnly = true)
    public java.util.List<ExternalApiLog> getAllApiLogs() {
        return apiLogRepository.findAll();
    }

    /**
     * API 제공자별 로그 조회
     */
    @Transactional(readOnly = true)
    public java.util.List<ExternalApiLog> getApiLogsByProvider(ApiProvider provider) {
        return apiLogRepository.findByApiProvider(provider);
    }

    /**
     * 메시지 ID로 로그 조회
     */
    @Transactional(readOnly = true)
    public java.util.List<ExternalApiLog> getApiLogsByMessageId(UUID messageId) {
        return apiLogRepository.findByMessageId(messageId);
    }
}