package com.oneforlogis.hub.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DijkstraResult(
        BigDecimal distance,
        Integer time,
        List<UUID> pathNodes
) {}