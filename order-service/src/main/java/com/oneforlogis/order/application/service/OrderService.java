package com.oneforlogis.order.application.service;

import com.oneforlogis.common.api.PageResponse;
import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.order.domain.model.Order;
import com.oneforlogis.order.domain.model.OrderItem;
import com.oneforlogis.order.domain.model.OrderStatus;
import com.oneforlogis.order.domain.repository.OrderRepository;
import com.oneforlogis.order.presentation.request.OrderCreateRequest;
import com.oneforlogis.order.presentation.request.OrderStatusChangeRequest;
import com.oneforlogis.order.presentation.response.OrderCreateResponse;
import com.oneforlogis.order.presentation.response.OrderDetailResponse;
import com.oneforlogis.order.presentation.response.OrderStatusChangeResponse;
import com.oneforlogis.order.presentation.response.OrderSummaryResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    public PageResponse<OrderSummaryResponse> getOrders(
            String status,
            String supplierId,
            String receiverId,
            String startDate,
            String endDate,
            int page,
            int size,
            String sort
    ) {
        // 정렬 처리 (기본값: createdAt,DESC)
        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        // 필터 파라미터 변환
        OrderStatus orderStatus = parseOrderStatus(status);
        UUID supplierUuid = parseUuid(supplierId);
        UUID receiverUuid = parseUuid(receiverId);
        LocalDateTime startDateTime = parseStartDate(startDate);
        LocalDateTime endDateTime = parseEndDate(endDate);

        // 조회
        Page<Order> orderPage = orderRepository.findByFilters(
                orderStatus,
                supplierUuid,
                receiverUuid,
                startDateTime,
                endDateTime,
                pageable
        );

        return PageResponse.fromPage(orderPage.map(OrderSummaryResponse::from));
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        try {
            String[] parts = sort.split(",");
            if (parts.length != 2) {
                return Sort.by(Sort.Direction.DESC, "createdAt");
            }

            String field = parts[0].trim();
            String direction = parts[1].trim().toUpperCase();

            Sort.Direction sortDirection = direction.equals("ASC") 
                    ? Sort.Direction.ASC 
                    : Sort.Direction.DESC;

            return Sort.by(sortDirection, field);
        } catch (Exception e) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
    }

    private OrderStatus parseOrderStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
    }

    private UUID parseUuid(String uuidStr) {
        if (uuidStr == null || uuidStr.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
    }

    private LocalDateTime parseStartDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return date.atStartOfDay(); // 00:00:00
        } catch (DateTimeParseException e) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
    }

    private LocalDateTime parseEndDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return date.atTime(23, 59, 59); // 23:59:59
        } catch (DateTimeParseException e) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
    }

    @Transactional
    public OrderStatusChangeResponse changeStatus(UUID orderId, OrderStatusChangeRequest request) {
        // 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 최종 상태 체크 (DELIVERED, CANCELED는 변경 불가)
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELED) {
            throw new CustomException(ErrorCode.ORDER_ALREADY_FINAL);
        }

        // 목표 상태 파싱
        OrderStatus toStatus;
        try {
            toStatus = OrderStatus.valueOf(request.toStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        // 현재 상태와 동일하면 변경 불필요
        if (order.getStatus() == toStatus) {
            throw new CustomException(ErrorCode.ORDER_STATUS_CHANGE_INVALID);
        }

        // 상태 전이 검증
        if (!isValidStatusTransition(order.getStatus(), toStatus)) {
            throw new CustomException(ErrorCode.ORDER_STATUS_CHANGE_INVALID);
        }

        // 상태 변경 전 상태 저장
        OrderStatus fromStatus = order.getStatus();

        // 상태 변경 (이력 자동 생성)
        String reason = request.reason() != null && !request.reason().isBlank() 
                ? request.reason() 
                : "상태 변경";
        order.changeStatus(toStatus, reason);

        // 저장
        Order savedOrder = orderRepository.save(order);

        // 응답 생성
        return new OrderStatusChangeResponse(
                savedOrder.getId(),
                fromStatus.name(),
                toStatus.name(),
                savedOrder.getUpdatedAt() != null ? savedOrder.getUpdatedAt().toString() : null
        );
    }

    /**
     * 상태 전이 유효성 검증
     * - PENDING -> PAID
     * - PAID -> PACKING
     * - PACKING -> SHIPPED
     * - SHIPPED -> DELIVERED
     * - ANY -> CANCELED
     */
    private boolean isValidStatusTransition(OrderStatus from, OrderStatus to) {
        // 취소는 어느 상태에서든 가능
        if (to == OrderStatus.CANCELED) {
            return true;
        }

        // 정상 전이 규칙
        return switch (from) {
            case PENDING -> to == OrderStatus.PAID;
            case PAID -> to == OrderStatus.PACKING;
            case PACKING -> to == OrderStatus.SHIPPED;
            case SHIPPED -> to == OrderStatus.DELIVERED;
            default -> false; // DELIVERED, CANCELED는 이미 위에서 체크됨
        };
    }
}

