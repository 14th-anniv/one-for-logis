package com.oneforlogis.notification.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneforlogis.common.api.ApiResponse;
import com.oneforlogis.common.model.Role;
import com.oneforlogis.common.security.CustomAccessDeniedHandler;
import com.oneforlogis.common.security.UserPrincipal;
import com.oneforlogis.notification.application.service.ExternalApiLogService;
import com.oneforlogis.notification.application.service.NotificationService;
import com.oneforlogis.notification.domain.model.*;
import com.oneforlogis.notification.infrastructure.client.user.UserResponse;
import com.oneforlogis.notification.infrastructure.client.user.UserServiceClient;
import com.oneforlogis.notification.presentation.request.DeliveryStatusNotificationRequest;
import com.oneforlogis.notification.presentation.request.ManualNotificationRequest;
import com.oneforlogis.notification.presentation.request.OrderNotificationRequest;
import com.oneforlogis.notification.presentation.response.ApiStatisticsResponse;
import com.oneforlogis.notification.presentation.response.ExternalApiLogResponse;
import com.oneforlogis.notification.presentation.response.NotificationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * NotificationController 단위 테스트
 * - @WebMvcTest: Web Layer만 테스트 (Controller + Filter + Security)
 * - Service 계층은 @MockBean으로 모킹
 */
