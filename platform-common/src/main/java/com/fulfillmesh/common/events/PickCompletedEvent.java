package com.fulfillmesh.common.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PickCompletedEvent(
        UUID eventId,
        UUID orderId,
        List<PickStopPayload> pickStops,
        int optimizedDistance,
        int sequentialDistance,
        double improvementPercent,
        Instant occurredAt
) {
}
