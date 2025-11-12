package com.oneforlogis.notification.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "topics")
public class TopicProperties {

    private String orderCreated = "order.created";
    private String deliveryStatusChanged = "delivery.status.changed";

    public String getOrderCreated() {
        return orderCreated;
    }

    public void setOrderCreated(String orderCreated) {
        this.orderCreated = orderCreated;
    }

    public String getDeliveryStatusChanged() {
        return deliveryStatusChanged;
    }

    public void setDeliveryStatusChanged(String deliveryStatusChanged) {
        this.deliveryStatusChanged = deliveryStatusChanged;
    }
}
