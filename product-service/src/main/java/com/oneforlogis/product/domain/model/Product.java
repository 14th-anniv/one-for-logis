package com.oneforlogis.product.domain.model;

import com.oneforlogis.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private UUID hubId;

    @Column(nullable = false)
    private UUID companyId;

    @Builder
    public Product(String name, Integer quantity, BigDecimal price, UUID hubId, UUID companyId) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.hubId = hubId;
        this.companyId = companyId;
    }


    public static Product createProduct(String name, Integer quantity, BigDecimal price, UUID hubId, UUID companyId){
        return Product.builder()
                .name(name)
                .quantity(quantity)
                .price(price)
                .hubId(hubId)
                .companyId(companyId)
                .build();
    }

    /**
     * 임시 예외 처리 (IllegalArgumentException 변경 예정)
     */
    public void updateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("상품명은 비워둘 수 없습니다.");
        }
        this.name = name;
    }

    public void updateQuantity(Integer quantity) {
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("재고는 0 이상이어야 합니다.");
        }
        this.quantity = quantity;
    }

    public void updatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("단가는 0 이상이어야 합니다.");
        }
        this.price = price;
    }

    public void deleteProduct(String userName){
        this.markAsDeleted(userName);
    }
}
