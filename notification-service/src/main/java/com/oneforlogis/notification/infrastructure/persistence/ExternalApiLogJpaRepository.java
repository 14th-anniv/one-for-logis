package com.oneforlogis.notification.infrastructure.persistence;

import com.oneforlogis.notification.domain.model.ApiProvider;
import com.oneforlogis.notification.domain.model.ExternalApiLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * ExternalApiLog JPA Repository
 * Spring Data JPA가 자동으로 구현체 생성
 */
public interface ExternalApiLogJpaRepository extends JpaRepository<ExternalApiLog, UUID> {

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
