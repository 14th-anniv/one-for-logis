package com.oneforlogis.order.infrastructure.persistence;

import com.oneforlogis.order.domain.model.Order;
import com.oneforlogis.order.domain.repository.OrderRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaOrderRepository extends JpaRepository<Order, UUID>, OrderRepository {
}

