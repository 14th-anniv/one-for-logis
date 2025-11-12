package com.oneforlogis.delivery.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "p_delivery_staff")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_id")
    private Long staffId;

    @Column(name = "hub_id")
    private UUID hubId;

    @Enumerated(EnumType.STRING)
    @Column(name = "staff_type", nullable = false, length = 30)
    private DeliveryStaffType staffType;

    @Column(name = "slack_id", length = 100)
    private String slackId;

    @Column(name = "assign_order", nullable = false)
    private Integer assignOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static DeliveryStaff create(
            UUID hubId,
            DeliveryStaffType staffType,
            String slackId,
            Integer assignOrder,
            Boolean isActive) {
        DeliveryStaff s = new DeliveryStaff();
        s.hubId = hubId;
        s.staffType = staffType;
        s.slackId = slackId;
        s.assignOrder = (assignOrder == null ? 1 : assignOrder);
        s.isActive = (isActive == null ? Boolean.TRUE : isActive);
        s.createdAt = LocalDateTime.now();
        s.updatedAt = s.createdAt;
        return s;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return this.staffId;
    }
}
