package com.oneforlogis.delivery.application.dto;

import com.oneforlogis.delivery.domain.model.DeliveryRouteStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record DeliveryRouteRequest(

        @NotNull(message = "배송 경로 상태는 필수입니다.")
        DeliveryRouteStatus routeStatus,
        @NotNull(message = "이벤트 발생 시간은 필수입니다.")
        LocalDateTime eventAt,
        String hubId,
        Double latitude,
        Double longitude,
        String remark
) {

}