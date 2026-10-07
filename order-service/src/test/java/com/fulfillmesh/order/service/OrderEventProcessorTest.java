package com.fulfillmesh.order.service;

import com.fulfillmesh.common.events.InventoryReservedEvent;
import com.fulfillmesh.order.entity.OrderState;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderEventProcessorTest {
    private final OrderStateService states = mock(OrderStateService.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final OrderEventProcessor processor = new OrderEventProcessor(states, redis);

    @Test
    void processesNewEventOnce() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(
                eq("event:order:" + eventId),
                eq("1"),
                eq(Duration.ofDays(2))
        )).thenReturn(true);

        processor.onInventoryReserved(
                new InventoryReservedEvent(eventId, orderId, List.of(), Instant.now())
        );

        verify(states).move(orderId, OrderState.INVENTORY_RESERVED);
    }

    @Test
    void skipsDuplicateEvent() {
        UUID eventId = UUID.randomUUID();
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(false);

        processor.onInventoryReserved(
                new InventoryReservedEvent(eventId, UUID.randomUUID(), List.of(), Instant.now())
        );

        verifyNoInteractions(states);
    }
}
