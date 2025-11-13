package com.oneforlogis.delivery.application.dto.request;

import com.oneforlogis.delivery.domain.model.DeliveryStatus;
import java.util.UUID;

public record DeliverySearchCond(
        DeliveryStatus status,
        String receiverName,
        UUID orderId,
        UUID fromHubId,
        UUID toHubId
) {

}