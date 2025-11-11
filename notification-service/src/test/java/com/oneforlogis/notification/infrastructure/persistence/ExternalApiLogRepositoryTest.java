package com.oneforlogis.notification.infrastructure.persistence;

import com.oneforlogis.notification.domain.model.ApiProvider;
import com.oneforlogis.notification.domain.model.ExternalApiLog;
import com.oneforlogis.notification.domain.repository.ExternalApiLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

// ExternalApiLogRepository 통합 테스트
@DataJpaTest
@Import({ExternalApiLogRepositoryImpl.class, com.oneforlogis.notification.config.TestJpaConfig.class})
@ActiveProfiles("test")
class ExternalApiLogRepositoryTest {

    @Autowired
    private ExternalApiLogRepository externalApiLogRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    @DisplayName("API 로그 저장 및 조회 테스트")
    void saveAndFindApiLog() {
        // Given
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("channel", "C12345");
        requestData.put("text", "테스트 메시지");

        ExternalApiLog log = ExternalApiLog.builder()
                .apiProvider(ApiProvider.SLACK)
                .apiMethod("chat.postMessage")
                .requestData(requestData)
                .messageId(UUID.randomUUID())
                .build();

        // When
        ExternalApiLog saved = externalApiLogRepository.save(log);
        Optional<ExternalApiLog> found = externalApiLogRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getApiProvider()).isEqualTo(ApiProvider.SLACK);
        assertThat(found.get().getApiMethod()).isEqualTo("chat.postMessage");
        assertThat(found.get().getIsSuccess()).isFalse(); // 기본값
    }

    @Test
    @DisplayName("API 호출 성공 기록 테스트")
    void recordApiSuccess() {
        // Given
        ExternalApiLog log = createApiLog(ApiProvider.GEMINI, "completions");
        ExternalApiLog saved = externalApiLogRepository.save(log);

        // When
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("choices", List.of("응답 내용"));
        saved.recordSuccess(responseData, 200, 1500L);
        externalApiLogRepository.save(saved);

        // Then
        Optional<ExternalApiLog> found = externalApiLogRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getIsSuccess()).isTrue();
        assertThat(found.get().getHttpStatus()).isEqualTo(200);
        assertThat(found.get().getDurationMs()).isEqualTo(1500L);
        assertThat(found.get().getResponseData()).isNotNull();
    }

    @Test
    @DisplayName("API 호출 실패 기록 테스트")
    void recordApiFailure() {
        // Given
        ExternalApiLog log = createApiLog(ApiProvider.SLACK, "chat.postMessage");
        ExternalApiLog saved = externalApiLogRepository.save(log);

        // When
        saved.recordFailure("RATE_LIMITED", "Too many requests", 429, 500L);
        externalApiLogRepository.save(saved);

        // Then
        Optional<ExternalApiLog> found = externalApiLogRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getIsSuccess()).isFalse();
        assertThat(found.get().getErrorCode()).isEqualTo("RATE_LIMITED");
        assertThat(found.get().getErrorMessage()).isEqualTo("Too many requests");
        assertThat(found.get().getHttpStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("API 제공자별 로그 조회 테스트")
    void findByApiProvider() {
        // Given
        ExternalApiLog slackLog = createApiLog(ApiProvider.SLACK, "chat.postMessage");
        ExternalApiLog chatGptLog = createApiLog(ApiProvider.GEMINI, "completions");
        externalApiLogRepository.save(slackLog);
        externalApiLogRepository.save(chatGptLog);

        // When
        List<ExternalApiLog> slackLogs = externalApiLogRepository.findByApiProvider(ApiProvider.SLACK);

        // Then
        assertThat(slackLogs).isNotEmpty();
        assertThat(slackLogs).allMatch(log -> log.getApiProvider() == ApiProvider.SLACK);
    }

    @Test
    @DisplayName("성공 여부로 로그 조회 테스트")
    void findByIsSuccess() {
        // Given
        ExternalApiLog successLog = createApiLog(ApiProvider.SLACK, "chat.postMessage");
        successLog.recordSuccess(Map.of("ok", true), 200, 1000L);

        ExternalApiLog failLog = createApiLog(ApiProvider.GEMINI, "completions");
        failLog.recordFailure("ERROR", "Failed", 500, 2000L);

        externalApiLogRepository.save(successLog);
        externalApiLogRepository.save(failLog);

        // When
        List<ExternalApiLog> successLogs = externalApiLogRepository.findByIsSuccess(true);
        List<ExternalApiLog> failLogs = externalApiLogRepository.findByIsSuccess(false);

        // Then
        assertThat(successLogs).isNotEmpty();
        assertThat(failLogs).isNotEmpty();
    }

    @Test
    @DisplayName("메시지 ID로 관련 API 로그 조회 테스트")
    void findByMessageId() {
        // Given
        UUID messageId = UUID.randomUUID();
        ExternalApiLog log1 = createApiLogWithMessageId(ApiProvider.SLACK, messageId);
        ExternalApiLog log2 = createApiLogWithMessageId(ApiProvider.GEMINI, messageId);
        externalApiLogRepository.save(log1);
        externalApiLogRepository.save(log2);

        // When
        List<ExternalApiLog> logs = externalApiLogRepository.findByMessageId(messageId);

        // Then
        assertThat(logs).hasSize(2);
        assertThat(logs).allMatch(log -> log.getMessageId().equals(messageId));
    }

    @Test
    @DisplayName("특정 기간 내 API 로그 조회 테스트")
    void findByCalledAtBetween() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);

        ExternalApiLog log = createApiLog(ApiProvider.NAVER_MAPS, "directions5");
        externalApiLogRepository.save(log);

        // When
        List<ExternalApiLog> logs = externalApiLogRepository.findByCalledAtBetween(start, end);

        // Then
        assertThat(logs).isNotEmpty();
    }

    @Test
    @DisplayName("API 제공자 및 성공 여부로 로그 조회 테스트")
    void findByApiProviderAndIsSuccess() {
        // Given
        ExternalApiLog successLog = createApiLog(ApiProvider.SLACK, "chat.postMessage");
        successLog.recordSuccess(Map.of(), 200, 1000L);
        externalApiLogRepository.save(successLog);

        // When
        List<ExternalApiLog> logs = externalApiLogRepository.findByApiProviderAndIsSuccess(
                ApiProvider.SLACK, true
        );

        // Then
        assertThat(logs).isNotEmpty();
        assertThat(logs).allMatch(log ->
                log.getApiProvider() == ApiProvider.SLACK && log.getIsSuccess()
        );
    }

    @Test
    @DisplayName("특정 기간 내 API 제공자별 로그 조회 테스트")
    void findByApiProviderAndCalledAtBetween() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);

        ExternalApiLog log = createApiLog(ApiProvider.GEMINI, "completions");
        externalApiLogRepository.save(log);

        // When
        List<ExternalApiLog> logs = externalApiLogRepository.findByApiProviderAndCalledAtBetween(
                ApiProvider.GEMINI, start, end
        );

        // Then
        assertThat(logs).isNotEmpty();
        assertThat(logs).allMatch(l -> l.getApiProvider() == ApiProvider.GEMINI);
    }

    @Test
    @DisplayName("API 호출 비용 설정 테스트")
    void setApiCost() {
        // Given
        ExternalApiLog log = createApiLog(ApiProvider.GEMINI, "completions");
        ExternalApiLog saved = externalApiLogRepository.save(log);

        // When
        BigDecimal cost = new BigDecimal("0.0025");
        saved.setCost(cost);
        externalApiLogRepository.save(saved);

        // Then
        Optional<ExternalApiLog> found = externalApiLogRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getCost()).isEqualByComparingTo(cost);
    }

    @Test
    @DisplayName("JSONB 필드 저장 및 조회 테스트")
    void saveAndRetrieveJsonbData() {
        // Given
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("model", "gpt-4");
        requestData.put("messages", List.of(
                Map.of("role", "user", "content", "테스트 질문")
        ));

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("choices", List.of(
                Map.of("message", Map.of("content", "테스트 응답"))
        ));

        ExternalApiLog log = ExternalApiLog.builder()
                .apiProvider(ApiProvider.GEMINI)
                .apiMethod("completions")
                .requestData(requestData)
                .build();

        log.recordSuccess(responseData, 200, 2000L);

        // When
        ExternalApiLog saved = externalApiLogRepository.save(log);

        // Then
        Optional<ExternalApiLog> found = externalApiLogRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getRequestData()).containsKey("model");
        assertThat(found.get().getResponseData()).containsKey("choices");
    }

    // Helper methods
    private ExternalApiLog createApiLog(ApiProvider provider, String method) {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("test", "data");

        return ExternalApiLog.builder()
                .apiProvider(provider)
                .apiMethod(method)
                .requestData(requestData)
                .build();
    }

    private ExternalApiLog createApiLogWithMessageId(ApiProvider provider, UUID messageId) {
        return ExternalApiLog.builder()
                .apiProvider(provider)
                .apiMethod("test.method")
                .requestData(Map.of("test", "data"))
                .messageId(messageId)
                .build();
    }
}