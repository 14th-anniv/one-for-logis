package com.oneforlogis.notification.presentation.controller;

import com.oneforlogis.common.api.ApiResponse;
import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.common.security.UserPrincipal;
import com.oneforlogis.notification.application.service.ExternalApiLogService;
import com.oneforlogis.notification.application.service.NotificationService;
import com.oneforlogis.notification.domain.model.ApiProvider;
import com.oneforlogis.notification.domain.model.ExternalApiLog;
import com.oneforlogis.notification.domain.model.MessageStatus;
import com.oneforlogis.notification.domain.model.MessageType;
import com.oneforlogis.notification.infrastructure.client.user.UserResponse;
import com.oneforlogis.notification.infrastructure.client.user.UserServiceClient;
import com.oneforlogis.notification.presentation.request.DeliveryStatusNotificationRequest;
import com.oneforlogis.notification.presentation.request.ManualNotificationRequest;
import com.oneforlogis.notification.presentation.request.OrderNotificationRequest;
import com.oneforlogis.notification.presentation.response.ApiStatisticsResponse;
import com.oneforlogis.notification.presentation.response.ExternalApiLogResponse;
import com.oneforlogis.notification.presentation.response.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@Tag(name = "Notifications", description = "알림 관리 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final ExternalApiLogService externalApiLogService;
    private final UserServiceClient userServiceClient;

    /**
     * 주문 알림 발송 (order-service에서 호출)
     * - 내부 서비스 간 통신이므로 권한 체크 없음
     * - Gemini AI로 발송 시한 계산 후 Slack 메시지 발송
     */
    @Operation(
            summary = "주문 알림 발송 (내부 API)",
            description = "주문 생성 시 order-service에서 호출. Gemini AI로 최종 발송 시한을 계산하고 Slack 메시지를 발송합니다."
    )
    @PostMapping("/order")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NotificationResponse> sendOrderNotification(
            @Valid @RequestBody OrderNotificationRequest request
    ) {
        log.info("[NotificationController] POST /api/v1/notifications/order - orderId: {}", request.orderId());
        NotificationResponse response = notificationService.sendOrderNotification(request);
        return ApiResponse.created(response);
    }

    /**
     * 수동 메시지 발송 (인증된 사용자가 직접 호출)
     * - 모든 인증된 사용자 가능
     * - 발신자 정보를 스냅샷으로 저장
     * - user-service의 마이페이지 API로 최신 사용자 정보 조회
     */
    @Operation(
            summary = "수동 메시지 발송",
            description = "인증된 사용자가 직접 Slack 메시지를 발송합니다. 발신자 정보는 스냅샷으로 저장됩니다."
    )
    @PreAuthorize("hasAnyRole('MASTER', 'HUB_MANAGER', 'DELIVERY_MANAGER', 'COMPANY_MANAGER')")
    @PostMapping("/manual")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NotificationResponse> sendManualNotification(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ManualNotificationRequest request
    ) {
        log.info("[NotificationController] POST /api/v1/notifications/manual - from: {}, to: {}",
                userPrincipal.username(), request.recipientSlackId());

        // user-service 마이페이지 API로 발신자 정보 조회 (최신 정보 보장)
        ApiResponse<UserResponse> userApiResponse = userServiceClient.getMyInfo(userPrincipal.id());

        if (userApiResponse == null || userApiResponse.data() == null) {
            log.error("[NotificationController] user-service 응답이 null입니다 - userId: {}", userPrincipal.id());
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        UserResponse userResponse = userApiResponse.data();

        NotificationResponse response = notificationService.sendManualNotification(
                request,
                userPrincipal.username(),
                userResponse.getSlackId(),
                userResponse.getName()
        );

        return ApiResponse.created(response);
    }

    /**
     * 배송 상태 변경 알림 발송 (delivery-service에서 호출 또는 재발송용)
     * - 모든 인증된 사용자 가능
     * - Kafka 이벤트와 동일한 형식의 알림 발송
     */
    @Operation(
            summary = "배송 상태 변경 알림 발송",
            description = "배송 상태가 변경될 때 Slack 알림을 발송합니다. delivery-service에서 호출하거나 수동 재발송 시 사용합니다."
    )
    @PreAuthorize("hasAnyRole('MASTER', 'HUB_MANAGER', 'DELIVERY_MANAGER', 'COMPANY_MANAGER')")
    @PostMapping("/delivery-status")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NotificationResponse> sendDeliveryStatusNotification(
            @Valid @RequestBody DeliveryStatusNotificationRequest request
    ) {
        log.info("[NotificationController] POST /api/v1/notifications/delivery-status - deliveryId: {}, status: {} → {}",
                request.deliveryId(), request.previousStatus(), request.currentStatus());

        NotificationResponse response = notificationService.sendDeliveryStatusNotification(request);
        return ApiResponse.created(response);
    }

    /**
     * 알림 ID로 단일 조회
     * - 모든 인증된 사용자 가능
     */
    @Operation(
            summary = "알림 단일 조회",
            description = "알림 ID로 특정 알림 정보를 조회합니다."
    )
    @PreAuthorize("hasAnyRole('MASTER', 'HUB_MANAGER', 'DELIVERY_MANAGER', 'COMPANY_MANAGER')")
    @GetMapping("/{notificationId}")
    public ApiResponse<NotificationResponse> getNotification(
            @PathVariable UUID notificationId
    ) {
        log.info("[NotificationController] GET /api/v1/notifications/{}", notificationId);
        NotificationResponse response = notificationService.getNotification(notificationId);
        return ApiResponse.success(response);
    }

    /**
     * 알림 목록 조회 (페이징)
     * - MASTER만 전체 조회 가능
     */
    @Operation(
            summary = "알림 목록 조회 (페이징)",
            description = "알림 목록을 페이징 형태로 조회합니다. MASTER 권한 필요. 최신순(createdAt DESC) 정렬."
    )
    @PreAuthorize("hasRole('MASTER')")
    @GetMapping
    public ApiResponse<Page<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "false") boolean isAsc
    ) {
        log.info("[NotificationController] GET /api/v1/notifications - page: {}, size: {}, sortBy: {}, isAsc: {}",
                page, size, sortBy, isAsc);

        Page<NotificationResponse> notifications = notificationService.getNotifications(page, size, sortBy, isAsc);

        return ApiResponse.success(notifications);
    }

    /**
     * 알림 필터링 조회 (페이징)
     * - MASTER만 전체 조회 가능
     * - 발신자, 수신자, 메시지 타입, 상태별 필터링 지원
     */
    @Operation(
            summary = "알림 필터링 조회 (페이징)",
            description = "알림을 필터 조건에 따라 페이징하여 조회합니다. MASTER 권한 필요. 발신자, 수신자, 메시지 타입, 상태별 필터링 지원."
    )
    @PreAuthorize("hasRole('MASTER')")
    @GetMapping("/search")
    public ApiResponse<Page<NotificationResponse>> searchNotifications(
            @RequestParam(required = false) String senderUsername,
            @RequestParam(required = false) String recipientSlackId,
            @RequestParam(required = false) MessageType messageType,
            @RequestParam(required = false) MessageStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "false") boolean isAsc
    ) {
        log.info("[NotificationController] GET /api/v1/notifications/search - senderUsername: {}, recipientSlackId: {}, messageType: {}, status: {}, page: {}, size: {}",
                senderUsername, recipientSlackId, messageType, status, page, size);

        Page<NotificationResponse> notifications = notificationService.searchNotifications(
                senderUsername, recipientSlackId, messageType, status, page, size, sortBy, isAsc
        );

        return ApiResponse.success(notifications);
    }

    /**
     * 외부 API 로그 전체 조회 (페이징)
     * - MASTER만 조회 가능
     */
    @Operation(
            summary = "외부 API 로그 전체 조회 (페이징)",
            description = "외부 API 호출 로그 전체를 페이징하여 조회합니다. MASTER 권한 필요. (Slack, Gemini, Naver Maps API 호출 이력)"
    )
    @PreAuthorize("hasRole('MASTER')")
    @GetMapping("/api-logs")
    public ApiResponse<Page<ExternalApiLogResponse>> getAllApiLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "calledAt") String sortBy,
            @RequestParam(defaultValue = "false") boolean isAsc
    ) {
        log.info("[NotificationController] GET /api/v1/notifications/api-logs - page: {}, size: {}, sortBy: {}, isAsc: {}",
                page, size, sortBy, isAsc);

        Page<ExternalApiLogResponse> responses = externalApiLogService.getAllApiLogs(page, size, sortBy, isAsc);

        return ApiResponse.success(responses);
    }

    /**
     * 외부 API 로그 제공자별 조회 (페이징)
     * - MASTER만 조회 가능
     */
    @Operation(
            summary = "외부 API 로그 제공자별 조회 (페이징)",
            description = "특정 API 제공자(SLACK, GEMINI, NAVER_MAPS)의 호출 로그를 페이징하여 조회합니다. MASTER 권한 필요."
    )
    @PreAuthorize("hasRole('MASTER')")
    @GetMapping("/api-logs/provider/{provider}")
    public ApiResponse<Page<ExternalApiLogResponse>> getApiLogsByProvider(
            @PathVariable ApiProvider provider,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "calledAt") String sortBy,
            @RequestParam(defaultValue = "false") boolean isAsc
    ) {
        log.info("[NotificationController] GET /api/v1/notifications/api-logs/provider/{} - page: {}, size: {}, sortBy: {}, isAsc: {}",
                provider, page, size, sortBy, isAsc);

        Page<ExternalApiLogResponse> responses = externalApiLogService.getApiLogsByProvider(provider, page, size, sortBy, isAsc);

        return ApiResponse.success(responses);
    }

    /**
     * 외부 API 로그 메시지 ID로 조회 (페이징)
     * - MASTER만 조회 가능
     */
    @Operation(
            summary = "외부 API 로그 메시지 ID로 조회 (페이징)",
            description = "특정 메시지와 연관된 외부 API 호출 로그를 페이징하여 조회합니다. MASTER 권한 필요."
    )
    @PreAuthorize("hasRole('MASTER')")
    @GetMapping("/api-logs/message/{messageId}")
    public ApiResponse<Page<ExternalApiLogResponse>> getApiLogsByMessageId(
            @PathVariable UUID messageId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "calledAt") String sortBy,
            @RequestParam(defaultValue = "false") boolean isAsc
    ) {
        log.info("[NotificationController] GET /api/v1/notifications/api-logs/message/{} - page: {}, size: {}, sortBy: {}, isAsc: {}",
                messageId, page, size, sortBy, isAsc);

        Page<ExternalApiLogResponse> responses = externalApiLogService.getApiLogsByMessageId(messageId, page, size, sortBy, isAsc);

        return ApiResponse.success(responses);
    }

    /**
     * API 통계 조회
     * - MASTER만 조회 가능
     * - 제공자별 호출 수, 성공률, 평균 응답 시간, 총 비용 등 통계
     */
    @Operation(
            summary = "API 통계 조회",
            description = "외부 API 호출 통계를 조회합니다. MASTER 권한 필요. 제공자별 호출 수, 성공률, 평균 응답 시간, 총 비용 포함."
    )
    @PreAuthorize("hasRole('MASTER')")
    @GetMapping("/api-logs/stats")
    public ApiResponse<java.util.Map<ApiProvider, ApiStatisticsResponse>> getApiStatistics() {
        log.info("[NotificationController] GET /api/v1/notifications/api-logs/stats");
        java.util.Map<ApiProvider, ApiStatisticsResponse> statistics = externalApiLogService.getApiStatistics();
        return ApiResponse.success(statistics);
    }
}
