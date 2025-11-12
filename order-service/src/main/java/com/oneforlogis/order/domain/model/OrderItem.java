package com.oneforlogis.order.domain.model;

import com.oneforlogis.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "p_order_item")
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    // 단방향 매핑: Order 엔티티에서 @JoinColumn 으로 관리 (여기엔 Order 필드 없음)

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name", length = 255, nullable = false)
    private String productName;

    @Column(name = "unit_price", precision = 18, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "line_total", precision = 18, scale = 2, nullable = false)
    private BigDecimal lineTotal;

    @Builder
    public OrderItem(UUID productId, String productName, 
                     BigDecimal unitPrice, Integer quantity) {
        if (productId == null) {
            throw new IllegalArgumentException("상품 ID는 필수입니다.");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("단가는 필수입니다.");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("단가는 0 이상이어야 합니다.");
        }

        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * 수량 변경 시 lineTotal 자동 재계산
     */
    public void updateQuantity(Integer quantity) {
        this.quantity = quantity;
        this.lineTotal = this.unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * OrderItem 생성 정적 팩토리 메서드
     */
    public static OrderItem from(UUID productId, String productName,
                                 BigDecimal unitPrice, Integer quantity) {
        // Builder 대신 생성자를 직접 호출하여 lineTotal이 자동 계산되도록 함
        return new OrderItem(productId, productName, unitPrice, quantity);
    }
}

