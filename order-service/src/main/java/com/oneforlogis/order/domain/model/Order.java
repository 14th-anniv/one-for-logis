package com.oneforlogis.order.domain.model;

import com.oneforlogis.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(name = "p_order")
@SQLRestriction("deleted_at IS NULL")
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "order_no", length = 30, nullable = false, unique = true)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "supplier_company_id", nullable = false)
    private UUID supplierCompanyId;

    @Column(name = "receiver_company_id", nullable = false)
    private UUID receiverCompanyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private OrderStatus status;

    @Column(name = "delivery_id")
    private UUID deliveryId;

    @Column(name = "items_count", nullable = false)
    private Integer itemsCount;

    @Column(name = "total_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "request_note", columnDefinition = "TEXT")
    private String requestNote;

    // 단방향: Order -> OrderItem
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id") // p_order_item.order_id
    private List<OrderItem> orderItems = new ArrayList<>();

    // 단방향: Order -> OrderStatusHistory
    @OneToMany(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "order_id") // p_order_status_history.order_id
    private List<OrderStatusHistory> statusHistories = new ArrayList<>();

    @Builder
    public Order(String orderNo, Long userId, UUID supplierCompanyId, UUID receiverCompanyId,
                 OrderStatus status, UUID deliveryId, Integer itemsCount, BigDecimal totalAmount,
                 String requestNote, List<OrderItem> orderItems, List<OrderStatusHistory> statusHistories) {
        this.orderNo = orderNo;
        this.userId = userId;
        this.supplierCompanyId = supplierCompanyId;
        this.receiverCompanyId = receiverCompanyId;
        this.status = status;
        this.deliveryId = deliveryId;
        this.itemsCount = itemsCount != null ? itemsCount : 0;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.requestNote = requestNote;
        this.orderItems = orderItems != null ? orderItems : new ArrayList<>();
        this.statusHistories = statusHistories != null ? statusHistories : new ArrayList<>();
    }

    /**
     * 주문번호 생성 (예: ORD-20250115-00001)
     */
    private static String generateOrderNo() {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = String.format("%05d", (int) (Math.random() * 100000));
        return "ORD-" + datePrefix + "-" + randomSuffix;
    }

    /**
     * OrderItem 추가 및 자동 계산
     */
    public void addOrderItem(OrderItem item) {
        this.orderItems.add(item);
        this.itemsCount = this.orderItems.size();
        this.totalAmount = this.orderItems.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * OrderStatusHistory 추가
     */
    public void addStatusHistory(OrderStatusHistory history) {
        this.statusHistories.add(history);
    }

    /**
     * 주문 상태 변경 및 이력 자동 생성
     */
    public void changeStatus(OrderStatus newStatus, String reason) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .fromStatus(this.status)
                .toStatus(newStatus)
                .reason(reason)
                .build();
        this.addStatusHistory(history);
        this.status = newStatus;
    }

    /**
     * 주문 취소
     */
    public void cancel(String reason) {
        if (this.status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("배송완료된 주문은 취소할 수 없습니다.");
        }
        this.changeStatus(OrderStatus.CANCELED, reason);
    }

    /**
     * 엔티티 저장 전 검증
     */
    @PrePersist
    @PreUpdate
    private void validate() {
        if (itemsCount != orderItems.size()) {
            throw new IllegalStateException("itemsCount와 orderItems.size()가 일치하지 않습니다.");
        }
        
        BigDecimal calculatedTotal = orderItems.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalAmount.compareTo(calculatedTotal) != 0) {
            throw new IllegalStateException("totalAmount와 orderItems의 lineTotal 합계가 일치하지 않습니다.");
        }
    }
}

