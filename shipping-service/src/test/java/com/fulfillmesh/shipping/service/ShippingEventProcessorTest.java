package com.fulfillmesh.shipping.service;

import com.fulfillmesh.common.events.PickCompletedEvent;
import com.fulfillmesh.common.events.ShipmentCreatedEvent;
import com.fulfillmesh.common.events.Topics;
import com.fulfillmesh.shipping.entity.Shipment;
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

class ShippingEventProcessorTest {
    private final ShipmentCreationService shipments = mock(ShipmentCreationService.class);
    private final KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final ShippingProcessor processor = new ShippingProcessor(shipments, kafka, redis);

    @Test
    void publishesShipmentCreated() {
        UUID orderId = UUID.randomUUID();
        PickCompletedEvent event = new PickCompletedEvent(
                UUID.randomUUID(), orderId, List.of(), 80, 102, 21.5, Instant.now()
        );
        Shipment shipment = new Shipment(UUID.randomUUID(), orderId, "FM-123", Instant.now());

        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), eq("1"), eq(Duration.ofDays(2)))).thenReturn(true);
        when(shipments.create(orderId)).thenReturn(shipment);

        processor.onPickingCompleted(event);

        verify(kafka).send(eq(Topics.SHIPMENT_CREATED), eq(orderId.toString()), any(ShipmentCreatedEvent.class));
    }

    @Test
    void skipsDuplicateEvent() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(false);

        processor.onPickingCompleted(new PickCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), List.of(), 0, 0, 0, Instant.now()
        ));

        verifyNoInteractions(shipments);
    }
}
