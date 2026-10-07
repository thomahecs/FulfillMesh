package com.fulfillmesh.order.service;

import com.fulfillmesh.common.events.*;
import com.fulfillmesh.order.entity.OrderState;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class OrderEventProcessor {
    private final OrderStateService states;
    private final StringRedisTemplate redis;

    public OrderEventProcessor(OrderStateService states, StringRedisTemplate redis) {
        this.states = states;
        this.redis = redis;
    }

    @KafkaListener(topics = Topics.INVENTORY_RESERVED, groupId = "${app.kafka.group-id}")
    public void onInventoryReserved(InventoryReservedEvent event) {
        processOnce(event.eventId(), () -> states.move(event.orderId(), OrderState.INVENTORY_RESERVED));
    }

    @KafkaListener(topics = Topics.INVENTORY_REJECTED, groupId = "${app.kafka.group-id}")
    public void onInventoryRejected(InventoryRejectedEvent event) {
        processOnce(event.eventId(), () -> states.move(event.orderId(), OrderState.INVENTORY_REJECTED));
    }

    @KafkaListener(topics = Topics.PICKING_COMPLETED, groupId = "${app.kafka.group-id}")
    public void onPickingCompleted(PickCompletedEvent event) {
        processOnce(event.eventId(), () -> states.move(event.orderId(), OrderState.PICKING));
    }

    @KafkaListener(topics = Topics.SHIPMENT_CREATED, groupId = "${app.kafka.group-id}")
    public void onShipmentCreated(ShipmentCreatedEvent event) {
        processOnce(event.eventId(), () -> states.move(event.orderId(), OrderState.SHIPPED));
    }

    private void processOnce(UUID eventId, Runnable work) {
        String key = "event:order:" + eventId;
        Boolean acquired = redis.opsForValue().setIfAbsent(key, "1", Duration.ofDays(2));
        if (!Boolean.TRUE.equals(acquired)) {
            return;
        }

        try {
            work.run();
        } catch (RuntimeException ex) {
            // A failed attempt is released so Kafka can retry it.
            redis.delete(key);
            throw ex;
        }
    }
}
