package com.oneforlogis.delivery.application.service;

import com.oneforlogis.common.exception.CustomException;
import com.oneforlogis.common.exception.ErrorCode;
import com.oneforlogis.delivery.application.dto.DeliveryResponse;
import com.oneforlogis.delivery.application.dto.DeliverySearchCond;
import com.oneforlogis.delivery.application.event.OrderCreatedMessage;
import com.oneforlogis.delivery.domain.model.Delivery;
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

    @Transactional(readOnly = true)
    public DeliveryResponse getOne(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new CustomException(ErrorCode.DELIVERY_NOT_FOUND));
        return DeliveryResponse.from(delivery);
    }

    @Transactional(readOnly = true)
    public Page<DeliveryResponse> search(DeliverySearchCond cond, Pageable pageable) {
        Page<Delivery> result = deliveryRepository.findAll(
                DeliverySpecifications.search(cond), pageable
        );
        return result.map(DeliveryResponse::from);
    }
}