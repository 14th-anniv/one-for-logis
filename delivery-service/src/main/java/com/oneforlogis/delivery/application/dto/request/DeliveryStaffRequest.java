package com.oneforlogis.delivery.application.dto.request;

import com.oneforlogis.delivery.domain.model.DeliveryStaffType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record DeliveryStaffRequest(
        @NotNull(message = "hubId는 필수입니다.")
        UUID hubId,

        @NotNull(message = "staffType은 필수입니다.")
        DeliveryStaffType staffType,

        @Size(max = 100, message = "slackId는 최대 100자입니다.")
        String slackId,

        Integer assignOrder,

        Boolean isActive
) {

}