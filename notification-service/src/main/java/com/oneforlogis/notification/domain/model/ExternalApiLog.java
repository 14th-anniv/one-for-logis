package com.oneforlogis.notification.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

// 외부 API 호출 로그 엔티티
// Slack, ChatGPT, Naver Maps API 호출 이력 추적
@Entity
@Table(name = "p_external_api_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExternalApiLog {

    @Id
    @Column(name = "log_id")
    private UUID id;

    // API 제공자
    @Enumerated(EnumType.STRING)
    @Column(name = "api_provider", nullable = false, length = 20)
    private ApiProvider apiProvider;

    // API 메서드 (예: chat.postMessage, completions, directions5)
    @Column(name = "api_method", nullable = false, length = 100)
    private String apiMethod;

    // 요청 데이터 (JSONB)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_data", columnDefinition = "TEXT")
    private Map<String, Object> requestData;

    // 응답 데이터 (JSONB)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_data", columnDefinition = "TEXT")
    private Map<String, Object> responseData;

    // HTTP 상태 코드
    @Column(name = "http_status")
    private Integer httpStatus;

    // 성공 여부
    @Column(name = "is_success", nullable = false)
    private Boolean isSuccess;

    // 에러 코드
    @Column(name = "error_code", length = 50)
    private String errorCode;

    // 에러 메시지
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // 응답 시간 (milliseconds)
    @Column(name = "duration_ms")
    private Long durationMs;

    // API 호출 비용 (선택적)
    @Column(name = "cost", precision = 10, scale = 4)
    private BigDecimal cost;

    // API 호출 시각
    @Column(name = "called_at", nullable = false)
    private LocalDateTime calledAt;

    // 연관된 알림 메시지 ID (논리적 FK, nullable)
    @Column(name = "message_id")
    private UUID messageId;

    @Builder
    public ExternalApiLog(
            ApiProvider apiProvider,
            String apiMethod,
            Map<String, Object> requestData,
            UUID messageId
    ) {
        this.id = UUID.randomUUID();
        this.apiProvider = apiProvider;
        this.apiMethod = apiMethod;
        this.requestData = requestData;
        this.messageId = messageId;
        this.calledAt = LocalDateTime.now();
        this.isSuccess = false; // 기본값
    }

    /**
     * API 호출 성공 처리
     */
    public void recordSuccess(
            Map<String, Object> responseData,
            Integer httpStatus,
            Long durationMs
    ) {
        this.isSuccess = true;
        this.responseData = responseData;
        this.httpStatus = httpStatus;
        this.durationMs = durationMs;
        this.errorCode = null;
        this.errorMessage = null;
    }

    /**
     * API 호출 실패 처리
     */
    public void recordFailure(
            String errorCode,
            String errorMessage,
            Integer httpStatus,
            Long durationMs
    ) {
        this.isSuccess = false;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.httpStatus = httpStatus;
        this.durationMs = durationMs;
    }

    /**
     * API 호출 비용 설정
     */
    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    /**
     * 엔티티 저장 전 검증
     */
    @PrePersist
    @PreUpdate
    private void validateEntity() {
        if (apiProvider == null) {
            throw new IllegalStateException("apiProvider는 필수입니다.");
        }
        if (apiMethod == null || apiMethod.isBlank()) {
            throw new IllegalStateException("apiMethod는 필수입니다.");
        }
        if (calledAt == null) {
            throw new IllegalStateException("calledAt은 필수입니다.");
        }
        if (isSuccess == null) {
            throw new IllegalStateException("isSuccess는 필수입니다.");
        }
    }
}
