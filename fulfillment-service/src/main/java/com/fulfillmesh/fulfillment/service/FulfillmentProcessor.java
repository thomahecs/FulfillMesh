package com.fulfillmesh.fulfillment.service;

import com.fulfillmesh.common.events.*;
import com.fulfillmesh.fulfillment.routing.RoutePlan;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
public class FulfillmentProcessor {
    private final FulfillmentTaskService tasks;
    private final KafkaTemplate<String, Object> kafka;
    private final StringRedisTemplate redis;

    public FulfillmentProcessor(
            FulfillmentTaskService tasks,
            KafkaTemplate<String, Object> kafka,
            StringRedisTemplate redis
    ) {
        this.tasks = tasks;
        this.kafka = kafka;
        this.redis = redis;
    }

    @KafkaListener(topics = Topics.INVENTORY_RESERVED, groupId = "${app.kafka.group-id}")
    public void onInventoryReserved(InventoryReservedEvent event) {
        String key = "event:fulfillment:" + event.eventId();
        Boolean acquired = redis.opsForValue().setIfAbsent(key, "1", Duration.ofDays(2));
        if (!Boolean.TRUE.equals(acquired)) {
            return;
        }

        try {
            RoutePlan route = tasks.createTaskAndRoute(event);
            PickCompletedEvent completed = new PickCompletedEvent(
                    UUID.randomUUID(),
                    event.orderId(),
                    route.optimizedStops(),
                    route.optimizedDistance(),
                    route.sequentialDistance(),
                    route.improvementPercent(),
                    Instant.now()
            );
            kafka.send(Topics.PICKING_COMPLETED, event.orderId().toString(), completed);
        } catch (RuntimeException ex) {
            redis.delete(key);
            throw ex;
        }
    }
}
