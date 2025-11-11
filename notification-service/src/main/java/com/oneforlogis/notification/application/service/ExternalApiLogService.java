package com.oneforlogis.notification.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneforlogis.notification.domain.model.ApiProvider;
import com.oneforlogis.notification.domain.model.ExternalApiLog;
import com.oneforlogis.notification.domain.repository.ExternalApiLogRepository;
import com.oneforlogis.notification.presentation.response.ApiStatisticsResponse;
import com.oneforlogis.notification.presentation.response.ExternalApiLogResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

// 외부 API 호출 로그 애플리케이션 서비스
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
     * 페이징 헬퍼 메서드 (팀 표준 - company-service 패턴)
     * - Size 검증: 10, 30, 50만 허용
     * - Page 음수 보정
     * - SortBy 화이트리스트 검증 (보안)
     */
    private Pageable createPageable(int page, int size, String sortBy, boolean isAsc) {
        // Size 검증 (10, 30, 50만 허용)
        int validatedSize = List.of(10, 30, 50).contains(size) ? size : 10;

        // Page 음수 보정
        int validatedPage = Math.max(page, 0);

        // SortBy 화이트리스트 (SQL Injection 방지)
        Set<String> allowedSortFields = Set.of("calledAt", "id", "durationMs");
        String validatedSortBy = allowedSortFields.contains(sortBy) ? sortBy : "calledAt";

        Sort.Direction direction = isAsc
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(
                validatedPage,
                validatedSize,
                Sort.by(direction, validatedSortBy)
        );
    }

    /**
     * 모든 외부 API 로그 조회
     * - MASTER 권한만 조회 가능
     */
    public List<ExternalApiLog> getAllApiLogs() {
        return apiLogRepository.findAll();
    }

    /**
     * 모든 외부 API 로그 페이징 조회
     * - MASTER 권한만 조회 가능
     */
    public Page<ExternalApiLog> getAllApiLogs(Pageable pageable) {
        return apiLogRepository.findAll(pageable);
    }

    /**
     * 모든 외부 API 로그 페이징 조회 (팀 표준 패턴)
     * - 헬퍼 메서드 사용
     */
    public Page<ExternalApiLogResponse> getAllApiLogs(
            int page, int size, String sortBy, boolean isAsc) {
        Pageable pageable = createPageable(page, size, sortBy, isAsc);
        Page<ExternalApiLog> logs = apiLogRepository.findAll(pageable);
        return logs.map(ExternalApiLogResponse::from);
    }

    /**
     * API 제공자별 로그 조회
     */
    public List<ExternalApiLog> getApiLogsByProvider(ApiProvider provider) {
        return apiLogRepository.findByApiProvider(provider);
    }

    /**
     * API 제공자별 로그 페이징 조회
     */
    public Page<ExternalApiLog> getApiLogsByProvider(ApiProvider provider, Pageable pageable) {
        return apiLogRepository.findByApiProvider(provider, pageable);
    }

    /**
     * API 제공자별 로그 페이징 조회 (팀 표준 패턴)
     * - 헬퍼 메서드 사용
     */
    public Page<ExternalApiLogResponse> getApiLogsByProvider(
            ApiProvider provider, int page, int size, String sortBy, boolean isAsc) {
        Pageable pageable = createPageable(page, size, sortBy, isAsc);
        Page<ExternalApiLog> logs = apiLogRepository.findByApiProvider(provider, pageable);
        return logs.map(ExternalApiLogResponse::from);
    }

    /**
     * 메시지 ID로 로그 조회
     */
    public List<ExternalApiLog> getApiLogsByMessageId(UUID messageId) {
        return apiLogRepository.findByMessageId(messageId);
    }

    /**
     * 메시지 ID로 로그 페이징 조회
     */
    public Page<ExternalApiLog> getApiLogsByMessageId(UUID messageId, Pageable pageable) {
        return apiLogRepository.findByMessageId(messageId, pageable);
    }

    /**
     * 메시지 ID로 로그 페이징 조회 (팀 표준 패턴)
     * - 헬퍼 메서드 사용
     */
    public Page<ExternalApiLogResponse> getApiLogsByMessageId(
            UUID messageId, int page, int size, String sortBy, boolean isAsc) {
        Pageable pageable = createPageable(page, size, sortBy, isAsc);
        Page<ExternalApiLog> logs = apiLogRepository.findByMessageId(messageId, pageable);
        return logs.map(ExternalApiLogResponse::from);
    }

    /**
     * API 제공자별 통계 계산
     */
    public Map<ApiProvider, ApiStatisticsResponse> getApiStatistics() {
        List<ExternalApiLog> allLogs = apiLogRepository.findAll();

        return Arrays.stream(ApiProvider.values())
                .collect(Collectors.toMap(
                        provider -> provider,
                        provider -> calculateStatisticsForProvider(allLogs, provider)
                ));
    }

    /**
     * 특정 API 제공자의 통계 계산
     */
    private ApiStatisticsResponse calculateStatisticsForProvider(
            List<ExternalApiLog> allLogs,
            ApiProvider provider
    ) {
        List<ExternalApiLog> providerLogs = allLogs.stream()
                .filter(log -> log.getApiProvider() == provider)
                .toList();

        if (providerLogs.isEmpty()) {
            return ApiStatisticsResponse.of(
                    provider, 0, 0, 0, 0.0, 0, 0, BigDecimal.ZERO
            );
        }

        long totalCalls = providerLogs.size();
        long successCalls = providerLogs.stream().filter(ExternalApiLog::getIsSuccess).count();
        long failedCalls = totalCalls - successCalls;

        double avgResponseTime = providerLogs.stream()
                .mapToLong(ExternalApiLog::getDurationMs)
                .average()
                .orElse(0.0);

        long minResponseTime = providerLogs.stream()
                .mapToLong(ExternalApiLog::getDurationMs)
                .min()
                .orElse(0);

        long maxResponseTime = providerLogs.stream()
                .mapToLong(ExternalApiLog::getDurationMs)
                .max()
                .orElse(0);

        BigDecimal totalCost = providerLogs.stream()
                .map(log -> log.getCost() != null ? log.getCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ApiStatisticsResponse.of(
                provider,
                totalCalls,
                successCalls,
                failedCalls,
                avgResponseTime,
                minResponseTime,
                maxResponseTime,
                totalCost
        );
    }
}