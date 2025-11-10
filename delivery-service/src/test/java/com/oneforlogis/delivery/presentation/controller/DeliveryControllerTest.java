package com.oneforlogis.delivery.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oneforlogis.delivery.application.dto.DeliveryResponse;
import com.oneforlogis.delivery.application.service.DeliveryService;
import com.oneforlogis.delivery.presentation.advice.DeliveryExceptionHandler;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Import(DeliveryExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = DeliveryController.class)
class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeliveryService deliveryService;

    private DeliveryResponse buildDeliveryResponse(UUID deliveryId) {
        return DeliveryResponse.builder()
                .id(deliveryId)
                .orderId(UUID.randomUUID())
                .status("WAITING_AT_HUB")
                .fromHubId(UUID.randomUUID())
                .toHubId(UUID.randomUUID())
                .estimatedDistanceKm(0.0)
                .estimatedDurationMin(0)
                .arrivedDestinationHub(false)
                .destinationHubArrivedAt(null)
                .deliveryStaffId(null)
                .receiverName("홍길동")
                .receiverAddress("서울특별시 중구 을지로 100")
                .receiverSlackId("U1234567")
                .build();
    }

    @Test
    @DisplayName("배송 단건 조회 성공")
    void getDeliveryById_success() throws Exception {
        // given
        UUID deliveryId = UUID.randomUUID();
        DeliveryResponse mockResponse = buildDeliveryResponse(deliveryId);
        Mockito.when(deliveryService.getOne(any(UUID.class))).thenReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/v1/deliveries/{deliveryId}", deliveryId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deliveryId.toString()))
                .andExpect(jsonPath("$.receiverName").value("홍길동"))
                .andExpect(jsonPath("$.status").value("WAITING_AT_HUB"));
    }

    @Test
    @DisplayName("배송 단건 조회 실패 - 존재하지 않는 ID")
    void getDeliveryById_notFound() throws Exception {
        // given
        UUID deliveryId = UUID.randomUUID();
        Mockito.when(deliveryService.getOne(any(UUID.class)))
                .thenThrow(new IllegalArgumentException("해당 배송을 찾을 수 없습니다."));

        // when & then
        mockMvc.perform(get("/api/v1/deliveries/{deliveryId}", deliveryId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }
}