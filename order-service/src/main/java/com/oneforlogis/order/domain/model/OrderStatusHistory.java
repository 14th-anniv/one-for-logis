package com.oneforlogis.order.domain.model;

import com.oneforlogis.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "p_order_status_history")
public class OrderStatusHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    // 단방향 매핑: Order 에서 @JoinColumn 으로 관리 (여기엔 Order 필드 없음)

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20, nullable = false)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 20, nullable = false)
    private OrderStatus toStatus;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
}

