package com.fulfillmesh.inventory.service;

import com.fulfillmesh.common.events.*;
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

class InventoryEventProcessorTest {
    private final InventoryReservationService reservations = mock(InventoryReservationService.class);
    private final KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final InventoryEventProcessor processor = new InventoryEventProcessor(reservations, kafka, redis);

    @Test
    void publishesReservedEvent() {
        OrderCreatedEvent event = event();
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), eq("1"), eq(Duration.ofDays(2)))).thenReturn(true);
        when(reservations.reserve(event)).thenReturn(ReservationResult.accepted(
                List.of(new ReservedItemPayload("SKU-A", 1, 2, 12))
        ));

        processor.onOrderCreated(event);

        verify(kafka).send(eq(Topics.INVENTORY_RESERVED), eq(event.orderId().toString()), any(InventoryReservedEvent.class));
    }

    @Test
    void publishesRejectedEvent() {
        OrderCreatedEvent event = event();
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);
        when(reservations.reserve(event)).thenReturn(ReservationResult.rejected("no stock"));

        processor.onOrderCreated(event);

        verify(kafka).send(eq(Topics.INVENTORY_REJECTED), eq(event.orderId().toString()), any(InventoryRejectedEvent.class));
    }

    @Test
    void duplicateEventDoesNothing() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(false);

        processor.onOrderCreated(event());

        verifyNoInteractions(reservations);
        verifyNoInteractions(kafka);
    }

    private OrderCreatedEvent event() {
        return new OrderCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "student@example.com",
                List.of(new OrderLinePayload("SKU-A", 1)),
                Instant.now()
        );
    }
}