@WebMvcTest(controllers = NotificationController.class)
@Import(com.oneforlogis.notification.global.config.SecurityConfig.class)
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private ExternalApiLogService externalApiLogService;

    @MockBean
    private UserServiceClient userServiceClient;

    @MockBean
    private CustomAccessDeniedHandler accessDeniedHandler;

    @Test
    @DisplayName("주문 알림 API - 성공 (201 Created)")
    void sendOrderNotification_Success() throws Exception {
        // Given
        OrderNotificationRequest request = new OrderNotificationRequest(
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
                "C09QY22AMEE",
                "부산허브 관리자"
        );

        NotificationResponse response = createMockNotificationResponse(MessageStatus.SENT);

        when(notificationService.sendOrderNotification(any(OrderNotificationRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/notifications/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SENT"))
                .andExpect(jsonPath("$.data.messageType").value("ORDER_NOTIFICATION"))
                .andExpect(jsonPath("$.data.senderType").value("SYSTEM"));
    }

    @Test
    @DisplayName("수동 메시지 발송 API - 성공 (201 Created)")
    void sendManualNotification_Success() throws Exception {
        // Given
        ManualNotificationRequest request = new ManualNotificationRequest(
                "C09QY22AMEE",
                "수신자 이름",
                "테스트 메시지입니다."
        );

        UserResponse userResponse = UserResponse.builder()
                .userId(1L)
                .username("testuser")
                .name("테스트 사용자")
                .slackId("C09QY22AMEE")
                .role(Role.HUB_MANAGER)
                .build();

        // USER 타입 응답 생성
        NotificationResponse response = new NotificationResponse(
                UUID.randomUUID(),
                SenderType.USER,
                "testuser",
                "C09QY22AMEE",
                "테스트 사용자",
                "C09QY22AMEE",
                "수신자 이름",
                "테스트 메시지입니다.",
                MessageType.MANUAL,
                null,
                MessageStatus.SENT,
                LocalDateTime.now().toString(),
                null,
                "testuser",
                LocalDateTime.now().toString(),
                "testuser",
                LocalDateTime.now().toString()
        );

        when(userServiceClient.getUserByUsername("testuser"))
                .thenReturn(ApiResponse.success(userResponse));
        when(notificationService.sendManualNotification(
                any(ManualNotificationRequest.class),
                eq("testuser"),
                eq("C09QY22AMEE"),
                eq("테스트 사용자")
        )).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/notifications/manual")
                        .with(authentication(createAuthentication("testuser", Role.HUB_MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SENT"))
                .andExpect(jsonPath("$.data.senderType").value("USER"));
    }

    @Test
    @DisplayName("배송 상태 변경 알림 발송 API - 성공 (201 Created)")
    void sendDeliveryStatusNotification_Success() throws Exception {
        // Given
        UUID deliveryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        DeliveryStatusNotificationRequest request = new DeliveryStatusNotificationRequest(
                deliveryId,
                orderId,
                "HUB_WAITING",
                "HUB_MOVING",
                "C09QY22AMEE",
                "배송담당자"
        );

        // DELIVERY_STATUS_UPDATE 타입 응답 생성
        NotificationResponse response = new NotificationResponse(
                UUID.randomUUID(),
                SenderType.SYSTEM,
                null,
                null,
                null,
                "C09QY22AMEE",
                "배송담당자",
                "🚚 *배송 상태 업데이트*\n\n배송 ID: `" + deliveryId + "`\n주문 ID: `" + orderId + "`\n이전 상태: `HUB_WAITING`\n현재 상태: `HUB_MOVING`\n\n수령인: 배송담당자\n",
                MessageType.DELIVERY_STATUS_UPDATE,
                deliveryId,
                MessageStatus.SENT,
                LocalDateTime.now().toString(),
                null,
                "system",
                LocalDateTime.now().toString(),
                "system",
                LocalDateTime.now().toString()
        );

        when(notificationService.sendDeliveryStatusNotification(any(DeliveryStatusNotificationRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/notifications/delivery-status")
                        .with(authentication(createAuthentication("testuser", Role.DELIVERY_MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SENT"))
                .andExpect(jsonPath("$.data.messageType").value("DELIVERY_STATUS_UPDATE"))
                .andExpect(jsonPath("$.data.senderType").value("SYSTEM"));
    }

    @Test
    @DisplayName("배송 상태 변경 알림 발송 API - 필수 필드 누락 시 400 Bad Request")
    void sendDeliveryStatusNotification_MissingFields_400() throws Exception {
        // Given - deliveryId 누락
        DeliveryStatusNotificationRequest request = new DeliveryStatusNotificationRequest(
                null,  // deliveryId 누락
                UUID.randomUUID(),
                "HUB_WAITING",
                "HUB_MOVING",
                "C09QY22AMEE",
                "배송담당자"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/notifications/delivery-status")
                        .with(authentication(createAuthentication("testuser", Role.DELIVERY_MANAGER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("알림 단일 조회 API - 성공 (200 OK)")
    void getNotification_Success() throws Exception {
        // Given
        UUID notificationId = UUID.randomUUID();
        NotificationResponse response = createMockNotificationResponse(MessageStatus.SENT);

        when(notificationService.getNotification(notificationId))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/notifications/{notificationId}", notificationId)
                        .with(authentication(createAuthentication("testuser", Role.MASTER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.status").value("SENT"));
    }

    @Test
    @DisplayName("알림 목록 페이징 조회 - 성공 (200 OK)")
    void getNotifications_Pageable_Success() throws Exception {
        // Given
        List<NotificationResponse> content = Arrays.asList(
                createMockNotificationResponse(MessageStatus.SENT),
                createMockNotificationResponse(MessageStatus.SENT)
        );
        Page<NotificationResponse> page = new PageImpl<>(content, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")), 2);

        when(notificationService.getNotifications(anyInt(), anyInt(), anyString(), anyBoolean()))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/notifications")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "createdAt")
                        .param("isAsc", "false")
                        .with(authentication(createAuthentication("admin", Role.MASTER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.number").value(0));
    }

    @Test
    @DisplayName("외부 API 로그 조회 - 전체 (200 OK)")
    void getApiLogs_Success() throws Exception {
        // Given
        List<ExternalApiLogResponse> content = Arrays.asList(
                ExternalApiLogResponse.from(createMockApiLog(ApiProvider.SLACK, true)),
                ExternalApiLogResponse.from(createMockApiLog(ApiProvider.GEMINI, true))
        );
        Page<ExternalApiLogResponse> page = new PageImpl<>(content, PageRequest.of(0, 10), 2);

        when(externalApiLogService.getAllApiLogs(anyInt(), anyInt(), anyString(), anyBoolean()))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/notifications/api-logs")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "calledAt")
                        .param("isAsc", "false")
                        .with(authentication(createAuthentication("admin", Role.MASTER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    @DisplayName("외부 API 로그 조회 - Provider별 (200 OK)")
    void getApiLogsByProvider_Success() throws Exception {
        // Given
        List<ExternalApiLogResponse> content = Arrays.asList(
                ExternalApiLogResponse.from(createMockApiLog(ApiProvider.SLACK, true))
        );
        Page<ExternalApiLogResponse> page = new PageImpl<>(content, PageRequest.of(0, 10), 1);

        when(externalApiLogService.getApiLogsByProvider(eq(ApiProvider.SLACK), anyInt(), anyInt(), anyString(), anyBoolean()))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/notifications/api-logs/provider/{provider}", "SLACK")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "calledAt")
                        .param("isAsc", "false")
                        .with(authentication(createAuthentication("admin", Role.MASTER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].apiProvider").value("SLACK"));
    }

    @Test
    @DisplayName("외부 API 로그 조회 - 메시지 ID별 (200 OK)")
    void getApiLogsByMessageId_Success() throws Exception {
        // Given
        UUID messageId = UUID.randomUUID();
        List<ExternalApiLogResponse> content = Arrays.asList(
                ExternalApiLogResponse.from(createMockApiLog(ApiProvider.GEMINI, true)),
                ExternalApiLogResponse.from(createMockApiLog(ApiProvider.SLACK, true))
        );
        Page<ExternalApiLogResponse> page = new PageImpl<>(content, PageRequest.of(0, 10), 2);

        when(externalApiLogService.getApiLogsByMessageId(eq(messageId), anyInt(), anyInt(), anyString(), anyBoolean()))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/notifications/api-logs/message/{messageId}", messageId)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "calledAt")
                        .param("isAsc", "false")
                        .with(authentication(createAuthentication("admin", Role.MASTER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    @DisplayName("알림 필터링 조회 - 성공 (200 OK)")
    void searchNotifications_Success() throws Exception {
        // Given
        List<NotificationResponse> content = Arrays.asList(
                createMockNotificationResponse(MessageStatus.SENT)
        );
        Page<NotificationResponse> page = new PageImpl<>(content, PageRequest.of(0, 10), 1);

        when(notificationService.searchNotifications(
                eq("testuser"),
                eq("C09QY22AMEE"),
                eq(MessageType.MANUAL),
                eq(MessageStatus.SENT),
                anyInt(), anyInt(), anyString(), anyBoolean()))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/notifications/search")
                        .param("senderUsername", "testuser")
                        .param("recipientSlackId", "C09QY22AMEE")
                        .param("messageType", "MANUAL")
                        .param("status", "SENT")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "createdAt")
                        .param("isAsc", "false")
                        .with(authentication(createAuthentication("admin", Role.MASTER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    @DisplayName("API 통계 조회 - 성공 (200 OK)")
    void getApiStatistics_Success() throws Exception {
        // Given
        ApiStatisticsResponse slackStats = ApiStatisticsResponse.of(
                ApiProvider.SLACK, 100, 95, 5, 250.5, 100, 1500, BigDecimal.ZERO
        );
        ApiStatisticsResponse geminiStats = ApiStatisticsResponse.of(
                ApiProvider.GEMINI, 50, 48, 2, 1200.3, 800, 3000, BigDecimal.valueOf(0.05)
        );

        Map<ApiProvider, ApiStatisticsResponse> statistics = new HashMap<>();
        statistics.put(ApiProvider.SLACK, slackStats);
        statistics.put(ApiProvider.GEMINI, geminiStats);

        when(externalApiLogService.getApiStatistics())
                .thenReturn(statistics);

        // When & Then
        mockMvc.perform(get("/api/v1/notifications/api-logs/stats")
                        .with(authentication(createAuthentication("admin", Role.MASTER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.SLACK").exists())
                .andExpect(jsonPath("$.data.SLACK.totalCalls").value(100))
                .andExpect(jsonPath("$.data.SLACK.successRate").value(95.0))
                .andExpect(jsonPath("$.data.GEMINI").exists())
                .andExpect(jsonPath("$.data.GEMINI.totalCalls").value(50))
                .andExpect(jsonPath("$.data.GEMINI.totalCost").value(0.05));
    }

    @Test
    @DisplayName("수동 메시지 발송 - 권한 없음 (403 Forbidden)")
    void sendManualNotification_Forbidden() throws Exception {
        // Given
        ManualNotificationRequest request = new ManualNotificationRequest(
                "C09QY22AMEE",
                "수신자",
                "테스트"
        );

        // When & Then (인증 없이 호출)
        mockMvc.perform(post("/api/v1/notifications/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // TODO: @PreAuthorize 권한 체크는 @WebMvcTest에서 작동하지 않음
    // 향후 @SpringBootTest 통합 테스트로 검증 필요 (Issue #16)
    // @Test
    // @DisplayName("API 로그 조회 - MASTER 외 권한 없음 (403 Forbidden)")
    // void getApiLogs_Forbidden_NonMaster() throws Exception {
    //     // When & Then
    //     mockMvc.perform(get("/api/v1/notifications/api-logs")
    //                     .with(authentication(createAuthentication("user", Role.HUB_MANAGER))))
    //             .andExpect(status().isForbidden());
    // }

    // ===== Helper Methods =====

    private NotificationResponse createMockNotificationResponse(MessageStatus status) {
        return new NotificationResponse(
                UUID.randomUUID(),
                SenderType.SYSTEM,
                null,
                null,
                null,
                "C09QY22AMEE",
                "수신자",
                "테스트 메시지",
                MessageType.ORDER_NOTIFICATION,
                UUID.randomUUID(),
                status,
                LocalDateTime.now().toString(),
                null,
                "system",
                LocalDateTime.now().toString(),
                "system",
                LocalDateTime.now().toString()
        );
    }

    private ExternalApiLog createMockApiLog(ApiProvider provider, boolean success) {
        return ExternalApiLog.builder()
                .apiProvider(provider)
                .apiMethod("testMethod")
                .requestData(Collections.singletonMap("test", "data"))
                .responseData(Collections.singletonMap("result", "ok"))
                .httpStatus(200)
                .isSuccess(success)
                .durationMs(100L)
                .cost(BigDecimal.valueOf(0.001))
                .messageId(UUID.randomUUID())
                .build();
    }

    private UserPrincipal createUserPrincipal(String username, Role role) {
        return new UserPrincipal(UUID.randomUUID(), username, role);
    }

    private Authentication createAuthentication(String username, Role role) {
        UserPrincipal principal = createUserPrincipal(username, role);
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }
}
