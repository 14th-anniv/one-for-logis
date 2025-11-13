package com.oneforlogis.delivery.application.dto.response;

import com.oneforlogis.delivery.domain.model.DeliveryStaffType;
import java.util.UUID;

public record DeliveryStaffResponse(
        Long staffId,
        UUID hubId,
        DeliveryStaffType staffType,
        String slackId,
        Integer assignOrder,
        Boolean isActive
) {

}
