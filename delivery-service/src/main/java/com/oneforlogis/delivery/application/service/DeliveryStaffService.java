package com.oneforlogis.delivery.application.service;


import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.delivery.application.dto.request.DeliveryStaffRequest;
import com.oneforlogis.delivery.domain.model.Delivery;
import com.oneforlogis.delivery.domain.model.DeliveryStaff;
import com.oneforlogis.delivery.domain.model.DeliveryStatus;
import com.oneforlogis.delivery.domain.repository.DeliveryRepository;
import com.oneforlogis.delivery.domain.repository.DeliveryStaffRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryStaffService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryStaffRepository deliveryStaffRepository;

    @Transactional
    public Long register(UUID deliveryId, DeliveryStaffRequest req) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new CustomException(ErrorCode.DELIVERY_NOT_FOUND));

        if (delivery.getStatus() != DeliveryStatus.WAITING_AT_HUB) {
            throw new CustomException(ErrorCode.INVALID_DELIVERY_ASSIGNMENT);
        }

        if (delivery.getDeliveryStaffId() != null) {
            throw new CustomException(ErrorCode.DUPLICATE_DELIVERY_STAFF);
        }

        DeliveryStaff staff = DeliveryStaff.create(
                delivery,
                req.hubId(),
                req.staffType(),
                req.slackId(),
                req.assignOrder(),
                req.isActive()
        );
        DeliveryStaff saved = deliveryStaffRepository.save(staff);

        delivery.assignStaff(saved.getId());
        return saved.getId();
    }
}