package com.oneforlogis.order.application.service;

import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.order.domain.model.Order;
import com.oneforlogis.order.domain.model.OrderItem;
import com.oneforlogis.order.domain.repository.OrderRepository;
import com.oneforlogis.order.presentation.request.OrderCreateRequest;
import com.oneforlogis.order.presentation.response.OrderCreateResponse;
import com.oneforlogis.order.presentation.response.OrderDetailResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public OrderCreateResponse createOrder(OrderCreateRequest request) {
        // TODO: 추후 JWT에서 userId 추출하도록 변경
        Long userId = 1L;

        // OrderItem 생성
        // TODO: 추후 product-service에서 실제 unitPrice 조회하도록 변경
        // 현재는 1차 구현 테스트를 위해 임시로 0으로 설정
        List<OrderItem> orderItems = request.items().stream()
                .map(item -> OrderItem.from(
                        item.productId(),
                        item.productName(),
                        BigDecimal.ZERO, // TODO: product-service 연동 시 실제 가격 조회
                        item.quantity()
                ))
                .collect(Collectors.toList());

        // Order 생성
        Order order = Order.create(
                userId,
                request.supplierCompanyId(),
                request.receiverCompanyId(),
                request.requestNote(),
                orderItems
        );

        // 저장
        Order savedOrder = orderRepository.save(order);

        return new OrderCreateResponse(savedOrder.getId());
    }

    public OrderDetailResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        return OrderDetailResponse.from(order);
    }
}

