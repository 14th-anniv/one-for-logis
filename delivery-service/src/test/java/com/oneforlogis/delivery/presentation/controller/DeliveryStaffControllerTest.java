package com.oneforlogis.delivery.presentation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneforlogis.delivery.application.dto.request.DeliveryStaffRequest;
import com.oneforlogis.delivery.application.service.DeliveryStaffService;
import com.oneforlogis.delivery.domain.model.DeliveryStaffType;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

        Mockito.when(deliveryStaffService.register(Mockito.eq(deliveryId), Mockito.any()))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/deliveries/{deliveryId}/staff", deliveryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("배송 담당자가 등록되었습니다."));
    }

    @Test
    @DisplayName("배송 담당자 등록 실패 - 유효성 검증 실패")
    void registerStaff_validationFail() throws Exception {
        DeliveryStaffRequest bad = new DeliveryStaffRequest(
                null, null, "", null, null
        );

        mockMvc.perform(post("/api/v1/deliveries/{deliveryId}/staff", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }
}