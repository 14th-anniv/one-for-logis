package com.oneforlogis.notification.application.service;

import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.notification.domain.model.*;
import com.oneforlogis.notification.domain.repository.NotificationRepository;
import com.oneforlogis.notification.infrastructure.client.GeminiClientWrapper;
import com.oneforlogis.notification.infrastructure.client.SlackClientWrapper;
import com.oneforlogis.notification.infrastructure.client.gemini.GeminiRequest;
import com.oneforlogis.notification.infrastructure.client.gemini.GeminiResponse;
import com.oneforlogis.notification.infrastructure.client.slack.SlackMessageRequest;
import com.oneforlogis.notification.infrastructure.client.slack.SlackMessageResponse;
import com.oneforlogis.notification.presentation.request.ManualNotificationRequest;
import com.oneforlogis.notification.presentation.request.OrderNotificationRequest;
import com.oneforlogis.notification.presentation.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SlackClientWrapper slackClientWrapper;
    private final GeminiClientWrapper geminiClientWrapper;

    /**
     * 주문 알림 발송 (order-service REST API에서 호출)
     * - Gemini AI로 최종 발송 시한 계산
     * - Slack 메시지 발송
     * - Notification 엔티티 저장
     */
    @Transactional
    public NotificationResponse sendOrderNotification(OrderNotificationRequest request) {
        return sendOrderNotificationInternal(request, null);
    }

    /**
     * 주문 알림 발송 (Kafka 이벤트에서 호출, 멱등성 보장)
     * - eventId를 Notification에 저장하여 중복 처리 방지
     */
    @Transactional
    public NotificationResponse sendOrderNotificationFromEvent(OrderNotificationRequest request, String eventId) {
        return sendOrderNotificationInternal(request, eventId);
    }

    /**
     * 주문 알림 발송 내부 로직 (공통)
     * Priority 2-1: Gemini messageId 연계를 위해 Notification을 먼저 저장(PENDING) 후 Gemini 호출
     */
    private NotificationResponse sendOrderNotificationInternal(OrderNotificationRequest request, String eventId) {
        log.info("[NotificationService] 주문 알림 발송 시작 - orderId: {}, eventId: {}", request.orderId(), eventId);

        // Step 1: Notification 엔티티 먼저 생성 (PENDING 상태로 저장)
        Notification notification = Notification.builder()
                .senderType(SenderType.SYSTEM)
                .senderUsername(null)
                .senderSlackId(null)
                .senderName(null)
                .recipientSlackId(request.recipientSlackId())
                .recipientName(request.recipientName())
                .messageContent("Processing...")  // 임시 메시지
                .messageType(MessageType.ORDER_NOTIFICATION)
                .referenceId(request.orderId())
                .eventId(eventId)  // Kafka 이벤트인 경우에만 eventId 저장
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        log.info("[NotificationService] Notification 저장 완료 - notificationId: {} (PENDING)", savedNotification.getId());

        // Step 2: Gemini AI로 최종 발송 시한 계산 (notificationId 전달)
        String aiGeneratedDeadline = calculateDepartureDeadline(request, savedNotification.getId());

        // Step 3: Slack 메시지 생성 및 Notification 업데이트
        String slackMessage = buildOrderNotificationMessage(request, aiGeneratedDeadline);
        savedNotification.updateMessageContent(slackMessage);

        // Step 4: Slack API 호출
        SlackMessageRequest slackRequest = SlackMessageRequest.builder()
                .channel(request.recipientSlackId())
                .text(slackMessage)
                .build();

        SlackMessageResponse slackResponse = slackClientWrapper.postMessage(slackRequest, savedNotification.getId());

        // Step 5: 발송 상태 업데이트 (실패 시 예외 throw - Priority 1-3)
        if (slackResponse != null && slackResponse.isOk()) {
            savedNotification.markAsSent();
            log.info("[NotificationService] 주문 알림 발송 성공 - notificationId: {}", savedNotification.getId());
            return NotificationResponse.from(savedNotification);
        } else {
            String errorMsg = slackResponse != null ? slackResponse.getError() : "Unknown error";
            savedNotification.markAsFailed(errorMsg);
            log.error("[NotificationService] 주문 알림 발송 실패 - notificationId: {}, error: {}",
                    savedNotification.getId(), errorMsg);
            throw new CustomException(ErrorCode.NOTIFICATION_SEND_FAILED);
        }
    }

    /**
     * 수동 메시지 발송 (인증된 사용자가 직접 호출)
     * - 사용자 정보 스냅샷 저장
     * - Slack 메시지 발송
     */
    @Transactional
    public NotificationResponse sendManualNotification(
            ManualNotificationRequest request,
            String currentUsername,
            String currentUserSlackId,
            String currentUserName
    ) {
        log.info("[NotificationService] 수동 메시지 발송 시작 - from: {}, to: {}",
                currentUsername, request.recipientSlackId());

        // Step 1: Notification 엔티티 생성 (USER 타입)
        Notification notification = Notification.builder()
                .senderType(SenderType.USER)
                .senderUsername(currentUsername)
                .senderSlackId(currentUserSlackId)
                .senderName(currentUserName)
                .recipientSlackId(request.recipientSlackId())
                .recipientName(request.recipientName())
                .messageContent(request.messageContent())
                .messageType(MessageType.MANUAL)
                .referenceId(null)
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        // Step 2: Slack API 호출
        SlackMessageRequest slackRequest = SlackMessageRequest.builder()
                .channel(request.recipientSlackId())
                .text(request.messageContent())
                .build();

        SlackMessageResponse slackResponse = slackClientWrapper.postMessage(slackRequest, savedNotification.getId());

        // Step 3: 발송 상태 업데이트 (실패 시 예외 throw - Priority 1-3)
        if (slackResponse != null && slackResponse.isOk()) {
            savedNotification.markAsSent();
            log.info("[NotificationService] 수동 메시지 발송 성공 - notificationId: {}", savedNotification.getId());
            return NotificationResponse.from(savedNotification);
        } else {
            String errorMsg = slackResponse != null ? slackResponse.getError() : "Unknown error";
            savedNotification.markAsFailed(errorMsg);
            log.error("[NotificationService] 수동 메시지 발송 실패 - notificationId: {}, error: {}",
                    savedNotification.getId(), errorMsg);
            throw new CustomException(ErrorCode.NOTIFICATION_SEND_FAILED);
        }
    }

    /**
     * 알림 목록 조회 (페이징)
     */
    public Page<NotificationResponse> getNotifications(Pageable pageable) {
        Page<Notification> notificationPage = notificationRepository.findAll(pageable);
        return notificationPage.map(NotificationResponse::from);
    }

    /**
     * 알림 ID로 조회
     */
    public NotificationResponse getNotification(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        return NotificationResponse.from(notification);
    }

    /**
     * 알림 페이징 조회 (팀 표준 패턴)
     * - 헬퍼 메서드 사용
     */
    public Page<NotificationResponse> getNotifications(int page, int size, String sortBy, boolean isAsc) {
        Pageable pageable = createPageable(page, size, sortBy, isAsc);
        Page<Notification> notifications = notificationRepository.findAll(pageable);
        return notifications.map(NotificationResponse::from);
    }

    /**
     * Gemini AI를 통한 최종 발송 시한 계산
     * Priority 2-1: notificationId를 전달하여 ExternalApiLog와 Notification 연계
     */
    private String calculateDepartureDeadline(OrderNotificationRequest request, UUID notificationId) {
        String promptText = buildGeminiPrompt(request);

        GeminiRequest geminiRequest = GeminiRequest.createTextRequest(promptText);

        GeminiResponse geminiResponse = geminiClientWrapper.generateContent(geminiRequest, notificationId);

        if (geminiResponse != null && geminiResponse.getContent() != null && !geminiResponse.getContent().isBlank()) {
            return geminiResponse.getContent().trim();
        } else {
            log.warn("[NotificationService] Gemini AI 응답이 비어있습니다. 기본 메시지를 사용합니다.");
            return "AI 계산 실패 - 담당자가 직접 계산 바랍니다.";
        }
    }

    /**
     * Gemini AI 프롬프트 생성
     */
    private String buildGeminiPrompt(OrderNotificationRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 물류 시스템의 배송 시간 계산 전문가입니다.\n\n");
        prompt.append("다음 주문 정보를 바탕으로 **최종 발송 시한**(이 시간까지 출발해야 납기를 맞출 수 있는 마지막 시점)을 계산해주세요.\n\n");
        prompt.append("## 주문 정보\n");
        prompt.append("- 상품: ").append(request.productInfo()).append("\n");
        prompt.append("- 요청사항: ").append(request.requestDetails()).append("\n");
        prompt.append("- 출발지: ").append(request.departureHub()).append("\n");

        if (request.waypoints() != null && !request.waypoints().isEmpty()) {
            prompt.append("- 경유지: ").append(String.join(", ", request.waypoints())).append("\n");
        }

        prompt.append("- 도착지: ").append(request.destinationHub()).append("\n");
        prompt.append("- 최종 배송지: ").append(request.destinationAddress()).append("\n\n");

        prompt.append("## 제약 조건\n");
        prompt.append("- 배송 담당자 근무시간: 09:00 - 18:00\n");
        prompt.append("- 허브 간 이동 시간: 약 2-4시간 (거리에 따라 다름)\n");
        prompt.append("- 각 허브에서의 상하차 시간: 약 30분\n\n");

        prompt.append("## 응답 형식 (중요!)\n");
        prompt.append("**반드시** 다음 형식으로만 응답하세요:\n\n");
        prompt.append("날짜: YYYY-MM-DD HH:MM\n");
        prompt.append("근거: (200자 이내로 계산 근거를 간단히 설명)\n\n");
        prompt.append("예시:\n");
        prompt.append("날짜: 2025-12-10 14:00\n");
        prompt.append("근거: 총 이동시간 10시간 고려, 18:00 도착 목표로 역산\n");

        return prompt.toString();
    }

    /**
     * 주문 알림 Slack 메시지 생성
     */
    private String buildOrderNotificationMessage(OrderNotificationRequest request, String aiDeadline) {
        StringBuilder message = new StringBuilder();
        message.append("📦 **새로운 주문 알림**\n\n");
        message.append("주문 번호: ").append(request.orderId()).append("\n");
        message.append("주문자 정보: ").append(request.ordererInfo()).append("\n");
        message.append("상품 정보: ").append(request.productInfo()).append("\n");
        message.append("요청 사항: ").append(request.requestDetails() != null ? request.requestDetails() : "없음").append("\n\n");

        message.append("📍 **배송 경로**\n");
        message.append("발송지: ").append(request.departureHub()).append("\n");

        if (request.waypoints() != null && !request.waypoints().isEmpty()) {
            message.append("경유지: ").append(String.join(" → ", request.waypoints())).append("\n");
        }

        message.append("도착지: ").append(request.destinationHub()).append("\n");
        message.append("최종 배송지: ").append(request.destinationAddress()).append("\n\n");

        message.append("🚚 **배송 담당자**\n");
        message.append(request.deliveryPersonInfo()).append("\n\n");

        message.append("⏰ **AI 계산 결과**\n");
        message.append("최종 발송 시한: ").append(aiDeadline).append("\n\n");

        message.append("위 시한까지 출발해야 납품 기한을 맞출 수 있습니다.");

        return message.toString();
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
        Set<String> allowedSortFields = Set.of("createdAt", "updatedAt", "id");
        String validatedSortBy = allowedSortFields.contains(sortBy) ? sortBy : "createdAt";

        Sort.Direction direction = isAsc ? Sort.Direction.ASC : Sort.Direction.DESC;

        return PageRequest.of(validatedPage, validatedSize, Sort.by(direction, validatedSortBy));
    }

    /**
     * 알림 필터링 조회 (동적 쿼리)
     * - 발신자, 수신자, 메시지 타입, 상태별 필터링
     */
    public Page<NotificationResponse> searchNotifications(
            String senderUsername,
            String recipientSlackId,
            MessageType messageType,
            MessageStatus status,
            int page,
            int size,
            String sortBy,
            boolean isAsc
    ) {
        log.info("[NotificationService] 알림 필터링 조회 - senderUsername: {}, recipientSlackId: {}, messageType: {}, status: {}",
                senderUsername, recipientSlackId, messageType, status);

        Pageable pageable = createPageable(page, size, sortBy, isAsc);

        // 모든 필터가 null인 경우 전체 조회
        if (senderUsername == null && recipientSlackId == null && messageType == null && status == null) {
            Page<Notification> notifications = notificationRepository.findAll(pageable);
            return notifications.map(NotificationResponse::from);
        }

        // 필터 조건에 맞는 알림 조회
        List<Notification> allNotifications = notificationRepository.findAll();
        List<Notification> filteredNotifications = allNotifications.stream()
                .filter(n -> senderUsername == null || (n.getSenderUsername() != null && n.getSenderUsername().equals(senderUsername)))
                .filter(n -> recipientSlackId == null || n.getRecipientSlackId().equals(recipientSlackId))
                .filter(n -> messageType == null || n.getMessageType() == messageType)
                .filter(n -> status == null || n.getStatus() == status)
                .toList();

        // 수동 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredNotifications.size());
        List<Notification> pagedNotifications = start >= filteredNotifications.size()
                ? List.of()
                : filteredNotifications.subList(start, end);

        List<NotificationResponse> responses = pagedNotifications.stream()
                .map(NotificationResponse::from)
                .toList();

        return new org.springframework.data.domain.PageImpl<>(
                responses,
                pageable,
                filteredNotifications.size()
        );
    }
}
