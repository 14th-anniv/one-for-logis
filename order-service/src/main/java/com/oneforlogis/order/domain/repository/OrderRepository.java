package com.oneforlogis.order.domain.repository;

import com.oneforlogis.order.domain.model.Order;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(UUID id);
}

