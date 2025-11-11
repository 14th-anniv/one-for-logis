package com.oneforlogis.delivery.application.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderCreatedMessage(
        String eventId,
        OffsetDateTime occurredAt,
        Order order
) {

    public record Order(
            UUID orderId,
            Receiver receiver,
            Route route
    ) {

    }

    public record Receiver(String name, String address, String slackId) {

    }

    public record Route(String startHubId, String destinationHubId) {

    }
}
