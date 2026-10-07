package com.fulfillmesh.common.events;

import java.time.Instant;
import java.util.UUID;

public record ShipmentCreatedEvent(
        UUID eventId,
        UUID orderId,
        UUID shipmentId,
        String trackingNumber,
        Instant occurredAt
) {
}
