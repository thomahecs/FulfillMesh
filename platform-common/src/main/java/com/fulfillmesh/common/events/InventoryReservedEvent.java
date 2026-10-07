package com.fulfillmesh.common.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InventoryReservedEvent(
        UUID eventId,
        UUID orderId,
        List<ReservedItemPayload> items,
        Instant occurredAt
) {
}
