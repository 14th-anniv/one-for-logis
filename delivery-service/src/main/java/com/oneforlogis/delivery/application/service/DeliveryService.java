package com.oneforlogis.delivery.application.service;

import com.oneforlogis.delivery.application.event.OrderCreatedMessage;
import com.oneforlogis.delivery.domain.model.Delivery;
import com.oneforlogis.delivery.domain.repository.DeliveryRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;

    @Transactional
    public UUID createIfAbsentFromOrder(OrderCreatedMessage msg) {
        var orderId = msg.order().orderId();
        if (deliveryRepository.existsByOrderId(orderId)) {
            return deliveryRepository.findByOrderId(orderId)
                    .orElseThrow()
                    .getDeliveryId();
        }

        var delivery = Delivery.createFromOrder(
                orderId,
                msg.order().route().startHubId(),
                msg.order().route().destinationHubId(),
                msg.order().receiver().name(),
                msg.order().receiver().address(),
                msg.order().receiver().slackId()
        );

        return deliveryRepository.save(delivery).getDeliveryId();
    }
}