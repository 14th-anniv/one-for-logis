package com.oneforlogis.delivery.application.service;

import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.delivery.application.dto.request.DeliveryAssignRequest;
import com.oneforlogis.delivery.application.dto.request.DeliverySearchCond;
import com.oneforlogis.delivery.application.dto.request.DeliveryStatusUpdateRequest;
import com.oneforlogis.delivery.application.dto.response.DeliveryResponse;
import com.oneforlogis.delivery.application.event.OrderCreatedMessage;
import com.oneforlogis.delivery.domain.model.Delivery;
import com.oneforlogis.delivery.domain.model.DeliveryStatus;
import com.oneforlogis.delivery.domain.repository.DeliveryRepository;
import com.oneforlogis.delivery.infrastructure.repository.DeliverySpecifications;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;

    @Transactional
    public UUID createIfAbsentFromOrder(OrderCreatedMessage msg) {
        UUID orderId = msg.order().orderId();

        if (deliveryRepository.existsByOrderId(orderId)) {
            return deliveryRepository.findByOrderId(orderId)
                    .orElseThrow()
                    .getDeliveryId();
        }

        Delivery delivery = Delivery.createFromOrder(
                orderId,
                msg.order().route().startHubId(),
                msg.order().route().destinationHubId(),
                msg.order().receiver().name(),
                msg.order().receiver().address(),
                msg.order().receiver().slackId()
        );

        return deliveryRepository.save(delivery).getDeliveryId();
    }

    public DeliveryResponse getOne(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new CustomException(ErrorCode.DELIVERY_NOT_FOUND));
        return DeliveryResponse.from(delivery);
    }

    public Page<DeliveryResponse> search(DeliverySearchCond cond, Pageable pageable) {
        Page<Delivery> result = deliveryRepository.findAll(
                DeliverySpecifications.search(cond), pageable
        );
        return result.map(DeliveryResponse::from);
    }

    @Transactional
    public DeliveryResponse updateStatus(UUID deliveryId, DeliveryStatusUpdateRequest request) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new CustomException(ErrorCode.DELIVERY_NOT_FOUND));

        DeliveryStatus newStatus = DeliveryStatus.valueOf(request.status());
        delivery.updateStatus(newStatus); // 엔티티가 전이 규칙 위반 시 예외 던짐

        return DeliveryResponse.from(delivery);
    }

    @Transactional
    public DeliveryResponse assignStaff(UUID deliveryId, DeliveryAssignRequest request) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new CustomException(ErrorCode.DELIVERY_NOT_FOUND));

        if (delivery.getStatus() != DeliveryStatus.WAITING_AT_HUB) {
            throw new CustomException(ErrorCode.INVALID_DELIVERY_ASSIGNMENT);
        }
        delivery.assignStaff(request.deliveryStaffId());

        return DeliveryResponse.from(delivery);
    }

    @Transactional
    public DeliveryResponse unassignStaff(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new CustomException(ErrorCode.DELIVERY_NOT_FOUND));

        delivery.unassignStaff();
        return DeliveryResponse.from(delivery);
    }
}