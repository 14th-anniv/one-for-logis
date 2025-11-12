package com.oneforlogis.notification.application.event;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        String eventId,
        OffsetDateTime occurredAt,
        OrderData order
) {

    public record OrderData(
            UUID orderId,
            String ordererInfo,
            String requestingCompanyName,
            String receivingCompanyName,
            String productInfo,
            String requestDetails,
            RouteData route,
            ReceiverData receiver,
            HubManagerData hubManager
    ) {
    }

    public record RouteData(
            UUID startHubId,
            String startHubName,
            List<String> waypointHubNames,
            UUID destinationHubId,
            String destinationHubName
    ) {
    }

    public record ReceiverData(
            String name,
            String address,
            String slackId
    ) {
    }

    public record HubManagerData(
            String slackId,
            String name
    ) {
    }
}
