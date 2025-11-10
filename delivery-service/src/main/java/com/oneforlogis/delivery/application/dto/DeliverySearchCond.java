package com.oneforlogis.delivery.application.dto;

import com.oneforlogis.delivery.domain.model.DeliveryStatus;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeliverySearchCond {

    private final UUID orderId;
    private final DeliveryStatus status;
    private final String receiverName;
    private final String startHubId;
    private final String destinationHubId;
}