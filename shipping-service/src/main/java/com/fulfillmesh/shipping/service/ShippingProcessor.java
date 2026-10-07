package com.fulfillmesh.shipping.service;

import com.fulfillmesh.common.events.PickCompletedEvent;
import com.fulfillmesh.common.events.ShipmentCreatedEvent;
import com.fulfillmesh.common.events.Topics;
import com.fulfillmesh.shipping.entity.Shipment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
public class ShippingProcessor {
    private final ShipmentCreationService shipments;
    private final KafkaTemplate<String, Object> kafka;
    private final StringRedisTemplate redis;

    public ShippingProcessor(
            ShipmentCreationService shipments,
            KafkaTemplate<String, Object> kafka,
            StringRedisTemplate redis
    ) {
        this.shipments = shipments;
        this.kafka = kafka;
        this.redis = redis;
    }

    @KafkaListener(topics = Topics.PICKING_COMPLETED, groupId = "${app.kafka.group-id}")
    public void onPickingCompleted(PickCompletedEvent event) {
        String key = "event:shipping:" + event.eventId();
        Boolean acquired = redis.opsForValue().setIfAbsent(key, "1", Duration.ofDays(2));
        if (!Boolean.TRUE.equals(acquired)) {
            return;
        }

        try {
            Shipment shipment = shipments.create(event.orderId());
            ShipmentCreatedEvent created = new ShipmentCreatedEvent(
                    UUID.randomUUID(),
                    event.orderId(),
                    shipment.getId(),
                    shipment.getTrackingNumber(),
                    Instant.now()
            );
            kafka.send(Topics.SHIPMENT_CREATED, event.orderId().toString(), created);
        } catch (RuntimeException ex) {
            redis.delete(key);
            throw ex;
        }
    }
}
