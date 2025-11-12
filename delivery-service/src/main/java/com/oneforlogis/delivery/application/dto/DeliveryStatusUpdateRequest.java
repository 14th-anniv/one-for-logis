package com.oneforlogis.delivery.application.dto;

import java.time.LocalDateTime;

public record DeliveryStatusUpdateRequest(

        String status,

        LocalDateTime eventAt
) {

}