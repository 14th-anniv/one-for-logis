package com.oneforlogis.product.domain.model;

import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
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

    public void updateName(String name) {
        this.name = name;
    }
    public void updateQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    public void updatePrice(BigDecimal price) {
        this.price = price;
    }

    public void deleteProduct(String userName){
        this.markAsDeleted(userName);
    }


    /**
     * order 제공
     */
    // 차감
    public void decreaseStock(int amount) {
        int restStock = this.quantity - amount;
        if (restStock < 0) {
            throw new CustomException(ErrorCode.STOCK_NOT_ENOUGH);
        }
        this.quantity = restStock;
    }

    // 증가 및 복원
    public void increaseStock(int amount) {
        if (amount <= 0) {
            throw new CustomException(ErrorCode.INVALID_RESTOCK_AMOUNT);
        }
        this.quantity = this.quantity + amount;
    }
}
