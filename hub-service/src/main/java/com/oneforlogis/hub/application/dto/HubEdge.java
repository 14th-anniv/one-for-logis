package com.oneforlogis.hub.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record HubEdge(
        UUID toHubId,
        BigDecimal routeDistance,
        Integer routeTime
) {}