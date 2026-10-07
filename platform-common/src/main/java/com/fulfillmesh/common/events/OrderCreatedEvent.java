package com.fulfillmesh.common.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        UUID orderId,
        String customerEmail,
        List<OrderLinePayload> items,
        Instant occurredAt
) {
}
