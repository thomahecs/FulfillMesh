package com.fulfillmesh.fulfillment.dto;

import com.fulfillmesh.fulfillment.entity.FulfillmentState;

import java.time.Instant;
import java.util.UUID;

public record FulfillmentView(
        UUID id,
        UUID orderId,
        FulfillmentState state,
        Integer optimizedDistance,
        Integer sequentialDistance,
        Double improvementPercent,
        String pickRoute,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
