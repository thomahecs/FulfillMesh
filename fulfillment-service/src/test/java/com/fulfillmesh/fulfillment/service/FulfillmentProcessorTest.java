package com.fulfillmesh.fulfillment.service;

import com.fulfillmesh.common.events.*;
import com.fulfillmesh.fulfillment.routing.GridPoint;
import com.fulfillmesh.fulfillment.routing.RoutePlan;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FulfillmentProcessorTest {
    private final FulfillmentTaskService tasks = mock(FulfillmentTaskService.class);
    private final KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final FulfillmentProcessor processor = new FulfillmentProcessor(tasks, kafka, redis);

    @Test
    void publishesPickingCompleted() {
        InventoryReservedEvent event = new InventoryReservedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(new ReservedItemPayload("SKU-A", 1, 2, 12)),
                Instant.now()
        );
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), eq("1"), eq(Duration.ofDays(2)))).thenReturn(true);
        when(tasks.createTaskAndRoute(event)).thenReturn(new RoutePlan(
                List.of(new PickStopPayload("SKU-A", 2, 12)),
                List.of(new GridPoint(0, 0), new GridPoint(1, 0)),
                1,
                2,
                50.0
        ));

        processor.onInventoryReserved(event);

        verify(kafka).send(eq(Topics.PICKING_COMPLETED), eq(event.orderId().toString()), any(PickCompletedEvent.class));
    }

    @Test
    void skipsDuplicateEvent() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(false);

        processor.onInventoryReserved(new InventoryReservedEvent(
                UUID.randomUUID(), UUID.randomUUID(), List.of(), Instant.now()
        ));

        verifyNoInteractions(tasks);
    }
}
