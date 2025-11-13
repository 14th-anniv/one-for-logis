package com.oneforlogis.delivery.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.delivery.application.dto.request.DeliveryRouteRequest;
import com.oneforlogis.delivery.application.dto.response.DeliveryResponse;
import com.oneforlogis.delivery.application.dto.response.DeliveryRouteResponse;
import com.oneforlogis.delivery.application.service.DeliveryRouteService;
import com.oneforlogis.delivery.application.service.DeliveryService;
import com.oneforlogis.delivery.domain.model.DeliveryRouteStatus;
import com.oneforlogis.delivery.presentation.advice.DeliveryExceptionHandler;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Import(DeliveryExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = {DeliveryController.class, DeliveryRouteController.class})
class DeliveryRouteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeliveryService deliveryService;

    @MockBean
    private DeliveryRouteService deliveryRouteService;

    private DeliveryResponse buildDeliveryResponse(UUID deliveryId) {
        return new DeliveryResponse(
                deliveryId,
                UUID.randomUUID(),
                "WAITING_AT_HUB",
                UUID.randomUUID(),
                UUID.randomUUID(),
                0.0,
                0,
                false,
                null,
                null,
                "홍길동",
                "서울특별시 중구 을지로 100",
                "U1234567"
        );
    }

    private DeliveryResponse resp(UUID id, String status, Long staffId) {
        return new DeliveryResponse(
                id,
                UUID.randomUUID(),
                status,
                UUID.randomUUID(),
                UUID.randomUUID(),
                0.0,
                0,
                false,
                null,
                staffId,
                "홍길동",
                "서울특별시 중구 을지로 100",
                "U1234567"
        );
    }

    @Test
    @DisplayName("배송 경로 생성 성공")
    void createRoute_success() throws Exception {
        UUID deliveryId = UUID.randomUUID();

        String requestBody = """
                {
                    "routeStatus": "DEPARTED_FROM_HUB",
                    "eventAt": "2025-01-01T10:05:00",
                    "hubId": "22222222-2222-2222-2222-222222222222",
                    "latitude": 37.5665,
                    "longitude": 126.978,
                    "remark": "허브 출발"
                }
                """;

        mockMvc.perform(post("/api/v1/deliveries/" + deliveryId + "/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("배송 경로 생성 실패 - 허브 ID가 필요한 상태인데 누락됨")
    void createRoute_missingHub_whenRequired() throws Exception {
        // given
        UUID deliveryId = UUID.randomUUID();
        DeliveryRouteRequest req = new DeliveryRouteRequest(
                DeliveryRouteStatus.ARRIVED_AT_HUB,
                LocalDateTime.of(2025, 1, 1, 10, 30),
                null,
                37.5665,
                126.9780,
                "허브 도착"
        );

        when(
                deliveryRouteService.appendEvent(eq(deliveryId), any(DeliveryRouteRequest.class)))
                .thenThrow(
                        new CustomException(ErrorCode.INVALID_STATUS_TRANSITION));

        // when & then
        mockMvc.perform(post("/api/v1/deliveries/{deliveryId}/routes", deliveryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError());
    }

    private DeliveryRouteResponse sampleResponse(UUID deliveryId, int seq) {
        return new DeliveryRouteResponse(
                UUID.randomUUID(),
                deliveryId,
                seq,
                DeliveryRouteStatus.DEPARTED_FROM_HUB,
                "22222222-2222-2222-2222-222222222222",
                37.5665,
                126.9780,
                LocalDateTime.of(2025, 1, 1, 10, 5),
                "허브 출발"
        );
    }

    @Test
    @DisplayName("배송 경로 전체 조회 - 성공(200 OK)")
    void getRoutes_success() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        List<DeliveryRouteResponse> list = List.of(sampleResponse(deliveryId, 1),
                sampleResponse(deliveryId, 2));
        when(deliveryRouteService.getRoutes(any(UUID.class))).thenReturn(list);

        mockMvc.perform(get("/api/v1/deliveries/{deliveryId}/routes", deliveryId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deliveryId").value(deliveryId.toString()))
                .andExpect(jsonPath("$[0].routeSeq").value(1))
                .andExpect(jsonPath("$[1].routeSeq").value(2));
    }

    @Test
    @DisplayName("배송 경로 전체 조회 - 빈 결과(200 OK, [])")
    void getRoutes_empty() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        when(deliveryRouteService.getRoutes(any(UUID.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/deliveries/{deliveryId}/routes", deliveryId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("배송 경로 전체 조회 - 존재하지 않는 배송ID(404)")
    void getRoutes_notFound() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        when(deliveryRouteService.getRoutes(any(UUID.class)))
                .thenThrow(new CustomException(ErrorCode.DELIVERY_NOT_FOUND));

        mockMvc.perform(get("/api/v1/deliveries/{deliveryId}/routes", deliveryId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}