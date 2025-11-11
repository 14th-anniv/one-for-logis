package com.oneforlogis.notification.infrastructure.persistence;

import com.oneforlogis.notification.domain.model.ApiProvider;
import com.oneforlogis.notification.domain.model.ExternalApiLog;
import com.oneforlogis.notification.domain.repository.ExternalApiLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// ExternalApiLogRepository 구현체
// JPA Repository를 래핑하여 도메인 레이어의 인터페이스 구현
@Repository
@RequiredArgsConstructor
public class ExternalApiLogRepositoryImpl implements ExternalApiLogRepository {

    private final ExternalApiLogJpaRepository jpaRepository;

    @Override
    public ExternalApiLog save(ExternalApiLog log) {
        return jpaRepository.save(log);
    }

    @Override
    public Optional<ExternalApiLog> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<ExternalApiLog> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public Page<ExternalApiLog> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }

    @Override
    public List<ExternalApiLog> findByApiProvider(ApiProvider apiProvider) {
        return jpaRepository.findByApiProvider(apiProvider);
    }

    @Override
    public Page<ExternalApiLog> findByApiProvider(ApiProvider apiProvider, Pageable pageable) {
        return jpaRepository.findByApiProvider(apiProvider, pageable);
    }

    @Override
    public List<ExternalApiLog> findByIsSuccess(Boolean isSuccess) {
        return jpaRepository.findByIsSuccess(isSuccess);
    }

    @Override
    public List<ExternalApiLog> findByMessageId(UUID messageId) {
        return jpaRepository.findByMessageId(messageId);
    }

    @Override
    public Page<ExternalApiLog> findByMessageId(UUID messageId, Pageable pageable) {
        return jpaRepository.findByMessageId(messageId, pageable);
    }

    @Override
    public List<ExternalApiLog> findByCalledAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return jpaRepository.findByCalledAtBetween(startDate, endDate);
    }

    @Override
    public List<ExternalApiLog> findByApiProviderAndIsSuccess(ApiProvider apiProvider, Boolean isSuccess) {
        return jpaRepository.findByApiProviderAndIsSuccess(apiProvider, isSuccess);
    }

    @Override
    public List<ExternalApiLog> findByApiProviderAndCalledAtBetween(
            ApiProvider apiProvider,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        return jpaRepository.findByApiProviderAndCalledAtBetween(apiProvider, startDate, endDate);
    }
}
