package com.oneforlogis.delivery.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.delivery.application.dto.request.DeliveryStaffRequest;
import com.oneforlogis.delivery.application.dto.response.DeliveryStaffResponse;
import com.oneforlogis.delivery.application.service.DeliveryStaffService;
import com.oneforlogis.delivery.domain.model.DeliveryStaffType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DeliveryStaffController.class)
@AutoConfigureMockMvc(addFilters = false)
class DeliveryStaffControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeliveryStaffService deliveryStaffService;

    @Test
    @DisplayName("배송 담당자 등록 성공")
    void registerStaff_success() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        DeliveryStaffRequest request = new DeliveryStaffRequest(
                UUID.randomUUID(),
                DeliveryStaffType.IN_HOUSE,
                "U123456",
                1,
                true
        );

        when(deliveryStaffService.register(eq(deliveryId), any()))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/deliveries-staff/{deliveryId}", deliveryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("OK"));
    }

    @Test
    @DisplayName("배송 담당자 등록 실패 - 유효성 검증 실패")
    void registerStaff_validationFail() throws Exception {
        DeliveryStaffRequest bad = new DeliveryStaffRequest(
                null, null, "", null, null
        );

        mockMvc.perform(post("/api/v1/deliveries-staff/{deliveryId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("허브별 배송 담당자 조회 성공 - 결과 2건")
    void getHubStaff_success() throws Exception {
        UUID hubId = UUID.randomUUID();
        List<DeliveryStaffResponse> responses = List.of(
                new DeliveryStaffResponse(1L, hubId, DeliveryStaffType.IN_HOUSE, "U123456", 1,
                        true),
                new DeliveryStaffResponse(2L, hubId, DeliveryStaffType.PARTNER, "U654321", 0, false)
        );

        when(deliveryStaffService.getStaffByHub(eq(hubId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(responses));

        mockMvc.perform(get("/api/v1/deliveries-staff/{hubId}", hubId)
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "김")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].slackId").value("U123456"))
                .andExpect(jsonPath("$.data[1].staffType").value("PARTNER"));
    }

    @Test
    @DisplayName("허브별 배송 담당자 조회 성공 - 결과 0건")
    void getHubStaff_empty() throws Exception {
        UUID hubId = UUID.randomUUID();

        when(deliveryStaffService.getStaffByHub(eq(hubId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/deliveries-staff/{hubId}", hubId)
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("허브별 배송 담당자 조회 실패 - 잘못된 hubId 형식(바인딩 에러) -> 400")
    void getHubStaff_invalidHubIdFormat() throws Exception {
        mockMvc.perform(get("/api/v1/deliveries-staff/{hubId}", "not-a-uuid")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("허브별 배송 담당자 조회 실패 - 허브 없음 -> 404 + isSuccess=false")
    void getHubStaff_hubNotFound() throws Exception {
        UUID hubId = UUID.randomUUID();

        when(deliveryStaffService.getStaffByHub(eq(hubId), any(Pageable.class)))
                .thenThrow(new CustomException(ErrorCode.HUB_NOT_FOUND));

        mockMvc.perform(get("/api/v1/deliveries-staff/{hubId}", hubId)
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false));
    }

    @Test
    @DisplayName("다음 배정 대상 조회 성공 - 200 + isSuccess=true")
    void getNextStaff_success() throws Exception {
        // given
        UUID hubId = UUID.randomUUID();

        DeliveryStaffResponse response = new DeliveryStaffResponse(
                1L,
                hubId,
                DeliveryStaffType.IN_HOUSE,
                "U111111",
                1,
                true
        );

        when(deliveryStaffService.getNextStaff(eq(hubId)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/deliveries-staff/{hubId}/next", hubId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.hubId").value(hubId.toString()))
                .andExpect(jsonPath("$.data.slackId").value("U111111"));
    }

    @Test
    @DisplayName("다음 배정 대상 조회 실패 - 배정 가능한 직원이 없으면 400 + isSuccess=false")
    void getNextStaff_noActiveStaff() throws Exception {
        // given
        UUID hubId = UUID.randomUUID();

        when(deliveryStaffService.getNextStaff(eq(hubId)))
                .thenThrow(new CustomException(ErrorCode.NO_ACTIVE_STAFF));

        // when & then
        mockMvc.perform(get("/api/v1/deliveries-staff/{hubId}/next", hubId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("배정 가능한 직원이 없습니다."));
    }
}