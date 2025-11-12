package com.oneforlogis.notification.application.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeliveryStatusChangedEvent(
        String eventId,
        OffsetDateTime occurredAt,
        DeliveryData delivery
) {

    public record DeliveryData(
            UUID deliveryId,
            UUID orderId,
            String previousStatus,
            String currentStatus,
            String recipientSlackId,
            String recipientName
    ) {
    }
}
