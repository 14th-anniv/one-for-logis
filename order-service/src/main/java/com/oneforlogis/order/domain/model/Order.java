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
    @OneToMany(cascade = CascadeType.ALL)
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
     * 주문 생성 정적 팩토리 메서드
     */
    public static Order create(Long userId, UUID supplierCompanyId, UUID receiverCompanyId,
                               String requestNote, List<OrderItem> orderItems) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        if (supplierCompanyId == null) {
            throw new IllegalArgumentException("공급업체 회사 ID는 필수입니다.");
        }
        if (receiverCompanyId == null) {
            throw new IllegalArgumentException("수신업체 회사 ID는 필수입니다.");
        }
        if (orderItems == null || orderItems.isEmpty()) {
            throw new IllegalArgumentException("주문 항목은 최소 1개 이상 필요합니다.");
        }

        String orderNo = generateOrderNo();
        Order order = Order.builder()
                .orderNo(orderNo)
                .userId(userId)
                .supplierCompanyId(supplierCompanyId)
                .receiverCompanyId(receiverCompanyId)
                .status(OrderStatus.PENDING)
                .requestNote(requestNote)
                .orderItems(new ArrayList<>())
                .statusHistories(new ArrayList<>())
                .build();

        // OrderItem 추가 및 자동 계산
        for (OrderItem item : orderItems) {
            order.addOrderItem(item);
        }

        // 초기 상태 이력 생성 (fromStatus는 null 대신 PENDING으로 설정)
        // 실제로는 주문 생성 시점이므로 fromStatus와 toStatus가 동일
        OrderStatusHistory initialHistory = OrderStatusHistory.builder()
                .fromStatus(OrderStatus.PENDING)
                .toStatus(OrderStatus.PENDING)
                .reason("주문 접수")
                .build();
        order.addStatusHistory(initialHistory);

        return order;
    }

    /**
     * OrderItem 추가 및 자동 계산
     */
    public void addOrderItem(OrderItem item) {
        if (item == null) {
            throw new IllegalArgumentException("주문 항목은 null일 수 없습니다.");
        }
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
        if (history == null) {
            throw new IllegalArgumentException("OrderStatusHistory는 null일 수 없습니다.");
        }

        // fromStatus가 비어 있으면 현재 주문 상태 또는 기본값으로 채워서 재생성
        if (history.getFromStatus() == null) {
            OrderStatus fallback = this.status != null ? this.status : OrderStatus.PENDING;
            history = OrderStatusHistory.builder()
                    .fromStatus(fallback)
                    .toStatus(history.getToStatus())
                    .reason(history.getReason())
                    .build();
        }
        this.statusHistories.add(history);
    }

    /**
     * 주문 상태 변경 및 이력 자동 생성
     */
    public void changeStatus(OrderStatus newStatus, String reason) {
        if (this.status == null) {
            throw new IllegalStateException("주문 상태가 null입니다. 상태 변경이 불가능합니다.");
        }

        // 상태 변경 전 현재 상태를 명시적으로 저장
        OrderStatus currentStatus = this.status;

        // OrderStatusHistory 생성 (Builder 패턴 사용)
        OrderStatusHistory history = OrderStatusHistory.builder()
                .fromStatus(currentStatus)
                .toStatus(newStatus)
                .reason(reason != null && !reason.isBlank() ? reason : "상태 변경")
                .build();

        this.addStatusHistory(history);
        this.status = newStatus;
    }

    /**
     * 주문 취소
     */
    public void cancel(String reason) {
        // SHIPPED, DELIVERED 상태는 취소 불가
        if (this.status == OrderStatus.SHIPPED || this.status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("배송중이거나 배송완료된 주문은 취소할 수 없습니다.");
        }
        // 이미 CANCELED 상태면 멱등성을 위해 아무것도 하지 않음
        if (this.status == OrderStatus.CANCELED) {
            return;
        }
        this.changeStatus(OrderStatus.CANCELED, reason);
    }

    /**
     * 주문 정보 수정
     * - requestNote: DELIVERED, CANCELED가 아닌 모든 상태에서 수정 가능
     * - supplierCompanyId, receiverCompanyId, items: PENDING 상태일 때만 수정 가능
     */
    public void update(String requestNote, UUID supplierCompanyId, UUID receiverCompanyId, List<OrderItem> newItems) {
        // DELIVERED, CANCELED 상태는 모든 필드 수정 불가
        if (this.status == OrderStatus.DELIVERED || this.status == OrderStatus.CANCELED) {
            throw new IllegalStateException("완료되거나 취소된 주문은 수정할 수 없습니다.");
        }

        // requestNote 수정 (DELIVERED, CANCELED가 아닌 모든 상태에서 가능)
        if (requestNote != null) {
            this.requestNote = requestNote;
        }

        // PENDING 상태일 때만 supplierCompanyId, receiverCompanyId, items 수정 가능
        if (this.status == OrderStatus.PENDING) {
            if (supplierCompanyId != null) {
                this.supplierCompanyId = supplierCompanyId;
            }
            if (receiverCompanyId != null) {
                this.receiverCompanyId = receiverCompanyId;
            }
            if (newItems != null && !newItems.isEmpty()) {
                // 기존 items 제거 후 새 items 추가
                this.orderItems.clear();
                for (OrderItem item : newItems) {
                    this.addOrderItem(item);
                }
            }
        } else {
            // PENDING 상태가 아닐 때 supplierCompanyId, receiverCompanyId, items 수정 시도 시 예외
            if (supplierCompanyId != null || receiverCompanyId != null) {
                throw new IllegalStateException("주문 접수 상태가 아닌 경우 공급업체/수신업체 정보는 수정할 수 없습니다.");
            }
            if (newItems != null && !newItems.isEmpty()) {
                throw new IllegalStateException("주문 확정 후 주문 항목은 수정할 수 없습니다.");
            }
        }
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

