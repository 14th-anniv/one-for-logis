package com.oneforlogis.delivery.application.service;


import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.delivery.application.dto.request.DeliveryStaffRequest;
import com.oneforlogis.delivery.application.dto.response.DeliveryStaffResponse;
import com.oneforlogis.delivery.domain.model.Delivery;
import com.oneforlogis.delivery.domain.model.DeliveryStaff;
import com.oneforlogis.delivery.domain.model.DeliveryStatus;
import com.oneforlogis.delivery.domain.repository.DeliveryRepository;
import com.oneforlogis.delivery.domain.repository.DeliveryStaffRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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

    @Transactional
    public Page<DeliveryStaffResponse> getStaffByHub(UUID hubId, Pageable pageable) {
        Page<DeliveryStaff> page = deliveryStaffRepository.findByHubId(hubId, pageable);

        return page.map(s -> new DeliveryStaffResponse(
                s.getStaffId(),
                s.getHubId(),
                s.getStaffType(),
                s.getSlackId(),
                s.getAssignOrder(),
                s.getIsActive()
        ));
    }

    @Transactional
    public DeliveryStaffResponse getNextStaff(UUID hubId) {

        Page<DeliveryStaff> page = deliveryStaffRepository.findNextStaff(hubId,
                PageRequest.of(0, 1));

        if (page.isEmpty()) {
            throw new CustomException(ErrorCode.NO_ACTIVE_STAFF);
        }

        DeliveryStaff staff = page.getContent().get(0);

        staff.updateLastAssignedAt(LocalDateTime.now());

        return new DeliveryStaffResponse(
                staff.getStaffId(),
                staff.getHubId(),
                staff.getStaffType(),
                staff.getSlackId(),
                staff.getAssignOrder(),
                staff.getIsActive()
        );
    }
}