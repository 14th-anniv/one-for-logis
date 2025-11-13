package com.oneforlogis.delivery.application.dto.request;

import java.time.LocalDateTime;

public record DeliveryStatusUpdateRequest(

        String status,

        LocalDateTime eventAt
) {

}