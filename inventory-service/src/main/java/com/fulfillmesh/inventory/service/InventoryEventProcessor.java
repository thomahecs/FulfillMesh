package com.fulfillmesh.inventory.service;

import com.fulfillmesh.common.events.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
public class InventoryEventProcessor {
    private final InventoryReservationService reservations;
    private final KafkaTemplate<String, Object> kafka;
    private final StringRedisTemplate redis;

    public InventoryEventProcessor(
            InventoryReservationService reservations,
            KafkaTemplate<String, Object> kafka,
            StringRedisTemplate redis
    ) {
        this.reservations = reservations;
        this.kafka = kafka;
        this.redis = redis;
    }

    @KafkaListener(topics = Topics.ORDER_CREATED, groupId = "${app.kafka.group-id}")
    public void onOrderCreated(OrderCreatedEvent event) {
        String key = "event:inventory:" + event.eventId();

        // Kafka can redeliver messages. SETNX gives this consumer a cheap
        // idempotency barrier so the same order is not reserved twice.
        Boolean acquired = redis.opsForValue().setIfAbsent(key, "1", Duration.ofDays(2));
        if (!Boolean.TRUE.equals(acquired)) {
            return;
        }

        try {
            ReservationResult result = reservations.reserve(event);
            if (result.success()) {
                InventoryReservedEvent reserved = new InventoryReservedEvent(
                        UUID.randomUUID(),
                        event.orderId(),
                        result.reservedItems(),
                        Instant.now()
                );
                kafka.send(Topics.INVENTORY_RESERVED, event.orderId().toString(), reserved);
            } else {
                InventoryRejectedEvent rejected = new InventoryRejectedEvent(
                        UUID.randomUUID(),
                        event.orderId(),
                        result.reason(),
                        Instant.now()
                );
                kafka.send(Topics.INVENTORY_REJECTED, event.orderId().toString(), rejected);
            }
        } catch (RuntimeException ex) {
            redis.delete(key);
            throw ex;
        }
    }
}
