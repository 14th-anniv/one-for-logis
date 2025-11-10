package com.oneforlogis.order.domain.repository;

import com.oneforlogis.order.domain.model.Order;
import com.oneforlogis.order.domain.model.OrderStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(UUID id);
    Page<Order> findByFilters(OrderStatus status, UUID supplierId, UUID receiverId,
                             LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}

