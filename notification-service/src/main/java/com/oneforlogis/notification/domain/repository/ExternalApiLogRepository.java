package com.oneforlogis.notification.domain.repository;

import com.oneforlogis.notification.domain.model.ApiProvider;
import com.oneforlogis.notification.domain.model.ExternalApiLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ExternalApiLog 도메인 Repository 인터페이스
 * 인프라스트럭처 레이어에서 구현
 */
public interface ExternalApiLogRepository {

    /**
     * API 로그 저장
     */
    ExternalApiLog save(ExternalApiLog log);

    /**
     * ID로 API 로그 조회
     */
    Optional<ExternalApiLog> findById(UUID id);

    /**
     * 모든 API 로그 조회
     */
    List<ExternalApiLog> findAll();

    /**
     * API 제공자별 로그 조회
     */
    List<ExternalApiLog> findByApiProvider(ApiProvider apiProvider);

    /**
     * 성공 여부로 로그 조회
     */
    List<ExternalApiLog> findByIsSuccess(Boolean isSuccess);

    /**
     * 알림 메시지 ID로 관련 API 로그 조회
     */
    List<ExternalApiLog> findByMessageId(UUID messageId);

    /**
     * 특정 기간 내 API 로그 조회
     */
    List<ExternalApiLog> findByCalledAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * API 제공자 및 성공 여부로 로그 조회
     */
    List<ExternalApiLog> findByApiProviderAndIsSuccess(ApiProvider apiProvider, Boolean isSuccess);

    /**
     * 특정 기간 내 API 제공자별 로그 조회
     */
    List<ExternalApiLog> findByApiProviderAndCalledAtBetween(
            ApiProvider apiProvider,
            LocalDateTime startDate,
            LocalDateTime endDate
    );
}
