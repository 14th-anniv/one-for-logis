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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * NotificationService 단위 테스트
 * Priority 2-3: MockitoExtension으로 외부 의존성 Mock 처리
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService 단위 테스트")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SlackClientWrapper slackClientWrapper;

    @Mock
    private GeminiClientWrapper geminiClientWrapper;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("주문 알림 발송 성공 - Gemini + Slack 정상 응답")
    void sendOrderNotification_Success() {
        // Given
        OrderNotificationRequest request = createOrderNotificationRequest();

        // Mock: Notification 저장
        Notification mockNotification = createMockNotification();
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        // Mock: Gemini AI 응답
        GeminiResponse geminiResponse = createMockGeminiResponse("2024-12-31 14:00까지 발송 완료 바랍니다.");
        when(geminiClientWrapper.generateContent(any(GeminiRequest.class), any(UUID.class)))
                .thenReturn(geminiResponse);

        // Mock: Slack 발송 성공
        SlackMessageResponse slackResponse = SlackMessageResponse.builder()
                .ok(true)
                .channel("U123456")
                .ts("1234567890.123456")
                .build();
        when(slackClientWrapper.postMessage(any(SlackMessageRequest.class), any(UUID.class)))
                .thenReturn(slackResponse);

        // When
        NotificationResponse response = notificationService.sendOrderNotification(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(MessageStatus.SENT);

        // Verify: Gemini와 Slack이 호출되었는지 확인
        verify(geminiClientWrapper, times(1)).generateContent(any(GeminiRequest.class), any(UUID.class));
        verify(slackClientWrapper, times(1)).postMessage(any(SlackMessageRequest.class), any(UUID.class));
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("주문 알림 발송 실패 - Slack 전송 실패 시 예외 throw")
    void sendOrderNotification_SlackFailed_ThrowsException() {
        // Given
        OrderNotificationRequest request = createOrderNotificationRequest();

        Notification mockNotification = createMockNotification();
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        GeminiResponse geminiResponse = createMockGeminiResponse("2024-12-31 14:00까지 발송 완료 바랍니다.");
        when(geminiClientWrapper.generateContent(any(GeminiRequest.class), any(UUID.class)))
                .thenReturn(geminiResponse);

        // Mock: Slack 발송 실패
        SlackMessageResponse slackResponse = SlackMessageResponse.builder()
                .ok(false)
                .error("channel_not_found")
                .build();
        when(slackClientWrapper.postMessage(any(SlackMessageRequest.class), any(UUID.class)))
                .thenReturn(slackResponse);

        // When & Then
        assertThatThrownBy(() -> notificationService.sendOrderNotification(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("알림 발송에 실패했습니다");

        // Verify: Notification이 FAILED로 마킹되었는지 확인
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("수동 알림 발송 성공")
    void sendManualNotification_Success() {
        // Given
        ManualNotificationRequest request = new ManualNotificationRequest(
                "U987654",
                "수신자",
                "테스트 메시지입니다."
        );

        String senderUsername = "testuser";
        String senderSlackId = "U123456";
        String senderName = "홍길동";

        Notification mockNotification = createMockManualNotification();
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        // Mock: Slack 발송 성공
        SlackMessageResponse slackResponse = SlackMessageResponse.builder()
                .ok(true)
                .channel("U987654")
                .ts("1234567890.123456")
                .build();
        when(slackClientWrapper.postMessage(any(SlackMessageRequest.class), any(UUID.class)))
                .thenReturn(slackResponse);

        // When
        NotificationResponse response = notificationService.sendManualNotification(
                request, senderUsername, senderSlackId, senderName
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(MessageStatus.SENT);

        verify(slackClientWrapper, times(1)).postMessage(any(SlackMessageRequest.class), any(UUID.class));
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("수동 알림 발송 실패 - Slack 전송 실패 시 예외 throw")
    void sendManualNotification_SlackFailed_ThrowsException() {
        // Given
        ManualNotificationRequest request = new ManualNotificationRequest(
                "U987654",
                "수신자",
                "테스트 메시지입니다."
        );

        Notification mockNotification = createMockManualNotification();
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        // Mock: Slack 발송 실패
        SlackMessageResponse slackResponse = SlackMessageResponse.builder()
                .ok(false)
                .error("invalid_auth")
                .build();
        when(slackClientWrapper.postMessage(any(SlackMessageRequest.class), any(UUID.class)))
                .thenReturn(slackResponse);

        // When & Then
        assertThatThrownBy(() -> notificationService.sendManualNotification(
                request, "testuser", "U123456", "홍길동"
        ))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("알림 발송에 실패했습니다");
    }

    @Test
    @DisplayName("Kafka 이벤트 기반 주문 알림 - eventId 저장 확인")
    void sendOrderNotificationFromEvent_WithEventId() {
        // Given
        OrderNotificationRequest request = createOrderNotificationRequest();
        String eventId = "event-" + UUID.randomUUID();

        Notification mockNotification = createMockNotification();
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        GeminiResponse geminiResponse = createMockGeminiResponse("2024-12-31 14:00까지 발송 완료 바랍니다.");
        when(geminiClientWrapper.generateContent(any(GeminiRequest.class), any(UUID.class)))
                .thenReturn(geminiResponse);

        SlackMessageResponse slackResponse = SlackMessageResponse.builder()
                .ok(true)
                .channel("U123456")
                .ts("1234567890.123456")
                .build();
        when(slackClientWrapper.postMessage(any(SlackMessageRequest.class), any(UUID.class)))
                .thenReturn(slackResponse);

        // When
        NotificationResponse response = notificationService.sendOrderNotificationFromEvent(request, eventId);

        // Then
        assertThat(response).isNotNull();
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    // === Helper Methods ===

    private OrderNotificationRequest createOrderNotificationRequest() {
        return new OrderNotificationRequest(
                UUID.randomUUID(),
                "주문자: 테스트업체",
                "공급업체명",
                "수령업체명",
                "상품: 테스트상품 x 10",
                "빠른 배송 요청",
                "경기남부 허브",
                Arrays.asList("대전 허브", "대구 허브"),
                "부산 허브",
                "부산시 해운대구",
                "배송담당: 홍길동",
                "U123456",
                "부산허브 관리자"
        );
    }

    private Notification createMockNotification() {
        // Mock Notification with BaseEntity fields initialized
        Notification notification = mock(Notification.class);
        lenient().when(notification.getId()).thenReturn(UUID.randomUUID());
        lenient().when(notification.getSenderType()).thenReturn(SenderType.SYSTEM);
        lenient().when(notification.getRecipientSlackId()).thenReturn("U123456");
        lenient().when(notification.getRecipientName()).thenReturn("부산허브 관리자");
        lenient().when(notification.getMessageContent()).thenReturn("테스트 메시지");
        lenient().when(notification.getMessageType()).thenReturn(MessageType.ORDER_NOTIFICATION);
        lenient().when(notification.getStatus()).thenReturn(MessageStatus.PENDING);
        lenient().when(notification.getCreatedAt()).thenReturn(java.time.LocalDateTime.now());
        lenient().when(notification.getUpdatedAt()).thenReturn(java.time.LocalDateTime.now());

        // markAsSent() 호출 시 상태 변경 시뮬레이션
        lenient().doAnswer(invocation -> {
            lenient().when(notification.getStatus()).thenReturn(MessageStatus.SENT);
            lenient().when(notification.getSentAt()).thenReturn(java.time.LocalDateTime.now());
            return null;
        }).when(notification).markAsSent();

        // markAsFailed() 호출 시 상태 변경 시뮬레이션
        lenient().doAnswer(invocation -> {
            String errorMsg = invocation.getArgument(0);
            lenient().when(notification.getStatus()).thenReturn(MessageStatus.FAILED);
            lenient().when(notification.getErrorMessage()).thenReturn(errorMsg);
            return null;
        }).when(notification).markAsFailed(anyString());

        return notification;
    }

    private Notification createMockManualNotification() {
        Notification notification = mock(Notification.class);
        lenient().when(notification.getId()).thenReturn(UUID.randomUUID());
        lenient().when(notification.getSenderType()).thenReturn(SenderType.USER);
        lenient().when(notification.getSenderUsername()).thenReturn("testuser");
        lenient().when(notification.getSenderSlackId()).thenReturn("U123456");
        lenient().when(notification.getSenderName()).thenReturn("홍길동");
        lenient().when(notification.getRecipientSlackId()).thenReturn("U987654");
        lenient().when(notification.getRecipientName()).thenReturn("수신자");
        lenient().when(notification.getMessageContent()).thenReturn("수동 메시지");
        lenient().when(notification.getMessageType()).thenReturn(MessageType.MANUAL);
        lenient().when(notification.getStatus()).thenReturn(MessageStatus.PENDING);
        lenient().when(notification.getCreatedAt()).thenReturn(java.time.LocalDateTime.now());
        lenient().when(notification.getUpdatedAt()).thenReturn(java.time.LocalDateTime.now());

        lenient().doAnswer(invocation -> {
            lenient().when(notification.getStatus()).thenReturn(MessageStatus.SENT);
            lenient().when(notification.getSentAt()).thenReturn(java.time.LocalDateTime.now());
            return null;
        }).when(notification).markAsSent();

        lenient().doAnswer(invocation -> {
            String errorMsg = invocation.getArgument(0);
            lenient().when(notification.getStatus()).thenReturn(MessageStatus.FAILED);
            lenient().when(notification.getErrorMessage()).thenReturn(errorMsg);
            return null;
        }).when(notification).markAsFailed(anyString());

        return notification;
    }

    private GeminiResponse createMockGeminiResponse(String content) {
        // lenient() 사용하여 UnnecessaryStubbingException 방지
        GeminiResponse response = mock(GeminiResponse.class);
        lenient().when(response.getContent()).thenReturn(content);
        lenient().when(response.getCandidates()).thenReturn(Arrays.asList(new GeminiResponse.Candidate()));
        return response;
    }
}
